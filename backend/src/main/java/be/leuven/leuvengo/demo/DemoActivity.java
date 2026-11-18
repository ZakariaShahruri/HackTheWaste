package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.service.GeoBox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * A week of believable reports around the demo city: dirty spots people keep
 * reporting (two of them already cleaned up), a few clean streets, and
 * one-off reports scattered around the centre. Times are relative to
 * {@code now}, and the same seed always gives the same week.
 */
final class DemoActivity {

    private DemoActivity() {}

    static final Duration SPAN = Duration.ofDays(7);

    /** How far reports at one spot stray from it (one standard deviation). */
    private static final double SPREAD_M = 12;
    private static final double SCORE_SPREAD = 8;
    private static final int SCATTERED_REPORTS = 10;
    private static final GeoBox CENTRE = new GeoBox(50.872, 50.886, 4.692, 4.716);
    private static final List<String> SCATTERED_TAGS =
            List.of("plastic", "paper", "cigarettes", "glass", "food_waste");
    /** Share of reporters who report on their own clan's turf. */
    private static final double LOCAL_REPORTERS = 0.7;
    private static final int REPORTERS = 25;
    private static final double METERS_PER_DEGREE_LAT = 111_320;

    /** A spot reported {@code reports} times between {@code firstHoursAgo} and {@code lastHoursAgo}. */
    record Spot(String street, int reports, int meanScore, int firstHoursAgo, int lastHoursAgo,
                Integer cleanedHoursAgo, List<String> tags, List<String> descriptions) {}

    sealed interface Event permits Report, Cleanup {
        Instant at();
    }

    /** {@code spot} is null for scattered one-off reports. */
    record Report(Instant at, String spot, double lat, double lng, int score, String clan,
                  String reporterId, List<String> tags, String description) implements Event {}

    /** A crew finishes the work order at this spot. */
    record Cleanup(Instant at, String spot) implements Event {}

    static final List<Spot> SPOTS = List.of(
            new Spot("Oude Markt", 7, 18, 30, 2, null, List.of("glass", "plastic", "metal"), List.of(
                    "Broken glass and empty cans across the square after a night out.",
                    "Plastic cups and bottles piled up around the benches.",
                    "Overflowing bins with bags dumped next to them.")),
            new Spot("Martelarenplein", 5, 35, 20, 1, null, List.of("paper", "plastic"), List.of(
                    "Coffee cups and food packaging around the bike racks.",
                    "The bin by the station entrance is full and spilling over.")),
            new Spot("Naamsestraat", 4, 40, 40, 6, null, List.of("cardboard", "food_waste"), List.of(
                    "Takeaway boxes and napkins left on the pavement.",
                    "Pizza boxes stacked against a doorway.")),
            new Spot("Vaartkom", 3, 45, 60, 20, null, List.of("plastic", "glass"), List.of(
                    "Plastic bottles along the quay wall.",
                    "Bags of rubbish left next to the water.")),
            new Spot("Sint-Maartensdal", 3, 50, 70, 30, null, List.of("bulky_waste", "cardboard"), List.of(
                    "A mattress and flattened boxes dumped by the flats.")),
            new Spot("Ladeuzeplein", 3, 30, 110, 90, 80, List.of("food_waste", "paper"), List.of(
                    "Food scraps and wrappers left behind after the market.")),
            new Spot("Tiensestraat", 4, 25, 150, 130, 120, List.of("glass", "metal"), List.of(
                    "Cans and broken glass in the gutter outside the bars.")),
            new Spot("Diestsestraat", 2, 60, 26, 10, null, List.of("cigarettes", "paper"), List.of(
                    "Cigarette butts and flyers along the shop fronts.")),
            new Spot("Kessel-Lo Station", 2, 55, 50, 14, null, List.of("paper", "plastic"), List.of(
                    "Flyers and a few bottles around the bus stop.")),
            new Spot("Arenberg Campus", 2, 94, 18, 4, null, List.of(), List.of(
                    "Clean paths and empty bins, nothing to report.")),
            new Spot("Bondgenotenlaan", 2, 90, 12, 3, null, List.of(), List.of(
                    "Freshly swept pavement with the bins just emptied.")),
            new Spot("Parkstraat", 1, 92, 8, 8, null, List.of(), List.of(
                    "A tidy street with no litter in sight.")));

    /** Every report and cleanup of the week, oldest first. */
    static List<Event> plan(Instant now, Random rnd) {
        List<Event> events = new ArrayList<>();
        for (Spot spot : SPOTS) {
            DemoCity.Street street = DemoCity.street(spot.street());
            for (int i = 0; i < spot.reports(); i++) {
                double lat = street.lat() + rnd.nextGaussian() * SPREAD_M / METERS_PER_DEGREE_LAT;
                double lng = street.lng() + rnd.nextGaussian() * SPREAD_M / metersPerDegreeLng(street.lat());
                events.add(new Report(hoursAgo(now, reportHoursAgo(spot, i), rnd), spot.street(), lat, lng,
                        score(spot.meanScore(), rnd), clan(lat, lng, rnd), reporter(rnd),
                        spot.tags(), spot.descriptions().get(rnd.nextInt(spot.descriptions().size()))));
            }
            if (spot.cleanedHoursAgo() != null) {
                events.add(new Cleanup(now.minus(Duration.ofHours(spot.cleanedHoursAgo())), spot.street()));
            }
        }
        for (int i = 0; i < SCATTERED_REPORTS; i++) {
            double lat = CENTRE.latMin() + rnd.nextDouble() * (CENTRE.latMax() - CENTRE.latMin());
            double lng = CENTRE.lngMin() + rnd.nextDouble() * (CENTRE.lngMax() - CENTRE.lngMin());
            Instant at = now.minus(Duration.ofMinutes(30 + rnd.nextLong(SPAN.toMinutes() - 60)));
            String tag = SCATTERED_TAGS.get(rnd.nextInt(SCATTERED_TAGS.size()));
            events.add(new Report(at, null, lat, lng, 45 + rnd.nextInt(46), clan(lat, lng, rnd),
                    reporter(rnd), List.of(tag), null));
        }
        events.sort(Comparator.comparing(Event::at));
        return events;
    }

    /** Spread evenly from the first to the last report time. */
    private static double reportHoursAgo(Spot spot, int i) {
        if (spot.reports() == 1) return spot.lastHoursAgo();
        double step = (spot.firstHoursAgo() - spot.lastHoursAgo()) / (double) (spot.reports() - 1);
        return spot.firstHoursAgo() - step * i;
    }

    /** Up to 20 minutes either way, so reports don't land on the hour. */
    private static Instant hoursAgo(Instant now, double hours, Random rnd) {
        long minutes = Math.round(hours * 60) + rnd.nextInt(41) - 20;
        return now.minus(Duration.ofMinutes(Math.max(minutes, 5)));
    }

    private static int score(int mean, Random rnd) {
        return CleanlinessScale.clamp((int) Math.round(mean + rnd.nextGaussian() * SCORE_SPREAD));
    }

    private static String clan(double lat, double lng, Random rnd) {
        if (rnd.nextDouble() < LOCAL_REPORTERS) {
            var local = DemoCity.clanAt(lat, lng);
            if (local.isPresent()) return local.get().code();
        }
        return DemoCity.CLANS.get(rnd.nextInt(DemoCity.CLANS.size())).code();
    }

    private static String reporter(Random rnd) {
        return "demo-" + rnd.nextInt(REPORTERS);
    }

    private static double metersPerDegreeLng(double lat) {
        return METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat));
    }
}
