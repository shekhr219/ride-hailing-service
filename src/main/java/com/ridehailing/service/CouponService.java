package com.ridehailing.service;

import com.ridehailing.dto.CouponRequest;
import com.ridehailing.dto.CouponResponse;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.Coupon;
import com.ridehailing.model.DiscountType;
import com.ridehailing.repository.CouponRepository;
import com.ridehailing.strategy.DiscountStrategy;
import com.ridehailing.strategy.DiscountStrategyResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {
    private final CouponRepository couponRepository;
    private final DiscountStrategyResolver discountStrategyResolver;

    public CouponService(CouponRepository couponRepository, DiscountStrategyResolver discountStrategyResolver) {
        this.couponRepository = couponRepository;
        this.discountStrategyResolver = discountStrategyResolver;
    }

    public CouponResponse addCoupon(CouponRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Coupon request cannot be null");
        }
        String cleanCode = request.getCode().toUpperCase().trim();
        if (couponRepository.existsByCode(cleanCode)) {
            throw new InvalidOperationException("Coupon with code '" + cleanCode + "' already exists");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE) {
            if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100.0)) > 0) {
                throw new InvalidOperationException("Percentage discount cannot exceed 100%");
            }
        }

        Coupon coupon = new Coupon(
                cleanCode,
                request.getDiscountType(),
                request.getDiscountValue().setScale(2, RoundingMode.HALF_UP),
                Boolean.TRUE.equals(request.getActive())
        );
        couponRepository.save(coupon);
        return CouponResponse.fromModel(coupon);
    }

    public void deleteCoupon(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be empty");
        }
        boolean deleted = couponRepository.deleteByCode(code.trim());
        if (!deleted) {
            throw new ResourceNotFoundException("Coupon not found with code: " + code);
        }
    }

    public Coupon getCoupon(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be empty");
        }
        return couponRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::fromModel)
                .collect(Collectors.toList());
    }

    public BigDecimal validateAndCalculateDiscount(String code, BigDecimal baseFare) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Coupon coupon = couponRepository.findByCode(code.trim())
                .orElseThrow(() -> new InvalidOperationException("Invalid or non-existent coupon code: " + code));

        if (!coupon.isActive()) {
            throw new InvalidOperationException("Coupon '" + code + "' is inactive or expired");
        }

        DiscountStrategy strategy = discountStrategyResolver.resolve(coupon.getDiscountType())
                .orElseThrow(() -> new InvalidOperationException("Unsupported discount type: " + coupon.getDiscountType()));

        return strategy.calculateDiscount(baseFare, coupon);
    }
}
