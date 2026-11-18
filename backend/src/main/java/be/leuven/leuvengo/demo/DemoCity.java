package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.service.GeoBox;

import java.util.List;
import java.util.Optional;

/** The Leuven the app starts with: student clans and their turf, the streets it scores, the rewards. */
final class DemoCity {

    private DemoCity() {}

    record Clan(String name, String code, String color, String emoji, int members, GeoBox territory) {}

    record Street(String name, String district, double lat, double lng) {}

    record Reward(String title, String description, String sponsor, String category, int costPoints, int stock) {}

    /** Five bands of blocky territories that together tile the city, north to south. */
    static final List<Clan> CLANS = List.of(
            clan("Project Unseen", "PUS", "#475569", "👁", 950, 50.890, 50.898, 4.670, 4.697),
            clan("LBK", "LBK", "#65a30d", "🌱", 800, 50.890, 50.898, 4.697, 4.710),
            clan("ESN", "ESN", "#3b82f6", "🌐", 1500, 50.890, 50.898, 4.710, 4.730),

            clan("Run for Specials", "RFS", "#f59e0b", "🌟", 500, 50.882, 50.890, 4.670, 4.684),
            clan("Apolloon", "APO", "#1d4ed8", "🏃", 1200, 50.882, 50.890, 4.684, 4.699),
            clan("Industria", "IND", "#1e3a8a", "🏭", 2200, 50.882, 50.890, 4.699, 4.710),
            clan("Atmosphere", "ATM", "#0ea5e9", "🌍", 900, 50.882, 50.890, 4.710, 4.720),
            clan("Humasol", "HUM", "#eab308", "☀️", 1300, 50.882, 50.890, 4.720, 4.730),

            clan("Psychokring", "PSY", "#9333ea", "🧠", 1600, 50.874, 50.882, 4.670, 4.684),
            clan("Ekonomika", "EKO", "#10b981", "📊", 2400, 50.874, 50.882, 4.684, 4.699),
            clan("Thomas Morus", "TMO", "#ca8a04", "📜", 700, 50.874, 50.882, 4.699, 4.708),
            clan("VRG-Crimen", "VRG", "#991b1b", "⚖️", 1300, 50.874, 50.882, 4.708, 4.719),
            clan("UCLL", "UCL", "#2563eb", "🎓", 1900, 50.874, 50.882, 4.719, 4.730),

            clan("Wina", "WIN", "#ec4899", "🧮", 1100, 50.866, 50.874, 4.670, 4.684),
            clan("Lerkeveld", "LRK", "#78350f", "🏠", 1150, 50.866, 50.874, 4.684, 4.699),
            clan("VTK", "VTK", "#dc2626", "⚙️", 2800, 50.866, 50.874, 4.699, 4.708),
            clan("HMV", "HMV", "#a78bfa", "🎵", 750, 50.866, 50.874, 4.708, 4.719),
            clan("Pedal", "PED", "#0d9488", "🚴", 850, 50.866, 50.874, 4.719, 4.730),

            clan("Farmaceutica", "FAR", "#14b8a6", "💊", 1400, 50.850, 50.866, 4.670, 4.693),
            clan("Politika", "POL", "#0f172a", "🗳️", 1000, 50.850, 50.866, 4.693, 4.712),
            clan("Runner's High", "RNH", "#f97316", "💨", 600, 50.850, 50.866, 4.712, 4.730));

    static final List<Street> STREETS = List.of(
            new Street("Oude Markt", "Centrum", 50.8785, 4.7000),
            new Street("Naamsestraat", "Centrum", 50.8752, 4.7008),
            new Street("Ladeuzeplein", "Centrum", 50.8777, 4.7040),
            new Street("Tiensestraat", "Centrum", 50.8783, 4.7072),
            new Street("Bondgenotenlaan", "Centrum", 50.8803, 4.7090),
            new Street("Diestsestraat", "Centrum", 50.8830, 4.7090),
            new Street("Brusselsestraat", "Centrum", 50.8800, 4.6960),
            new Street("Parkstraat", "Centrum", 50.8730, 4.7035),
            new Street("Martelarenplein", "Station", 50.8812, 4.7150),
            new Street("Kessel-Lo Station", "Kessel-Lo", 50.8820, 4.7195),
            new Street("Vaartkom", "Noord", 50.8880, 4.7045),
            new Street("Sint-Maartensdal", "Noord", 50.8855, 4.6960),
            new Street("Arenberg Campus", "Heverlee", 50.8640, 4.6790),
            new Street("Gasthuisberg", "Gasthuisberg", 50.8775, 4.6735));

    static final List<Reward> REWARDS = List.of(
            new Reward("Free Stella @ Oude Markt", "One pint at any participating bar around Oude Markt.",
                    "City of Leuven", "Drinks", 120, 50),
            new Reward("Cinema Ticket - Kinepolis", "Standard ticket valid any weekday.",
                    "Kinepolis", "Entertainment", 200, 30),
            new Reward("LIBIS Library Print Credit (€5)", "Printing credit on your student card.",
                    "KU Leuven Libraries", "Study", 80, 100),
            new Reward("Velo Bike Day Pass", "24h shared-bike access across Leuven.",
                    "Velo", "Transport", 60, 200),
            new Reward("Alma Lunch Voucher", "Free lunch at any Alma student restaurant.",
                    "Alma", "Food", 100, 75),
            new Reward("STUK Concert Ticket", "Entry to a STUK arts centre event.",
                    "STUK", "Entertainment", 220, 20),
            new Reward("Stadsmagazijn Tour", "Behind-the-scenes city services tour.",
                    "City of Leuven", "Experience", 30, 500),
            new Reward("Leuven Go Hoodie", "Limited edition campaign hoodie.",
                    "City of Leuven", "Merch", 350, 40));

    /** The clan whose territory the point lies in (on a shared border, the first one listed). */
    static Optional<Clan> clanAt(double lat, double lng) {
        return CLANS.stream().filter(c -> c.territory().contains(lat, lng)).findFirst();
    }

    static Street street(String name) {
        return STREETS.stream().filter(s -> s.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No demo street " + name));
    }

    /** The territory as a GeoJSON Polygon, which the frontend draws as-is. */
    static String geoJson(GeoBox box) {
        // GeoJSON order is [lng, lat]; the ring ends where it starts.
        return "{\"type\":\"Polygon\",\"coordinates\":[[[%s,%s],[%s,%s],[%s,%s],[%s,%s],[%s,%s]]]}".formatted(
                box.lngMin(), box.latMin(), box.lngMin(), box.latMax(), box.lngMax(), box.latMax(),
                box.lngMax(), box.latMin(), box.lngMin(), box.latMin());
    }

    private static Clan clan(String name, String code, String color, String emoji, int members,
                             double latMin, double latMax, double lngMin, double lngMax) {
        return new Clan(name, code, color, emoji, members, new GeoBox(latMin, latMax, lngMin, lngMax));
    }
}
