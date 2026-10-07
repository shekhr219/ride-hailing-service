package com.ridehailing.strategy;

import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getSupportedDiscountType() {
        return DiscountType.PERCENTAGE;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal baseFare, Coupon coupon) {
        if (baseFare == null || coupon == null || baseFare.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal percentage = coupon.getDiscountValue();
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // discount = baseFare * (percentage / 100)
        BigDecimal discount = baseFare.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Ensure discount cannot exceed base fare (fare cannot become negative)
        if (discount.compareTo(baseFare) > 0) {
            return baseFare.setScale(2, RoundingMode.HALF_UP);
        }

        return discount;
    }
}
