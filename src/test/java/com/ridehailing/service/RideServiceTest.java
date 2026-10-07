package com.ridehailing.service;

import com.ridehailing.dto.BookRideRequest;
import com.ridehailing.dto.EndRideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.DriverUnavailableException;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.*;
import com.ridehailing.repository.*;
import com.ridehailing.strategy.*;
import com.ridehailing.util.HaversineDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RideServiceTest {

    private UserRepository userRepository;
    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private CouponRepository couponRepository;
    private RideService rideService;
    private CouponService couponService;

    private User testUser;
    private Driver sedanDriver;
    private Driver hatchDriver;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        driverRepository = new InMemoryDriverRepository();
        rideRepository = new InMemoryRideRepository();
        couponRepository = new InMemoryCouponRepository();

        HaversineDistanceCalculator distanceCalculator = new HaversineDistanceCalculator();
        NearestDriverStrategy matchingStrategy = new NearestDriverStrategy(driverRepository, distanceCalculator, 5.0);
        TieredPricingStrategy pricingStrategy = new TieredPricingStrategy();

        PercentageDiscountStrategy percentDiscount = new PercentageDiscountStrategy();
        FlatDiscountStrategy flatDiscount = new FlatDiscountStrategy();
        DiscountStrategyResolver discountResolver = new DiscountStrategyResolver(List.of(percentDiscount, flatDiscount));
        couponService = new CouponService(couponRepository, discountResolver);

        rideService = new RideService(
                userRepository,
                driverRepository,
                rideRepository,
                matchingStrategy,
                pricingStrategy,
                couponService,
                distanceCalculator
        );

        testUser = new User("U1", "John Doe", "9876543210");
        userRepository.save(testUser);

        sedanDriver = new Driver("D_SEDAN", "Sedan Driver", "9876543211", CarType.SEDAN, 4.8, new Location(12.9716, 77.5946));
        driverRepository.save(sedanDriver);

        hatchDriver = new Driver("D_HATCH", "Hatch Driver", "9876543212", CarType.HATCHBACK, 4.6, new Location(12.9720, 77.5950));
        driverRepository.save(hatchDriver);
    }

    @Test
    @DisplayName("Successful booking: assigns driver, marks driver ON_RIDE, ride is ONGOING")
    void testSuccessfulBooking() {
        BookRideRequest request = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse response = rideService.bookRide(request);

        assertNotNull(response);
        assertEquals(RideStatus.ONGOING, response.getStatus());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals("D_SEDAN", response.getDriverId());
        assertEquals(CarType.SEDAN, response.getRequestedCarType());
        assertEquals(CarType.SEDAN, response.getActualCarType());

        // Verify driver status updated
        Driver driver = driverRepository.findById("D_SEDAN").orElseThrow();
        assertEquals(DriverStatus.ON_RIDE, driver.getStatus());
    }

    @Test
    @DisplayName("Hatchback fallback: user requests HATCHBACK, only SEDAN available -> preserves requested and actual car types")
    void testHatchbackFallbackBooking() {
        // Mark the only hatchback driver as ON_RIDE
        hatchDriver.setStatus(DriverStatus.ON_RIDE);
        driverRepository.save(hatchDriver);

        BookRideRequest request = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.HATCHBACK);
        RideResponse response = rideService.bookRide(request);

        assertNotNull(response);
        assertEquals("D_SEDAN", response.getDriverId());
        assertEquals(CarType.HATCHBACK, response.getRequestedCarType());
        assertEquals(CarType.SEDAN, response.getActualCarType());
        assertEquals(DriverStatus.ON_RIDE, sedanDriver.getStatus());
    }

    @Test
    @DisplayName("Ride completion: calculates distance, fare, makes driver AVAILABLE, updates driver location")
    void testRideCompletionWithoutCoupon() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse bookedRide = rideService.bookRide(bookRequest);

        // End ride ~7.0 km away: latitude shift by ~0.063 degrees is ~7 km
        // Location (13.0346, 77.5946) is ~7.00 km north
        EndRideRequest endRequest = new EndRideRequest("D_SEDAN", 13.0346, 77.5946, null);
        RideResponse completedRide = rideService.endRide(bookedRide.getRideId(), endRequest);

        assertEquals(RideStatus.COMPLETED, completedRide.getStatus());
        assertNotNull(completedRide.getDistanceKm());
        assertTrue(completedRide.getDistanceKm() > 6.5);
        assertNotNull(completedRide.getFare());
        assertEquals(completedRide.getFare(), completedRide.getFinalFare());
        assertEquals(BigDecimal.ZERO.setScale(2), completedRide.getDiscountAmount());

        // Driver should be AVAILABLE again at dropoff location
        Driver driver = driverRepository.findById("D_SEDAN").orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertEquals(13.0346, driver.getCurrentLocation().getLatitude());
        assertEquals(77.5946, driver.getCurrentLocation().getLongitude());
    }

    @Test
    @DisplayName("Ride completion with coupon: applies discount correctly to final fare")
    void testRideCompletionWithCoupon() {
        couponRepository.save(new Coupon("SAVE20", DiscountType.PERCENTAGE, new BigDecimal("20.00"), true));

        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse bookedRide = rideService.bookRide(bookRequest);

        // Short ride (~1 km): base fare = ₹50.00 (min fare)
        EndRideRequest endRequest = new EndRideRequest("D_SEDAN", 12.9806, 77.5946, "SAVE20");
        RideResponse completedRide = rideService.endRide(bookedRide.getRideId(), endRequest);

        assertEquals(new BigDecimal("50.00"), completedRide.getFare());
        // 20% of 50.00 = 10.00 discount -> final fare = 40.00
        assertEquals(new BigDecimal("10.00"), completedRide.getDiscountAmount());
        assertEquals(new BigDecimal("40.00"), completedRide.getFinalFare());
        assertEquals("SAVE20", completedRide.getCouponCode());
    }

    @Test
    @DisplayName("Ride history: returns ongoing and completed rides for user and driver")
    void testRideHistory() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse ride1 = rideService.bookRide(bookRequest);

        // User ongoing rides should contain ride1
        List<RideResponse> userOngoing = rideService.getUserRides(testUser.getId(), RideStatus.ONGOING);
        assertEquals(1, userOngoing.size());
        assertEquals(ride1.getRideId(), userOngoing.get(0).getRideId());

        // Complete ride1
        rideService.endRide(ride1.getRideId(), new EndRideRequest("D_SEDAN", 12.9800, 77.5946, null));

        // Now ongoing should be empty, completed should have 1
        assertEquals(0, rideService.getUserRides(testUser.getId(), RideStatus.ONGOING).size());
        List<RideResponse> userCompleted = rideService.getUserRides(testUser.getId(), RideStatus.COMPLETED);
        assertEquals(1, userCompleted.size());

        // Driver history check
        List<RideResponse> driverCompleted = rideService.getDriverRides("D_SEDAN", RideStatus.COMPLETED);
        assertEquals(1, driverCompleted.size());
        assertEquals(ride1.getRideId(), driverCompleted.get(0).getRideId());
    }

    @Test
    @DisplayName("Booking with invalid user throws ResourceNotFoundException")
    void testBookingWithInvalidUserThrows() {
        BookRideRequest request = new BookRideRequest("NON_EXISTENT", 12.9716, 77.5946, CarType.SEDAN);
        assertThrows(ResourceNotFoundException.class, () -> rideService.bookRide(request));
    }

    @Test
    @DisplayName("Booking when no driver available throws DriverUnavailableException")
    void testBookingWhenNoDriverAvailableThrows() {
        sedanDriver.setStatus(DriverStatus.ON_RIDE);
        hatchDriver.setStatus(DriverStatus.ON_RIDE);
        driverRepository.save(sedanDriver);
        driverRepository.save(hatchDriver);

        BookRideRequest request = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        assertThrows(DriverUnavailableException.class, () -> rideService.bookRide(request));
    }

    @Test
    @DisplayName("Ending ride with mismatched driver throws InvalidOperationException")
    void testEndingRideWithMismatchedDriverThrows() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse ride = rideService.bookRide(bookRequest);

        EndRideRequest wrongDriverRequest = new EndRideRequest("WRONG_DRIVER", 12.9800, 77.5946, null);
        assertThrows(InvalidOperationException.class, () -> rideService.endRide(ride.getRideId(), wrongDriverRequest));
    }

    @Test
    @DisplayName("Ending an already completed ride throws InvalidOperationException")
    void testEndingAlreadyCompletedRideThrows() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse ride = rideService.bookRide(bookRequest);

        rideService.endRide(ride.getRideId(), new EndRideRequest("D_SEDAN", 12.9800, 77.5946, null));

        // Try ending again
        assertThrows(InvalidOperationException.class, () ->
                rideService.endRide(ride.getRideId(), new EndRideRequest("D_SEDAN", 12.9800, 77.5946, null)));
    }

    @Test
    @DisplayName("Cancel ride: marks ride CANCELLED, applies cancellation fee, releases driver to AVAILABLE")
    void testCancelRide() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse ride = rideService.bookRide(bookRequest);

        RideResponse cancelledRide = rideService.cancelRide(ride.getRideId());

        assertEquals(RideStatus.CANCELLED, cancelledRide.getStatus());
        assertEquals(new BigDecimal("25.00"), cancelledRide.getCancellationFee());
        assertEquals(new BigDecimal("25.00"), cancelledRide.getFinalFare());

        Driver driver = driverRepository.findById("D_SEDAN").orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
    }

    @Test
    @DisplayName("Cancelling non-ongoing ride throws InvalidOperationException")
    void testCancelNonOngoingRideThrows() {
        BookRideRequest bookRequest = new BookRideRequest(testUser.getId(), 12.9716, 77.5946, CarType.SEDAN);
        RideResponse ride = rideService.bookRide(bookRequest);

        rideService.cancelRide(ride.getRideId());

        // Try cancelling again
        assertThrows(InvalidOperationException.class, () -> rideService.cancelRide(ride.getRideId()));
    }
}
