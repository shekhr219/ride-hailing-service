package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.util.DistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Alternate Driver Matching Strategy that selects the highest-rated driver within the search radius.
 * Demonstrates clean Strategy Pattern extensibility.
 */
@Component("highestRatedDriverStrategy")
public class HighestRatedDriverStrategy implements DriverMatchingStrategy {

    private final DriverRepository driverRepository;
    private final DistanceCalculator distanceCalculator;
    private final double defaultRadiusKm;

    public HighestRatedDriverStrategy(DriverRepository driverRepository,
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

        Optional<Driver> matched = findAndAssignHighestRated(pickupLocation, requestedCarType, radiusKm);
        if (matched.isPresent()) {
            return matched;
        }

        // Hatchback fallback rule applies here as well
        if (requestedCarType == CarType.HATCHBACK) {
            return findAndAssignHighestRated(pickupLocation, CarType.SEDAN, radiusKm);
        }

        return Optional.empty();
    }

    private Optional<Driver> findAndAssignHighestRated(Location pickupLocation, CarType carType, double radiusKm) {
        List<Driver> eligibleDrivers = driverRepository.findAll().stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE && driver.getCarType() == carType)
                .filter(driver -> distanceCalculator.calculateDistanceKm(pickupLocation, driver.getCurrentLocation()) <= radiusKm)
                .sorted(Comparator.comparingDouble(Driver::getRating).reversed())
                .collect(Collectors.toList());

        for (Driver driver : eligibleDrivers) {
            if (driver.tryAssign()) {
                return Optional.of(driver);
            }
        }

        return Optional.empty();
    }
}
