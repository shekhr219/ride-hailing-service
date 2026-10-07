package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.util.DistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
public class NearestDriverStrategy implements DriverMatchingStrategy {

    private final DriverRepository driverRepository;
    private final DistanceCalculator distanceCalculator;
    private final double defaultRadiusKm;

    public NearestDriverStrategy(DriverRepository driverRepository,
                                 DistanceCalculator distanceCalculator,
                                 @Value("${ride.matching.radius-km:5.0}") double defaultRadiusKm) {
        this.driverRepository = driverRepository;
        this.distanceCalculator = distanceCalculator;
        this.defaultRadiusKm = defaultRadiusKm;
    }

    @Override
    public Optional<Driver> matchDriver(Location pickupLocation, CarType requestedCarType) {
        return matchDriver(pickupLocation, requestedCarType, defaultRadiusKm);
    }

    @Override
    public Optional<Driver> matchDriver(Location pickupLocation, CarType requestedCarType, double radiusKm) {
        if (pickupLocation == null || requestedCarType == null) {
            return Optional.empty();
        }

        // 1. Try matching the exact requested car type
        Optional<Driver> matchedDriver = findAndAssignNearestDriver(pickupLocation, requestedCarType, radiusKm);
        if (matchedDriver.isPresent()) {
            return matchedDriver;
        }

        // 2. Hatchback fallback rule: If HATCHBACK requested and none available, fallback to SEDAN
        if (requestedCarType == CarType.HATCHBACK) {
            return findAndAssignNearestDriver(pickupLocation, CarType.SEDAN, radiusKm);
        }

        return Optional.empty();
    }

    private Optional<Driver> findAndAssignNearestDriver(Location pickupLocation, CarType carType, double radiusKm) {
        // Find available drivers of the given car type within radius, sorted by distance ascending
        List<DriverDistancePair> eligibleDrivers = driverRepository.findAll().stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE && driver.getCarType() == carType)
                .map(driver -> new DriverDistancePair(driver, distanceCalculator.calculateDistanceKm(pickupLocation, driver.getCurrentLocation())))
                .filter(pair -> pair.distance <= radiusKm)
                .sorted(Comparator.comparingDouble(pair -> pair.distance))
                .collect(Collectors.toList());

        // Concurrency-safe atomic assignment loop:
        // Try each candidate in order of proximity until one is successfully assigned
        for (DriverDistancePair pair : eligibleDrivers) {
            if (pair.driver.tryAssign()) {
                return Optional.of(pair.driver);
            }
        }

        return Optional.empty();
    }

    private static class DriverDistancePair {
        final Driver driver;
        final double distance;

        DriverDistancePair(Driver driver, double distance) {
            this.driver = driver;
            this.distance = distance;
        }
    }
}
