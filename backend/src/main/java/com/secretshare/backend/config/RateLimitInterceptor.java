package com.secretshare.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int MAX_TRACKED_CLIENTS = 100_000;
    private static final int CLEANUP_INTERVAL = 256;

    private final Map<String, Window> createWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> viewWindows   = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();
    private final boolean trustForwardedHeaders;
    private final int maxTrackedClients;

    public RateLimitInterceptor(
            @Value("${app.proxy.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this(trustForwardedHeaders, MAX_TRACKED_CLIENTS);
    }

    RateLimitInterceptor(boolean trustForwardedHeaders, int maxTrackedClients) {
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.maxTrackedClients = maxTrackedClients;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String ip     = getClientIp(request);
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        boolean limited;
        if ("POST".equalsIgnoreCase(method) && "/api/secrets".equals(uri)) {
            limited = !tryConsume(createWindows, ip, CREATE_LIMIT);
        } else if ("GET".equalsIgnoreCase(method) && uri.startsWith("/api/secrets/")) {
            limited = !tryConsume(viewWindows, ip, VIEW_LIMIT);
        } else {
            return true;
        }

        if (!limited) return true;

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many requests — please slow down\"}");
        return false;
    }

    private boolean tryConsume(Map<String, Window> windows, String ip, int limit) {
        long now = Instant.now().toEpochMilli();
        if (requestsSinceCleanup.incrementAndGet() >= CLEANUP_INTERVAL) {
            requestsSinceCleanup.set(0);
            removeExpiredWindows(createWindows, now);
            removeExpiredWindows(viewWindows, now);
        }

        Window window = windows.get(ip);
        if (window == null) {
            synchronized (windows) {
                window = windows.get(ip);
                if (window == null) {
                    if (windows.size() >= maxTrackedClients) {
                        removeExpiredWindows(windows, now);
                    }
                    if (windows.size() >= maxTrackedClients) {
                        return false;
                    }
                    window = new Window(now);
                    windows.put(ip, window);
                }
            }
        }
        return window.tryConsume(limit, now);
    }

    private static void removeExpiredWindows(Map<String, Window> windows, long now) {
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String getClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart;

        Window(long windowStart) {
            this.windowStart = windowStart;
        }

        synchronized boolean tryConsume(int limit, long now) {
            if (now - windowStart >= WINDOW_MS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }

        boolean isExpired(long now) {
            return now - windowStart >= WINDOW_MS;
        }
    }
}
