package kg.qalab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT = 60;
    private static final int API_LIMIT = 1200;

    private final SecurityErrorWriter errorWriter;

    private long minute;
    private int authRequests;
    private int apiRequests;

    public RateLimitFilter(
        SecurityErrorWriter errorWriter
    ) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (
            !path.startsWith("/api/")
                || path.equals("/api/health")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean authRequest =
            path.equals("/api/auth/login")
                || path.equals("/api/auth/register");

        if (!allowed(authRequest)) {
            response.setHeader("Retry-After", "60");

            errorWriter.write(
                request,
                response,
                429,
                "RATE_LIMIT",
                "Слишком много запросов. Подождите одну минуту"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private synchronized boolean allowed(
        boolean authRequest
    ) {
        long currentMinute =
            System.currentTimeMillis() / 60_000;

        if (currentMinute != minute) {
            minute = currentMinute;
            authRequests = 0;
            apiRequests = 0;
        }

        if (authRequest) {
            return ++authRequests <= AUTH_LIMIT;
        }

        return ++apiRequests <= API_LIMIT;
    }
}
