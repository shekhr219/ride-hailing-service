package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.InMemoryDriverRepository;
import com.ridehailing.util.HaversineDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DriverMatchingStrategyTest {

    private DriverRepository driverRepository;
    private NearestDriverStrategy matchingStrategy;
    private final Location pickup = new Location(12.9716, 77.5946); // Bangalore center

    @BeforeEach
    void setUp() {
        driverRepository = new InMemoryDriverRepository();
        matchingStrategy = new NearestDriverStrategy(driverRepository, new HaversineDistanceCalculator(), 5.0);
    }

    @Test
    @DisplayName("Selects the nearest available driver within radius")
    void testSelectsNearestAvailableDriver() {
        // Driver 1: ~1.1 km away
        Driver d1 = new Driver("D1", "John", "111", CarType.SEDAN, 4.8, new Location(12.9816, 77.5946));
        // Driver 2: ~0.55 km away (closer)
        Driver d2 = new Driver("D2", "Alice", "222", CarType.SEDAN, 4.9, new Location(12.9766, 77.5946));

        driverRepository.save(d1);
        driverRepository.save(d2);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.SEDAN);

        assertTrue(matched.isPresent());
        assertEquals("D2", matched.get().getId());
        assertEquals(DriverStatus.ON_RIDE, matched.get().getStatus());
    }

    @Test
    @DisplayName("Ignores drivers located outside the matching radius")
    void testIgnoresDriverOutsideRadius() {
        // Driver far away (~50 km)
        Driver farDriver = new Driver("D_FAR", "Bob", "333", CarType.SEDAN, 4.5, new Location(13.3716, 77.5946));
        driverRepository.save(farDriver);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.SEDAN);

        assertTrue(matched.isEmpty());
    }

    @Test
    @DisplayName("Ignores unavailable drivers (already ON_RIDE)")
    void testIgnoresUnavailableDriver() {
        // Driver very close (~0.2 km), but status is ON_RIDE
        Driver busyDriver = new Driver("D_BUSY", "Busy Bob", "444", CarType.SEDAN, 5.0, new Location(12.9730, 77.5946));
        busyDriver.setStatus(DriverStatus.ON_RIDE);
        driverRepository.save(busyDriver);

        // Driver further away (~1.5 km), but AVAILABLE
        Driver freeDriver = new Driver("D_FREE", "Free Fiona", "555", CarType.SEDAN, 4.0, new Location(12.9850, 77.5946));
        driverRepository.save(freeDriver);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.SEDAN);

        assertTrue(matched.isPresent());
        assertEquals("D_FREE", matched.get().getId());
    }

    @Test
    @DisplayName("Requested HATCHBACK available: assigns nearest HATCHBACK even if closer SEDAN exists")
    void testRequestedHatchbackAvailableAssignsHatchback() {
        // Closer Sedan (~0.3 km)
        Driver sedanDriver = new Driver("D_SEDAN", "Sam", "666", CarType.SEDAN, 4.9, new Location(12.9740, 77.5946));
        driverRepository.save(sedanDriver);

        // Slightly further Hatchback (~0.8 km)
        Driver hatchDriver = new Driver("D_HATCH", "Harry", "777", CarType.HATCHBACK, 4.7, new Location(12.9780, 77.5946));
        driverRepository.save(hatchDriver);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.HATCHBACK);

        assertTrue(matched.isPresent());
        assertEquals("D_HATCH", matched.get().getId());
        assertEquals(CarType.HATCHBACK, matched.get().getCarType());
    }

    @Test
    @DisplayName("Requested HATCHBACK unavailable: automatically falls back to nearest available SEDAN")
    void testRequestedHatchbackUnavailableFallsBackToSedan() {
        // No Hatchback exists in system. Only Sedan exists.
        Driver sedanDriver = new Driver("D_SEDAN_FALLBACK", "Steve", "888", CarType.SEDAN, 4.8, new Location(12.9750, 77.5946));
        driverRepository.save(sedanDriver);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.HATCHBACK);

        assertTrue(matched.isPresent());
        assertEquals("D_SEDAN_FALLBACK", matched.get().getId());
        assertEquals(CarType.SEDAN, matched.get().getCarType());
    }

    @Test
    @DisplayName("Requested SEDAN when only HATCHBACK is available does NOT assign Hatchback")
    void testRequestedSedanDoesNotFallbackToHatchback() {
        Driver hatchDriver = new Driver("D_HATCH", "Harry", "999", CarType.HATCHBACK, 4.7, new Location(12.9750, 77.5946));
        driverRepository.save(hatchDriver);

        Optional<Driver> matched = matchingStrategy.matchDriver(pickup, CarType.SEDAN);

        assertTrue(matched.isEmpty());
    }

    @Test
    @DisplayName("HighestRatedDriverStrategy: picks the highest-rated driver rather than the nearest")
    void testHighestRatedDriverStrategy() {
        HighestRatedDriverStrategy ratedStrategy = new HighestRatedDriverStrategy(
                driverRepository, new HaversineDistanceCalculator(), 5.0);

        // Driver 1: Closer (0.2 km), but lower rating 4.2
        Driver closerDriver = new Driver("D_CLOSER", "Closer", "1111", CarType.SEDAN, 4.2, new Location(12.9730, 77.5946));
        // Driver 2: Further (1.5 km), but higher rating 4.95
        Driver topDriver = new Driver("D_TOP", "Top", "2222", CarType.SEDAN, 4.95, new Location(12.9850, 77.5946));

        driverRepository.save(closerDriver);
        driverRepository.save(topDriver);

        Optional<Driver> matched = ratedStrategy.matchDriver(pickup, CarType.SEDAN);

        assertTrue(matched.isPresent());
        assertEquals("D_TOP", matched.get().getId());
    }
}
