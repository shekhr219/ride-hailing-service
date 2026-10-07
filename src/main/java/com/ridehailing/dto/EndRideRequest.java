package com.ridehailing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class EndRideRequest {
    @NotBlank(message = "Driver ID is required")
    private String driverId;

    @DecimalMin(value = "-90.0", message = "Dropoff latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Dropoff latitude must be <= 90.0")
    private double dropoffLatitude;

    @DecimalMin(value = "-180.0", message = "Dropoff longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Dropoff longitude must be <= 180.0")
    private double dropoffLongitude;

    private String couponCode;

    public EndRideRequest() {
    }

    public EndRideRequest(String driverId, double dropoffLatitude, double dropoffLongitude, String couponCode) {
        this.driverId = driverId;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.couponCode = couponCode;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
