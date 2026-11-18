package be.leuven.leuvengo.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Turns exceptions into a consistent {@link ApiErrorBody}. Client errors carry a
 * human-readable message; unexpected errors are logged and returned as a
 * generic 500 so internals never leak to the caller.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorBody> invalidBody(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorBody> invalidParameters(HandlerMethodValidationException e, HttpServletRequest req) {
        String message = e.getAllErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorBody> unreadable(HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request body", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorBody> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Invalid value for '" + e.getName() + "'", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorBody> illegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    /**
     * Fallback. ResponseStatusException and Spring MVC's own exceptions (404, 405,
     * 415...) implement {@link ErrorResponse} and keep their status; anything else
     * is a bug and becomes a generic 500.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorBody> fallback(Exception e, HttpServletRequest req) {
        if (e instanceof ErrorResponse er) {
            String detail = er.getBody().getDetail();
            return body(er.getStatusCode(), detail != null ? detail : reasonOf(er.getStatusCode()), req);
        }
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side", req);
    }

    private static ResponseEntity<ApiErrorBody> body(HttpStatusCode status, String message,
                                                     HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ApiErrorBody(
                Instant.now(), status.value(), reasonOf(status), message, req.getRequestURI()));
    }

    private static String reasonOf(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }
}
