package com.ridehailing.repository;

import com.ridehailing.model.Coupon;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<String, Coupon> couponMap = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon == null || coupon.getCode() == null) {
            throw new IllegalArgumentException("Coupon and Coupon code must not be null");
        }
        couponMap.put(coupon.getCode().toUpperCase().trim(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(couponMap.get(code.toUpperCase().trim()));
    }

    @Override
    public boolean deleteByCode(String code) {
        if (code == null) return false;
        return couponMap.remove(code.toUpperCase().trim()) != null;
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(couponMap.values());
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null) return false;
        return couponMap.containsKey(code.toUpperCase().trim());
    }

    @Override
    public void clear() {
        couponMap.clear();
    }
}
