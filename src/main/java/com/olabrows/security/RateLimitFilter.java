package com.olabrows.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Max requests per window per IP per endpoint
    private static final int AI_MAX = 10;        // 10 AI requests per minute
    private static final int PAYMENT_MAX = 5;    // 5 payment attempts per minute
    private static final int SUBSCRIBE_MAX = 3;  // 3 subscribe attempts per minute
    private static final long WINDOW_MS = 60_000; // 1 minute window

    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate limit specific endpoints
        int limit = -1;
        if (path.contains("/api/ai/chat") && "POST".equals(method)) {
            limit = AI_MAX;
        } else if (path.contains("/api/payment/create-intent") && "POST".equals(method)) {
            limit = PAYMENT_MAX;
        } else if (path.contains("/api/subscribers") && "POST".equals(method)) {
            limit = SUBSCRIBE_MAX;
        }

        if (limit > 0) {
            String ip = getClientIp(request);
            String key = ip + ":" + path;
            long now = System.currentTimeMillis();

            requestCounts.compute(key, (k, val) -> {
                if (val == null || now - val[0] > WINDOW_MS) {
                    return new long[]{now, 1};
                }
                val[1]++;
                return val;
            });

            long[] data = requestCounts.get(key);
            if (data != null && data[1] > limit) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
