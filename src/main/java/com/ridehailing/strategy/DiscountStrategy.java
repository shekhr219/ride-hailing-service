package com.ridehailing.strategy;

import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;

import java.math.BigDecimal;

public interface DiscountStrategy {
    DiscountType getSupportedDiscountType();
    BigDecimal calculateDiscount(BigDecimal baseFare, Coupon coupon);
}
