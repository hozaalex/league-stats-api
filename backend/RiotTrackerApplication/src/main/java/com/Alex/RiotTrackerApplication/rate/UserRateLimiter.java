package com.Alex.RiotTrackerApplication.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserRateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Mono<Void> checkAllowed(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.simple(20, Duration.ofMinutes(1)))
                        .build()
        );

        if (bucket.tryConsume(1)) {
            return Mono.empty();
        } else {
            return Mono.error(new IllegalStateException("Rate limit exceeded"));
        }
    }
}

