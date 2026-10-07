package com.ridehailing.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Coupon {
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private boolean active;

    public Coupon() {
        this.active = true;
    }

    public Coupon(String code, DiscountType discountType, BigDecimal discountValue, boolean active) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.active = active;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(code, coupon.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "code='" + code + '\'' +
                ", discountType=" + discountType +
                ", discountValue=" + discountValue +
                ", active=" + active +
                '}';
    }
}
