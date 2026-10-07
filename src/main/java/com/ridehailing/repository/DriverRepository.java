package com.ridehailing.repository;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {
    Driver save(Driver driver);
    Optional<Driver> findById(String id);
    Optional<Driver> findByPhone(String phone);
    List<Driver> findAll();
    List<Driver> findAvailableDrivers();
    List<Driver> findAvailableDriversByCarType(CarType carType);
    boolean existsById(String id);
    void clear();
}
