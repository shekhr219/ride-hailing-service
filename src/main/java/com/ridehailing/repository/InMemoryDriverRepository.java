package com.ridehailing.repository;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryDriverRepository implements DriverRepository {
    private final Map<String, Driver> driverMap = new ConcurrentHashMap<>();

    @Override
    public Driver save(Driver driver) {
        if (driver == null || driver.getId() == null) {
            throw new IllegalArgumentException("Driver and Driver ID must not be null");
        }
        driverMap.put(driver.getId(), driver);
        return driver;
    }

    @Override
    public Optional<Driver> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(driverMap.get(id));
    }

    @Override
    public Optional<Driver> findByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return driverMap.values().stream()
                .filter(d -> phone.equals(d.getPhone()))
                .findFirst();
    }

    @Override
    public List<Driver> findAll() {
        return new ArrayList<>(driverMap.values());
    }

    @Override
    public List<Driver> findAvailableDrivers() {
        return driverMap.values().stream()
                .filter(d -> d.getStatus() == DriverStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Driver> findAvailableDriversByCarType(CarType carType) {
        return driverMap.values().stream()
                .filter(d -> d.getStatus() == DriverStatus.AVAILABLE && d.getCarType() == carType)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return id != null && driverMap.containsKey(id);
    }

    @Override
    public void clear() {
        driverMap.clear();
    }
}
