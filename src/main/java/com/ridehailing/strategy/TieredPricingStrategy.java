package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Component
public class TieredPricingStrategy implements PricingStrategy {

    public static class CarTypePricingConfig {
        private final BigDecimal minimumFare;
        private final BigDecimal tier1Rate; // 0 to 2 km
        private final BigDecimal tier2Rate; // 2 to 5 km
        private final BigDecimal tier3Rate; // > 5 km

        public CarTypePricingConfig(BigDecimal minimumFare, BigDecimal tier1Rate, BigDecimal tier2Rate, BigDecimal tier3Rate) {
            this.minimumFare = minimumFare;
            this.tier1Rate = tier1Rate;
            this.tier2Rate = tier2Rate;
            this.tier3Rate = tier3Rate;
        }

        public BigDecimal getMinimumFare() {
            return minimumFare;
        }

        public BigDecimal getTier1Rate() {
            return tier1Rate;
        }

        public BigDecimal getTier2Rate() {
            return tier2Rate;
        }

        public BigDecimal getTier3Rate() {
            return tier3Rate;
        }
    }

    private final Map<CarType, CarTypePricingConfig> pricingConfigMap = new EnumMap<>(CarType.class);

    public TieredPricingStrategy() {
        // SEDAN pricing as specified in prompt:
        // Min fare ₹50, first 2 km = ₹10/km, 3-5 km = ₹8/km, 6 km onward = ₹5/km
        pricingConfigMap.put(CarType.SEDAN, new CarTypePricingConfig(
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(8.00),
                BigDecimal.valueOf(5.00)
        ));

        // HATCHBACK pricing:
        // Min fare ₹40, first 2 km = ₹8/km, 3-5 km = ₹6/km, 6 km onward = ₹4/km
        pricingConfigMap.put(CarType.HATCHBACK, new CarTypePricingConfig(
                BigDecimal.valueOf(40.00),
                BigDecimal.valueOf(8.00),
                BigDecimal.valueOf(6.00),
                BigDecimal.valueOf(4.00)
        ));
    }

    public void configurePricing(CarType carType, CarTypePricingConfig config) {
        pricingConfigMap.put(carType, config);
    }

    @Override
    public BigDecimal calculateFare(CarType carType, double distanceKm) {
        if (carType == null) {
            throw new IllegalArgumentException("CarType must not be null");
        }
        if (distanceKm < 0.0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }

        CarTypePricingConfig config = pricingConfigMap.get(carType);
        if (config == null) {
            throw new IllegalArgumentException("No pricing configured for car type: " + carType);
        }

        // Distance breakdown:
        // Tier 1: up to 2.0 km
        double tier1Dist = Math.min(distanceKm, 2.0);
        // Tier 2: from 2.0 to 5.0 km (max 3.0 km)
        double tier2Dist = Math.max(0.0, Math.min(distanceKm, 5.0) - 2.0);
        // Tier 3: beyond 5.0 km
        double tier3Dist = Math.max(0.0, distanceKm - 5.0);

        BigDecimal fareTier1 = config.getTier1Rate().multiply(BigDecimal.valueOf(tier1Dist));
        BigDecimal fareTier2 = config.getTier2Rate().multiply(BigDecimal.valueOf(tier2Dist));
        BigDecimal fareTier3 = config.getTier3Rate().multiply(BigDecimal.valueOf(tier3Dist));

        BigDecimal calculatedFare = fareTier1.add(fareTier2).add(fareTier3);

        // Apply minimum fare rule
        BigDecimal finalFare = calculatedFare.compareTo(config.getMinimumFare()) < 0
                ? config.getMinimumFare()
                : calculatedFare;

        return finalFare.setScale(2, RoundingMode.HALF_UP);
    }
}
