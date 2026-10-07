package com.ridehailing.repository;

import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryRideRepository implements RideRepository {
    private final Map<String, Ride> rideMap = new ConcurrentHashMap<>();

    @Override
    public Ride save(Ride ride) {
        if (ride == null || ride.getId() == null) {
            throw new IllegalArgumentException("Ride and Ride ID must not be null");
        }
        rideMap.put(ride.getId(), ride);
        return ride;
    }

    @Override
    public Optional<Ride> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(rideMap.get(id));
    }

    @Override
    public List<Ride> findByUserId(String userId) {
        if (userId == null) return List.of();
        return rideMap.values().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .sorted(Comparator.comparing(Ride::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> findByUserIdAndStatus(String userId, RideStatus status) {
        if (userId == null) return List.of();
        return rideMap.values().stream()
                .filter(r -> userId.equals(r.getUserId()) && r.getStatus() == status)
                .sorted(Comparator.comparing(Ride::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> findByDriverId(String driverId) {
        if (driverId == null) return List.of();
        return rideMap.values().stream()
                .filter(r -> driverId.equals(r.getDriverId()))
                .sorted(Comparator.comparing(Ride::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status) {
        if (driverId == null) return List.of();
        return rideMap.values().stream()
                .filter(r -> driverId.equals(r.getDriverId()) && r.getStatus() == status)
                .sorted(Comparator.comparing(Ride::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> findAll() {
        return new ArrayList<>(rideMap.values());
    }

    @Override
    public void clear() {
        rideMap.clear();
    }
}
