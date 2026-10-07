package com.ridehailing.util;

import com.ridehailing.model.Location;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double calculateDistanceKm(Location start, Location end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end locations must not be null");
        }

        if (start.equals(end)) {
            return 0.0;
        }

        double lat1Rad = Math.toRadians(start.getLatitude());
        double lon1Rad = Math.toRadians(start.getLongitude());
        double lat2Rad = Math.toRadians(end.getLatitude());
        double lon2Rad = Math.toRadians(end.getLongitude());

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }
}
