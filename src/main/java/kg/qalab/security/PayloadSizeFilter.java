package kg.qalab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class PayloadSizeFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_BYTES = 16_384;

    private static final Set<String> BODY_METHODS =
        Set.of("POST", "PUT", "PATCH");

    private final SecurityErrorWriter errorWriter;

    public PayloadSizeFilter(
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
        if (
            !request.getRequestURI().startsWith("/api/")
                || !BODY_METHODS.contains(request.getMethod())
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] payload = request
            .getInputStream()
            .readNBytes(MAX_BODY_BYTES + 1);

        if (payload.length > MAX_BODY_BYTES) {
            errorWriter.write(
                request,
                response,
                413,
                "PAYLOAD_TOO_LARGE",
                "Учебное тело запроса ограничено 16 KiB"
            );
            return;
        }

        filterChain.doFilter(
            new BufferedRequest(request, payload),
            response
        );
    }

    private static final class BufferedRequest
        extends HttpServletRequestWrapper {

        private final byte[] payload;

        private BufferedRequest(
            HttpServletRequest request,
            byte[] payload
        ) {
            super(request);
            this.payload = payload;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input =
                new ByteArrayInputStream(payload);

            return new ServletInputStream() {

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(
                    ReadListener readListener
                ) {
                    throw new UnsupportedOperationException(
                        "Synchronous API only"
                    );
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(
                    getInputStream(),
                    StandardCharsets.UTF_8
                )
            );
        }
    }
}
