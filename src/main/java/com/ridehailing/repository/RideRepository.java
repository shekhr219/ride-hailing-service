package com.ridehailing.repository;

import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;

import java.util.List;
import java.util.Optional;

public interface RideRepository {
    Ride save(Ride ride);
    Optional<Ride> findById(String id);
    List<Ride> findByUserId(String userId);
    List<Ride> findByUserIdAndStatus(String userId, RideStatus status);
    List<Ride> findByDriverId(String driverId);
    List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status);
    List<Ride> findAll();
    void clear();
}
