package com.rapidalert.recipient.utils;

import com.rapidalert.recipient.entity.Geolocation;
import java.util.List;

public class PolygonUtils {

    /**
     * Ray-Casting algorithm to check if a Geolocation point is inside a polygon defined by a list of points.
     */
    public static boolean isPointInPolygon(Geolocation point, List<Geolocation> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        int intersectCount = 0;
        for (int j = 0; j < polygon.size() - 1; j++) {
            if (rayCastIntersect(point, polygon.get(j), polygon.get(j + 1))) {
                intersectCount++;
            }
        }
        // Check the closing line segment
        if (rayCastIntersect(point, polygon.get(polygon.size() - 1), polygon.get(0))) {
            intersectCount++;
        }

        return (intersectCount % 2) == 1; // odd = inside, even = outside
    }

    private static boolean rayCastIntersect(Geolocation point, Geolocation vertA, Geolocation vertB) {
        double aY = vertA.getLatitude();
        double bY = vertB.getLatitude();
        double aX = vertA.getLongitude();
        double bX = vertB.getLongitude();
        double pY = point.getLatitude();
        double pX = point.getLongitude();

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false;
        }

        double m = (aY - bY) / (aX - bX);
        double bee = (-aX) * m + aY;
        double x = (pY - bee) / m;

        return x > pX;
    }
}
