package com.ridehailing.repository;

import com.ridehailing.model.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findByCode(String code);
    boolean deleteByCode(String code);
    List<Coupon> findAll();
    boolean existsByCode(String code);
    void clear();
}
