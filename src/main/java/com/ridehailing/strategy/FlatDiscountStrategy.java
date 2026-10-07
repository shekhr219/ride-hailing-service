package com.ridehailing.strategy;

import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FlatDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getSupportedDiscountType() {
        return DiscountType.FLAT;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal baseFare, Coupon coupon) {
        if (baseFare == null || coupon == null || baseFare.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal flatAmount = coupon.getDiscountValue();
        if (flatAmount == null || flatAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Discount cannot exceed base fare
        BigDecimal discount = flatAmount.min(baseFare);
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
