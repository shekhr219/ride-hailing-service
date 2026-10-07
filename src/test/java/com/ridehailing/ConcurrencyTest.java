package com.ridehailing;

import com.ridehailing.dto.BookRideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.exception.DriverUnavailableException;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.model.User;
import com.ridehailing.repository.*;
import com.ridehailing.service.CouponService;
import com.ridehailing.service.RideService;
import com.ridehailing.strategy.DiscountStrategyResolver;
import com.ridehailing.strategy.NearestDriverStrategy;
import com.ridehailing.strategy.TieredPricingStrategy;
import com.ridehailing.util.HaversineDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    private UserRepository userRepository;
    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private CouponRepository couponRepository;
    private RideService rideService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        driverRepository = new InMemoryDriverRepository();
        rideRepository = new InMemoryRideRepository();
        couponRepository = new InMemoryCouponRepository();

        HaversineDistanceCalculator distanceCalculator = new HaversineDistanceCalculator();
        NearestDriverStrategy matchingStrategy = new NearestDriverStrategy(driverRepository, distanceCalculator, 10.0);
        TieredPricingStrategy pricingStrategy = new TieredPricingStrategy();
        DiscountStrategyResolver discountResolver = new DiscountStrategyResolver(List.of());
        CouponService couponService = new CouponService(couponRepository, discountResolver);

        rideService = new RideService(
                userRepository,
                driverRepository,
                rideRepository,
                matchingStrategy,
                pricingStrategy,
                couponService,
                distanceCalculator
        );
    }

    @Test
    @DisplayName("Concurrency: Simultaneous bookings for 1 available driver guarantees exactly 1 booking succeeds")
    void testSingleDriverConcurrentBooking() throws InterruptedException {
        // Register single driver
        Driver singleDriver = new Driver("D_CONCURRENT_1", "Speedy", "9999900001", CarType.SEDAN, 4.9, new Location(12.9716, 77.5946));
        driverRepository.save(singleDriver);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        Set<String> assignedDriverIds = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            String userId = "U_CONCURRENT_" + i;
            userRepository.save(new User(userId, "User " + i, "988880000" + i));

            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure all threads fire at once
                    RideResponse ride = rideService.bookRide(new BookRideRequest(userId, 12.9716, 77.5946, CarType.SEDAN));
                    successCount.incrementAndGet();
                    assignedDriverIds.add(ride.getDriverId());
                } catch (DriverUnavailableException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Release latch to let all 10 threads race concurrently
        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 1 thread must succeed, 9 must fail
        assertEquals(1, successCount.get(), "Exactly 1 booking must succeed for a single available driver");
        assertEquals(9, failureCount.get(), "9 bookings must fail due to driver unavailability");
        assertEquals(1, assignedDriverIds.size());
        assertEquals("D_CONCURRENT_1", assignedDriverIds.iterator().next());

        Driver driver = driverRepository.findById("D_CONCURRENT_1").orElseThrow();
        assertEquals(DriverStatus.ON_RIDE, driver.getStatus());
    }

    @Test
    @DisplayName("Concurrency: N available drivers with 2N concurrent requests assigns all N drivers without duplicates")
    void testMultiDriverConcurrentBooking() throws InterruptedException {
        int driverCount = 5;
        int requestCount = 15;

        for (int i = 0; i < driverCount; i++) {
            Driver d = new Driver("D_MULTI_" + i, "Driver " + i, "911110000" + i, CarType.SEDAN, 4.8, new Location(12.9716 + (i * 0.001), 77.5946));
            driverRepository.save(d);
        }

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        Set<String> assignedDriverIds = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < requestCount; i++) {
            String userId = "U_MULTI_" + i;
            userRepository.save(new User(userId, "User " + i, "922220000" + i));

            executor.submit(() -> {
                try {
                    startLatch.await();
                    RideResponse ride = rideService.bookRide(new BookRideRequest(userId, 12.9716, 77.5946, CarType.SEDAN));
                    successCount.incrementAndGet();
                    assignedDriverIds.add(ride.getDriverId());
                } catch (DriverUnavailableException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 5 bookings must succeed, 10 must fail
        assertEquals(5, successCount.get());
        assertEquals(10, failureCount.get());
        assertEquals(5, assignedDriverIds.size(), "Each successful booking must be assigned a unique driver");
    }
}
