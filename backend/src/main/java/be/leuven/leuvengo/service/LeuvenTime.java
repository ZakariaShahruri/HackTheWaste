package be.leuven.leuvengo.service;

import java.time.ZoneId;

/** Calendar maths happens in Leuven's timezone, whatever timezone the server runs in. */
public final class LeuvenTime {

    /** Also usable in annotations such as {@code @Scheduled(zone = ...)}. */
    public static final String ZONE_ID = "Europe/Brussels";
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    private LeuvenTime() {}
}
