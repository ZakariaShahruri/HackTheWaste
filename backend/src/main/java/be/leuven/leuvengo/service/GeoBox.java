package be.leuven.leuvengo.service;

/** Lat/lng bounding box, used to pre-filter geo queries before exact distance checks. */
public record GeoBox(double latMin, double latMax, double lngMin, double lngMax) {

    private static final double METERS_PER_DEGREE_LAT = 111_320d;

    /** Smallest box that contains the circle of {@code radiusM} around the point. */
    public static GeoBox around(double lat, double lng, double radiusM) {
        double dLat = radiusM / METERS_PER_DEGREE_LAT;
        double dLng = radiusM / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new GeoBox(lat - dLat, lat + dLat, lng - dLng, lng + dLng);
    }

    public boolean contains(double lat, double lng) {
        return lat >= latMin && lat <= latMax && lng >= lngMin && lng <= lngMax;
    }
}
