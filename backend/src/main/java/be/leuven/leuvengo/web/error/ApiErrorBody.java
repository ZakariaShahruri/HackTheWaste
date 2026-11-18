package be.leuven.leuvengo.web.error;

import java.time.Instant;

/** JSON body returned for every failed API request. */
public record ApiErrorBody(Instant timestamp, int status, String error, String message, String path) {}
