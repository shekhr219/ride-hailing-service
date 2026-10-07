package com.ridehailing.controller;

import com.ridehailing.dto.BookRideRequest;
import com.ridehailing.dto.EndRideRequest;
import com.ridehailing.dto.RideResponse;
import com.ridehailing.model.RideStatus;
import com.ridehailing.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RideController {
    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/rides")
    public ResponseEntity<RideResponse> bookRide(@Valid @RequestBody BookRideRequest request) {
        RideResponse response = rideService.bookRide(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/rides/{rideId}/end")
    public ResponseEntity<RideResponse> endRide(
            @PathVariable String rideId,
            @Valid @RequestBody EndRideRequest request) {
        RideResponse response = rideService.endRide(rideId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rides/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable String rideId) {
        RideResponse response = rideService.cancelRide(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rides/{rideId}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable String rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/rides")
    public ResponseEntity<List<RideResponse>> getUserRides(
            @PathVariable String userId,
            @RequestParam(required = false) RideStatus status) {
        List<RideResponse> rides = rideService.getUserRides(userId, status);
        return ResponseEntity.ok(rides);
    }

    @GetMapping("/drivers/{driverId}/rides")
    public ResponseEntity<List<RideResponse>> getDriverRides(
            @PathVariable String driverId,
            @RequestParam(required = false) RideStatus status) {
        List<RideResponse> rides = rideService.getDriverRides(driverId, status);
        return ResponseEntity.ok(rides);
    }
}
