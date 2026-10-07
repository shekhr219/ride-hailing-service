package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;

import java.util.Optional;

public interface DriverMatchingStrategy {
    Optional<Driver> matchDriver(Location pickupLocation, CarType requestedCarType);
    Optional<Driver> matchDriver(Location pickupLocation, CarType requestedCarType, double radiusKm);
}
