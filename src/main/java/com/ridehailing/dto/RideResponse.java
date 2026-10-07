package com.ridehailing.dto;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RideResponse {
    private String rideId;
    private String userId;
    private String driverId;
    private Location pickupLocation;
    private Location dropoffLocation;
    private CarType requestedCarType;
    private CarType actualCarType;
    private RideStatus status;
    private Double distanceKm;
    private BigDecimal fare;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal finalFare;
    private BigDecimal cancellationFee;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public RideResponse() {
    }

    public RideResponse(String rideId, String userId, String driverId, Location pickupLocation,
                        Location dropoffLocation, CarType requestedCarType, CarType actualCarType,
                        RideStatus status, Double distanceKm, BigDecimal fare, String couponCode,
                        BigDecimal discountAmount, BigDecimal finalFare, BigDecimal cancellationFee,
                        LocalDateTime createdAt, LocalDateTime completedAt) {
        this.rideId = rideId;
        this.userId = userId;
        this.driverId = driverId;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.requestedCarType = requestedCarType;
        this.actualCarType = actualCarType;
        this.status = status;
        this.distanceKm = distanceKm;
        this.fare = fare;
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
        this.finalFare = finalFare;
        this.cancellationFee = cancellationFee;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static RideResponse fromModel(Ride ride) {
        if (ride == null) return null;
        return new RideResponse(
                ride.getId(),
                ride.getUserId(),
                ride.getDriverId(),
                ride.getPickupLocation(),
                ride.getDropoffLocation(),
                ride.getRequestedCarType(),
                ride.getActualCarType(),
                ride.getStatus(),
                ride.getDistanceKm(),
                ride.getFare(),
                ride.getCouponCode(),
                ride.getDiscountAmount(),
                ride.getFinalFare(),
                ride.getCancellationFee(),
                ride.getCreatedAt(),
                ride.getCompletedAt()
        );
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(Location pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Location getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(Location dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    public CarType getRequestedCarType() {
        return requestedCarType;
    }

    public void setRequestedCarType(CarType requestedCarType) {
        this.requestedCarType = requestedCarType;
    }

    public CarType getActualCarType() {
        return actualCarType;
    }

    public void setActualCarType(CarType actualCarType) {
        this.actualCarType = actualCarType;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
