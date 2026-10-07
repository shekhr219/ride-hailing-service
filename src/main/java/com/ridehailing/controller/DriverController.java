package com.ridehailing.controller;

import com.ridehailing.dto.DriverRegistrationRequest;
import com.ridehailing.dto.DriverResponse;
import com.ridehailing.dto.UpdateLocationRequest;
import com.ridehailing.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {
    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody DriverRegistrationRequest request) {
        DriverResponse response = driverService.registerDriver(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String driverId) {
        DriverResponse response = driverService.getDriverById(driverId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers(@RequestParam(required = false) Boolean available) {
        if (Boolean.TRUE.equals(available)) {
            return ResponseEntity.ok(driverService.getAvailableDrivers());
        }
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @PatchMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateLocationRequest request) {
        DriverResponse response = driverService.updateLocation(driverId, request);
        return ResponseEntity.ok(response);
    }
}
