package com.Alex.RiotTrackerApplication.rate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserRateLimiter {

    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long WINDOW_MS = 60000;

    public Mono<Void> checkAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            return Mono.error(new IllegalArgumentException("IP is null or empty"));
        }

        long now = System.currentTimeMillis();

        requestCounts.compute(ip, (key, entry) -> {
            if (entry == null || now - entry.windowStart > WINDOW_MS) {

                return new RateLimitEntry(now, 1);
            } else {

                entry.count++;
                return entry;
            }
        });

        RateLimitEntry entry = requestCounts.get(ip);

        if (entry.count > MAX_REQUESTS_PER_MINUTE) {
            return Mono.error(new IllegalStateException(
                    "Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " requests per minute"
            ));
        }

        return Mono.empty();
    }


    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > WINDOW_MS
        );
    }

    private static class RateLimitEntry {
        long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
