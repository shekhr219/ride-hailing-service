package com.ridehailing.strategy;

import com.ridehailing.model.CarType;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculateFare(CarType carType, double distanceKm);
}
