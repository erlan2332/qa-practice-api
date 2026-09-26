package kg.qalab.exception;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
    int status,
    String code,
    String message,
    Map<String, String> fields,
    String requestId,
    Instant timestamp
) {

    public static ApiErrorResponse of(
        int status,
        String code,
        String message,
        Map<String, String> fields,
        HttpServletRequest request
    ) {
        return new ApiErrorResponse(
            status,
            code,
            message,
            fields,
            String.valueOf(request.getAttribute("requestId")),
            Instant.now()
        );
    }
}
