package com.secretshare.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Requests allowed per window per IP
    private static final int CREATE_LIMIT = 10;
    private static final int VIEW_LIMIT   = 30;
    private static final long WINDOW_MS   = 60_000;

    private final Map<String, Window> createWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> viewWindows   = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String ip     = getClientIp(request);
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        boolean limited;
        if ("POST".equalsIgnoreCase(method) && "/api/secrets".equals(uri)) {
            limited = !createWindows.computeIfAbsent(ip, k -> new Window()).tryConsume(CREATE_LIMIT);
        } else if ("GET".equalsIgnoreCase(method) && uri.startsWith("/api/secrets/")) {
            limited = !viewWindows.computeIfAbsent(ip, k -> new Window()).tryConsume(VIEW_LIMIT);
        } else {
            return true;
        }

        if (!limited) return true;

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many requests — please slow down\"}");
        return false;
    }

    private static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().toEpochMilli();

        synchronized boolean tryConsume(int limit) {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart >= WINDOW_MS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
