package be.leuven.leuvengo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/** Enforces {@link RateLimited} on controller methods. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;

    public RateLimitInterceptor(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RateLimited limit = method.getMethodAnnotation(RateLimited.class);
        if (limit == null) return true;

        // The real client address, even behind a proxy (see server.forward-headers-strategy).
        String key = limit.value() + ":" + request.getRemoteAddr();
        RateLimiter.Decision decision =
                limiter.tryAcquire(key, limit.requests(), Duration.ofSeconds(limit.perSeconds()));
        if (!decision.allowed()) {
            long seconds = Math.max(1, (long) Math.ceil(decision.retryAfter().toMillis() / 1000.0));
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts, try again in " + seconds + "s");
        }
        return true;
    }
}
