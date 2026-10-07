package com.ridehailing.strategy;

import com.ridehailing.dto.CouponRequest;
import com.ridehailing.dto.CouponResponse;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;
import com.ridehailing.repository.InMemoryCouponRepository;
import com.ridehailing.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CouponStrategyAndServiceTest {

    private PercentageDiscountStrategy percentageStrategy;
    private FlatDiscountStrategy flatStrategy;
    private InMemoryCouponRepository couponRepository;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        percentageStrategy = new PercentageDiscountStrategy();
        flatStrategy = new FlatDiscountStrategy();
        couponRepository = new InMemoryCouponRepository();
        DiscountStrategyResolver resolver = new DiscountStrategyResolver(List.of(percentageStrategy, flatStrategy));
        couponService = new CouponService(couponRepository, resolver);
    }

    @Test
    @DisplayName("PercentageDiscountStrategy: Calculates correct discount percentage")
    void testPercentageDiscountCalculation() {
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, new BigDecimal("20.00"), true);
        BigDecimal discount = percentageStrategy.calculateDiscount(new BigDecimal("100.00"), coupon);
        assertEquals(new BigDecimal("20.00"), discount);

        BigDecimal discount50 = percentageStrategy.calculateDiscount(new BigDecimal("50.00"), coupon);
        assertEquals(new BigDecimal("10.00"), discount50);
    }

    @Test
    @DisplayName("PercentageDiscountStrategy: Discount cannot exceed base fare")
    void testPercentageDiscountCappedAtBaseFare() {
        // Even if 120% discount is configured, it caps at base fare
        Coupon coupon = new Coupon("SUPER120", DiscountType.PERCENTAGE, new BigDecimal("120.00"), true);
        BigDecimal discount = percentageStrategy.calculateDiscount(new BigDecimal("80.00"), coupon);
        assertEquals(new BigDecimal("80.00"), discount);
    }

    @Test
    @DisplayName("FlatDiscountStrategy: Calculates flat deduction and caps at base fare")
    void testFlatDiscountCalculation() {
        Coupon coupon = new Coupon("FLAT30", DiscountType.FLAT, new BigDecimal("30.00"), true);
        BigDecimal discount = flatStrategy.calculateDiscount(new BigDecimal("100.00"), coupon);
        assertEquals(new BigDecimal("30.00"), discount);

        // Cap at base fare if fare is less than flat discount
        BigDecimal capped = flatStrategy.calculateDiscount(new BigDecimal("25.00"), coupon);
        assertEquals(new BigDecimal("25.00"), capped);
    }

    @Test
    @DisplayName("CouponService: Successfully add and retrieve coupon")
    void testAddAndGetCoupon() {
        CouponRequest request = new CouponRequest("WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"), true);
        CouponResponse response = couponService.addCoupon(request);

        assertNotNull(response);
        assertEquals("WELCOME10", response.getCode());
        assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());

        Coupon retrieved = couponService.getCoupon("welcome10"); // case-insensitive check
        assertEquals("WELCOME10", retrieved.getCode());
    }

    @Test
    @DisplayName("CouponService: Adding duplicate coupon throws InvalidOperationException")
    void testAddDuplicateCouponThrows() {
        CouponRequest request = new CouponRequest("PROMO", DiscountType.PERCENTAGE, new BigDecimal("15.00"), true);
        couponService.addCoupon(request);

        assertThrows(InvalidOperationException.class, () -> couponService.addCoupon(request));
    }

    @Test
    @DisplayName("CouponService: Adding coupon with percentage > 100 throws InvalidOperationException")
    void testAddExcessivePercentageThrows() {
        CouponRequest request = new CouponRequest("INVALID150", DiscountType.PERCENTAGE, new BigDecimal("150.00"), true);
        assertThrows(InvalidOperationException.class, () -> couponService.addCoupon(request));
    }

    @Test
    @DisplayName("CouponService: Delete coupon")
    void testDeleteCoupon() {
        CouponRequest request = new CouponRequest("DELME", DiscountType.PERCENTAGE, new BigDecimal("10.00"), true);
        couponService.addCoupon(request);

        couponService.deleteCoupon("DELME");
        assertThrows(ResourceNotFoundException.class, () -> couponService.getCoupon("DELME"));
    }

    @Test
    @DisplayName("CouponService: Deleting non-existent coupon throws ResourceNotFoundException")
    void testDeleteNonExistentCouponThrows() {
        assertThrows(ResourceNotFoundException.class, () -> couponService.deleteCoupon("NONEXISTENT"));
    }

    @Test
    @DisplayName("CouponService: Applying valid coupon calculates discount")
    void testValidateAndCalculateDiscount() {
        couponService.addCoupon(new CouponRequest("RIDE25", DiscountType.PERCENTAGE, new BigDecimal("25.00"), true));
        BigDecimal discount = couponService.validateAndCalculateDiscount("RIDE25", new BigDecimal("200.00"));
        assertEquals(new BigDecimal("50.00"), discount);
    }

    @Test
    @DisplayName("CouponService: Non-existent coupon throws InvalidOperationException")
    void testNonExistentCouponThrows() {
        assertThrows(InvalidOperationException.class, () ->
                couponService.validateAndCalculateDiscount("FAKECODE", new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("CouponService: Inactive coupon throws InvalidOperationException")
    void testInactiveCouponThrows() {
        couponService.addCoupon(new CouponRequest("EXPIRED", DiscountType.PERCENTAGE, new BigDecimal("20.00"), false));
        assertThrows(InvalidOperationException.class, () ->
                couponService.validateAndCalculateDiscount("EXPIRED", new BigDecimal("100.00")));
    }
}
