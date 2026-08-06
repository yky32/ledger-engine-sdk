package com.altech.ledger.sdk.error;

import com.altech.ledger.sdk.*;
import com.altech.ledger.sdk.json.JsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Maps HTTP status + body (engine {@link ApiError}) to typed {@link LedgerException}s.
 */
public final class ErrorMapper {
    private static final ObjectMapper MAPPER = JsonSupport.mapper();

    private ErrorMapper() {}

    public static LedgerException fromHttp(int status, String body, String requestId,
                                           String retryAfterHeader) {
        ApiError apiError = parse(body).orElse(null);
        String code = apiError != null && apiError.getCode() != null
            ? apiError.getCode()
            : defaultCode(status);
        String message = apiError != null && apiError.getMessage() != null
            ? apiError.getMessage()
            : "Unexpected HTTP " + status;

        return switch (status) {
            case 400, 422 -> new LedgerValidationException(status, code, message, body, requestId, apiError);
            case 401, 403 -> new LedgerAuthException(status, code, message, body, requestId, apiError);
            case 404 -> new LedgerNotFoundException(status, code, message, body, requestId, apiError);
            case 409 -> new LedgerConflictException(status, code, message, body, requestId, apiError);
            case 429 -> new LedgerRateLimitException(status, code, message, body, requestId, apiError,
                parseRetryAfter(retryAfterHeader));
            default -> {
                if (status >= 500) {
                    yield new LedgerServerException(status, code, message, body, requestId, apiError);
                }
                yield new LedgerException(message, null, status, code, body, requestId, apiError);
            }
        };
    }

    public static Optional<ApiError> parse(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            ApiError err = MAPPER.readValue(body, ApiError.class);
            if (err.getStatus() == 0 && err.getCode() == null && err.getMessage() == null) {
                return Optional.empty();
            }
            return Optional.of(err);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String defaultCode(int status) {
        return switch (status) {
            case 400, 422 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 429 -> "RATE_LIMITED";
            default -> status >= 500 ? "SERVER_ERROR" : "HTTP_" + status;
        };
    }

    private static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
