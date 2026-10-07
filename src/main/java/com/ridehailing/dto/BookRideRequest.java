package com.ridehailing.dto;

import com.ridehailing.model.CarType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BookRideRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @DecimalMin(value = "-90.0", message = "Pickup latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be <= 90.0")
    private double pickupLatitude;

    @DecimalMin(value = "-180.0", message = "Pickup longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be <= 180.0")
    private double pickupLongitude;

    @NotNull(message = "Requested car type is required (HATCHBACK or SEDAN)")
    private CarType requestedCarType;

    public BookRideRequest() {
    }

    public BookRideRequest(String userId, double pickupLatitude, double pickupLongitude, CarType requestedCarType) {
        this.userId = userId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.requestedCarType = requestedCarType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public CarType getRequestedCarType() {
        return requestedCarType;
    }

    public void setRequestedCarType(CarType requestedCarType) {
        this.requestedCarType = requestedCarType;
    }
}
