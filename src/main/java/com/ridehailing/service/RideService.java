package com.ridehailing.service;

import com.ridehailing.dto.BookRideRequest;
import com.ridehailing.dto.EndRideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.DriverUnavailableException;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RideRepository;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.strategy.DriverMatchingStrategy;
import com.ridehailing.strategy.PricingStrategy;
import com.ridehailing.util.DistanceCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RideService {
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final DriverMatchingStrategy driverMatchingStrategy;
    private final PricingStrategy pricingStrategy;
    private final CouponService couponService;
    private final DistanceCalculator distanceCalculator;

    public RideService(UserRepository userRepository,
                       DriverRepository driverRepository,
                       RideRepository rideRepository,
                       DriverMatchingStrategy driverMatchingStrategy,
                       PricingStrategy pricingStrategy,
                       CouponService couponService,
                       DistanceCalculator distanceCalculator) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.driverMatchingStrategy = driverMatchingStrategy;
        this.pricingStrategy = pricingStrategy;
        this.couponService = couponService;
        this.distanceCalculator = distanceCalculator;
    }

    public RideResponse bookRide(BookRideRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }

        // 1. Verify user exists
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Location pickupLocation = new Location(request.getPickupLatitude(), request.getPickupLongitude());

        // 2. Match driver (atomically marks driver as ON_RIDE upon match)
        Driver matchedDriver = driverMatchingStrategy.matchDriver(pickupLocation, request.getRequestedCarType())
                .orElseThrow(() -> new DriverUnavailableException(
                        "No available driver found within matching radius for requested type: " + request.getRequestedCarType()));

        driverRepository.save(matchedDriver);

        // 3. Create and save ongoing ride
        String rideId = "RIDE-" + UUID.randomUUID().toString().substring(0, 8);
        Ride ride = new Ride(
                rideId,
                request.getUserId(),
                matchedDriver.getId(),
                pickupLocation,
                request.getRequestedCarType(),
                matchedDriver.getCarType() // actual assigned car type (could be SEDAN via Hatchback fallback)
        );

        rideRepository.save(ride);
        return RideResponse.fromModel(ride);
    }

    public RideResponse endRide(String rideId, EndRideRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("End ride request cannot be null");
        }

        // 1. Find ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new InvalidOperationException("Cannot end ride in status: " + ride.getStatus());
        }

        // 2. Validate driver
        if (!ride.getDriverId().equals(request.getDriverId())) {
            throw new InvalidOperationException("Driver mismatch: provided driver is not assigned to this ride");
        }

        Driver driver = driverRepository.findById(ride.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + ride.getDriverId()));

        Location dropoffLocation = new Location(request.getDropoffLatitude(), request.getDropoffLongitude());

        // 3. Calculate distance
        double distanceKm = distanceCalculator.calculateDistanceKm(ride.getPickupLocation(), dropoffLocation);

        // 4. Calculate fare based on actual assigned car type
        BigDecimal baseFare = pricingStrategy.calculateFare(ride.getActualCarType(), distanceKm);

        // 5. Apply coupon if provided
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            discount = couponService.validateAndCalculateDiscount(couponCode, baseFare);
        }

        BigDecimal finalFare = baseFare.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // 6. Complete ride
        ride.setDropoffLocation(dropoffLocation);
        ride.setDistanceKm(distanceKm);
        ride.setFare(baseFare);
        ride.setCouponCode(couponCode != null && !couponCode.isBlank() ? couponCode.toUpperCase().trim() : null);
        ride.setDiscountAmount(discount);
        ride.setFinalFare(finalFare);
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        rideRepository.save(ride);

        // 7. Release driver and update location to dropoff
        driver.updateLocation(dropoffLocation);
        driver.release();
        driverRepository.save(driver);

        return RideResponse.fromModel(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));

        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new InvalidOperationException("Only ONGOING rides can be cancelled. Current status: " + ride.getStatus());
        }

        Driver driver = driverRepository.findById(ride.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + ride.getDriverId()));

        driver.release();
        driverRepository.save(driver);

        BigDecimal fee = BigDecimal.valueOf(25.00).setScale(2, RoundingMode.HALF_UP);
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationFee(fee);
        ride.setFinalFare(fee);
        ride.setCompletedAt(LocalDateTime.now());
        rideRepository.save(ride);

        return RideResponse.fromModel(ride);
    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        return RideResponse.fromModel(ride);
    }

    public List<RideResponse> getUserRides(String userId, RideStatus status) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Ride> rides = (status != null)
                ? rideRepository.findByUserIdAndStatus(userId, status)
                : rideRepository.findByUserId(userId);

        return rides.stream()
                .map(RideResponse::fromModel)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getDriverRides(String driverId, RideStatus status) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        List<Ride> rides = (status != null)
                ? rideRepository.findByDriverIdAndStatus(driverId, status)
                : rideRepository.findByDriverId(driverId);

        return rides.stream()
                .map(RideResponse::fromModel)
                .collect(Collectors.toList());
    }
}
