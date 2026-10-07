package com.ridehailing.dto;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;

public class DriverResponse {
    private String id;
    private String name;
    private String phone;
    private CarType carType;
    private double rating;
    private Location currentLocation;
    private DriverStatus status;

    public DriverResponse() {
    }

    public DriverResponse(String id, String name, String phone, CarType carType, double rating, Location currentLocation, DriverStatus status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.carType = carType;
        this.rating = rating;
        this.currentLocation = currentLocation;
        this.status = status;
    }

    public static DriverResponse fromModel(Driver driver) {
        if (driver == null) return null;
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getPhone(),
                driver.getCarType(),
                driver.getRating(),
                driver.getCurrentLocation(),
                driver.getStatus()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CarType getCarType() {
        return carType;
    }

    public void setCarType(CarType carType) {
        this.carType = carType;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}
