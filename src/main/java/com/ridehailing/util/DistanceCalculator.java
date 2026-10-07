package com.ridehailing.util;

import com.ridehailing.model.Location;

public interface DistanceCalculator {
    double calculateDistanceKm(Location start, Location end);
}
