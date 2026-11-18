package be.leuven.leuvengo.service;

/** The area Leuven Go accepts reports for: the city of Leuven with a small margin. */
public final class LeuvenArea {

    public static final GeoBox BOUNDS = new GeoBox(50.83, 50.93, 4.62, 4.79);

    private LeuvenArea() {}

    public static boolean contains(double lat, double lng) {
        return BOUNDS.contains(lat, lng);
    }
}
