package be.leuven.leuvengo.security;

import be.leuven.leuvengo.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-18T12:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);
    private static final Duration MINUTE = Duration.ofMinutes(1);

    @Test
    void allowsUpToCapacityThenBlocks() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, MINUTE).allowed()).isTrue();
        }
        RateLimiter.Decision blocked = limiter.tryAcquire("k", 3, MINUTE);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfter()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void refillsOverTime() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("k", 3, MINUTE);

        clock.advance(Duration.ofSeconds(20));
        assertThat(limiter.tryAcquire("k", 3, MINUTE).allowed()).isTrue();
        assertThat(limiter.tryAcquire("k", 3, MINUTE).allowed()).isFalse();
    }

    @Test
    void neverRefillsBeyondCapacity() {
        clock.advance(Duration.ofHours(5));
        for (int i = 0; i < 3; i++) limiter.tryAcquire("k", 3, MINUTE);
        assertThat(limiter.tryAcquire("k", 3, MINUTE).allowed()).isFalse();
    }

    @Test
    void keysHaveSeparateBudgets() {
        limiter.tryAcquire("a", 1, MINUTE);
        assertThat(limiter.tryAcquire("a", 1, MINUTE).allowed()).isFalse();
        assertThat(limiter.tryAcquire("b", 1, MINUTE).allowed()).isTrue();
    }

    @Test
    void evictsIdleBuckets() {
        limiter.tryAcquire("a", 1, MINUTE);
        clock.advance(Duration.ofHours(2));
        limiter.tryAcquire("b", 1, MINUTE);

        limiter.evictIdle();

        assertThat(limiter.size()).isEqualTo(1);
    }
}
