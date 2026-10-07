package com.ridehailing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Ride {
    private String id;
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

    public Ride() {
        this.status = RideStatus.ONGOING;
        this.createdAt = LocalDateTime.now();
    }

    public Ride(String id, String userId, String driverId, Location pickupLocation,
                CarType requestedCarType, CarType actualCarType) {
        this.id = id;
        this.userId = userId;
        this.driverId = driverId;
        this.pickupLocation = pickupLocation;
        this.requestedCarType = requestedCarType;
        this.actualCarType = actualCarType;
        this.status = RideStatus.ONGOING;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ride ride = (Ride) o;
        return Objects.equals(id, ride.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ride{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", driverId='" + driverId + '\'' +
                ", requestedCarType=" + requestedCarType +
                ", actualCarType=" + actualCarType +
                ", status=" + status +
                ", distanceKm=" + distanceKm +
                ", finalFare=" + finalFare +
                '}';
    }
}
