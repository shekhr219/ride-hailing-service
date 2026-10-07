package com.ridehailing.dto;

import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;

import java.math.BigDecimal;

public class CouponResponse {
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private boolean active;

    public CouponResponse() {
    }

    public CouponResponse(String code, DiscountType discountType, BigDecimal discountValue, boolean active) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.active = active;
    }

    public static CouponResponse fromModel(Coupon coupon) {
        if (coupon == null) return null;
        return new CouponResponse(
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.isActive()
        );
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
