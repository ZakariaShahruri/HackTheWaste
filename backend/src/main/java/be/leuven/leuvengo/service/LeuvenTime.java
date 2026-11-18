package be.leuven.leuvengo.service;

import java.time.ZoneId;

/** Calendar maths happens in Leuven's timezone, whatever timezone the server runs in. */
public final class LeuvenTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private LeuvenTime() {}
}
