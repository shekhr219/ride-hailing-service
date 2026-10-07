package com.ridehailing.model;

import java.util.Objects;

public class Driver {
    private String id;
    private String name;
    private String phone;
    private CarType carType;
    private double rating;
    private Location currentLocation;
    private DriverStatus status;

    public Driver() {
        this.status = DriverStatus.AVAILABLE;
    }

    public Driver(String id, String name, String phone, CarType carType, double rating, Location currentLocation) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.carType = carType;
        this.rating = rating;
        this.currentLocation = currentLocation;
        this.status = DriverStatus.AVAILABLE;
    }

    public synchronized boolean tryAssign() {
        if (this.status == DriverStatus.AVAILABLE) {
            this.status = DriverStatus.ON_RIDE;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        this.status = DriverStatus.AVAILABLE;
    }

    public synchronized void updateLocation(Location newLocation) {
        this.currentLocation = newLocation;
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

    public synchronized Location getCurrentLocation() {
        return currentLocation;
    }

    public synchronized void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public synchronized DriverStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(DriverStatus status) {
        this.status = status;
    }

    public synchronized boolean isAvailable() {
        return this.status == DriverStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
        return Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", carType=" + carType +
                ", rating=" + rating +
                ", currentLocation=" + currentLocation +
                ", status=" + status +
                '}';
    }
}
