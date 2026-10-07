package com.ridehailing.strategy;

import com.ridehailing.model.CarType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TieredPricingStrategyTest {

    private TieredPricingStrategy pricingStrategy;

    @BeforeEach
    void setUp() {
        pricingStrategy = new TieredPricingStrategy();
    }

    @Test
    @DisplayName("Sedan: Very short ride below minimum fare threshold applies minimum fare")
    void testSedanMinimumFareForShortRides() {
        // 0.5 km: tier1 = 0.5 * 10 = 5.0, min fare = 50.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 0.5);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Sedan: Exactly at 2 km tier 1 boundary applies minimum fare")
    void testSedanExactTier1Boundary() {
        // 2.0 km: 2 * 10 = 20.0, min fare = 50.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 2.0);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Sedan: Distance in second tier (4 km) still under minimum fare")
    void testSedanSecondTierUnderMinimumFare() {
        // 4.0 km: 2 * 10 + 2 * 8 = 20 + 16 = 36.0, min fare = 50.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 4.0);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Sedan: Exactly at 5 km tier 2 boundary")
    void testSedanExactTier2Boundary() {
        // 5.0 km: 2 * 10 + 3 * 8 = 20 + 24 = 44.0, min fare = 50.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 5.0);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Sedan: At 6 km tier 3 start still under minimum fare")
    void testSedanAt6Km() {
        // 6.0 km: 20 + 24 + 1 * 5 = 49.0, min fare = 50.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 6.0);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Sedan: At 7 km exceeds minimum fare threshold")
    void testSedanExceedingMinimumFare() {
        // 7.0 km: 2 * 10 + 3 * 8 + 2 * 5 = 20 + 24 + 10 = 54.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 7.0);
        assertEquals(new BigDecimal("54.00"), fare);
    }

    @Test
    @DisplayName("Sedan: Long distance (10 km)")
    void testSedanLongDistance() {
        // 10.0 km: 2 * 10 + 3 * 8 + 5 * 5 = 20 + 24 + 25 = 69.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.SEDAN, 10.0);
        assertEquals(new BigDecimal("69.00"), fare);
    }

    @Test
    @DisplayName("Hatchback: Minimum fare ₹40 applies for short rides")
    void testHatchbackMinimumFare() {
        // 1.0 km: 1 * 8 = 8, min fare = 40.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.HATCHBACK, 1.0);
        assertEquals(new BigDecimal("40.00"), fare);
    }

    @Test
    @DisplayName("Hatchback: Boundary at 2 km and 5 km")
    void testHatchbackBoundaries() {
        // 2.0 km: 2 * 8 = 16, min fare = 40.00
        assertEquals(new BigDecimal("40.00"), pricingStrategy.calculateFare(CarType.HATCHBACK, 2.0));

        // 5.0 km: 2 * 8 + 3 * 6 = 16 + 18 = 34, min fare = 40.00
        assertEquals(new BigDecimal("40.00"), pricingStrategy.calculateFare(CarType.HATCHBACK, 5.0));
    }

    @Test
    @DisplayName("Hatchback: Exceeding minimum fare at 7 km")
    void testHatchbackExceedingMinimumFare() {
        // 7.0 km: 2 * 8 + 3 * 6 + 2 * 4 = 16 + 18 + 8 = 42.00
        BigDecimal fare = pricingStrategy.calculateFare(CarType.HATCHBACK, 7.0);
        assertEquals(new BigDecimal("42.00"), fare);
    }

    @Test
    @DisplayName("Zero distance returns minimum fare")
    void testZeroDistanceReturnsMinimumFare() {
        assertEquals(new BigDecimal("50.00"), pricingStrategy.calculateFare(CarType.SEDAN, 0.0));
        assertEquals(new BigDecimal("40.00"), pricingStrategy.calculateFare(CarType.HATCHBACK, 0.0));
    }

    @Test
    @DisplayName("Negative distance throws IllegalArgumentException")
    void testNegativeDistanceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pricingStrategy.calculateFare(CarType.SEDAN, -1.0));
    }

    @Test
    @DisplayName("Null car type throws IllegalArgumentException")
    void testNullCarTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pricingStrategy.calculateFare(null, 5.0));
    }
}
