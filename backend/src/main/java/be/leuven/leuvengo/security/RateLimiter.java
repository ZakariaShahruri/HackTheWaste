package be.leuven.leuvengo.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token buckets keyed by an arbitrary string (e.g. "login:1.2.3.4").
 * Good enough for a single instance; a multi-instance deploy would move this
 * to Redis.
 */
@Component
public class RateLimiter {

    /** Outcome of a request: allowed, or how long until the next token. */
    public record Decision(boolean allowed, Duration retryAfter) {}

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public Decision tryAcquire(String key, int capacity, Duration period) {
        long now = nowNanos();
        double refillPerNano = capacity / (double) period.toNanos();
        Bucket bucket = buckets.computeIfAbsent(key, k -> {
            Bucket b = new Bucket();
            b.tokens = capacity;
            b.lastRefillNanos = now;
            return b;
        });
        synchronized (bucket) {
            bucket.tokens = Math.min(capacity, bucket.tokens + (now - bucket.lastRefillNanos) * refillPerNano);
            bucket.lastRefillNanos = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return new Decision(true, Duration.ZERO);
            }
            long waitNanos = (long) Math.ceil((1 - bucket.tokens) / refillPerNano);
            return new Decision(false, Duration.ofNanos(waitNanos));
        }
    }

    /** Drops buckets nobody has touched for an hour so memory stays bounded. */
    @Scheduled(fixedDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void evictIdle() {
        long cutoff = nowNanos() - Duration.ofHours(1).toNanos();
        buckets.values().removeIf(b -> b.lastRefillNanos < cutoff);
    }

    int size() {
        return buckets.size();
    }

    private long nowNanos() {
        return Duration.ofMillis(clock.millis()).toNanos();
    }
}
