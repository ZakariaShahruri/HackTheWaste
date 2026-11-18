package be.leuven.leuvengo.domain;

/**
 * The single cleanliness scale used across the app: a 0-100 score where
 * 100 is spotless and 0 is covered in waste, plus a coarse 0-5 rating
 * (0 = filthy, 5 = pristine) for places that want a star-like value.
 */
public final class CleanlinessScale {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;
    public static final int MAX_RATING = 5;

    private CleanlinessScale() {}

    public static int clamp(int score) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    /** 0-100 score to a 0-5 rating, rounding to the nearest step. */
    public static int toRating(int score) {
        return Math.round(clamp(score) * MAX_RATING / (float) MAX_SCORE);
    }

    /** 0-5 rating to the score in the middle of its band. */
    public static int fromRating(int rating) {
        int bounded = Math.max(0, Math.min(MAX_RATING, rating));
        return bounded * MAX_SCORE / MAX_RATING;
    }

    public static String label(int score) {
        if (score >= 80) return "Clean";
        if (score >= 60) return "Mild litter";
        if (score >= 40) return "Moderate waste";
        return "Critical";
    }
}
