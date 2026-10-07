package com.ridehailing.service;

import com.ridehailing.dto.DriverRegistrationRequest;
import com.ridehailing.dto.DriverResponse;
import com.ridehailing.dto.UpdateLocationRequest;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;
import com.ridehailing.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DriverService {
    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse registerDriver(DriverRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        if (driverRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new InvalidOperationException("Driver with phone " + request.getPhone() + " is already registered");
        }

        String driverId = "DRV-" + UUID.randomUUID().toString().substring(0, 8);
        Location location = new Location(request.getLatitude(), request.getLongitude());
        Driver driver = new Driver(
                driverId,
                request.getName().trim(),
                request.getPhone().trim(),
                request.getCarType(),
                request.getRating(),
                location
        );
        driverRepository.save(driver);
        return DriverResponse.fromModel(driver);
    }

    public DriverResponse getDriverById(String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        return DriverResponse.fromModel(driver);
    }

    public DriverResponse updateLocation(String driverId, UpdateLocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Location update request cannot be null");
        }
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        Location newLocation = new Location(request.getLatitude(), request.getLongitude());
        driver.updateLocation(newLocation);
        driverRepository.save(driver);
        return DriverResponse.fromModel(driver);
    }

    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(DriverResponse::fromModel)
                .collect(Collectors.toList());
    }

    public List<DriverResponse> getAvailableDrivers() {
        return driverRepository.findAvailableDrivers().stream()
                .map(DriverResponse::fromModel)
                .collect(Collectors.toList());
    }
}
