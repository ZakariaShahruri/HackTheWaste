package be.leuven.leuvengo.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Caps how often one client (by IP) may call an endpoint: at most
 * {@code requests} calls per {@code perSeconds}, refilled continuously.
 * Excess calls get a 429 with a Retry-After header.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /** Bucket name; endpoints sharing a name share a budget. */
    String value();

    int requests();

    int perSeconds();
}
