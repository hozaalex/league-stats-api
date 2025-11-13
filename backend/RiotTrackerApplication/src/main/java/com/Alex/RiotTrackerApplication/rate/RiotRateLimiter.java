package com.Alex.RiotTrackerApplication.rate;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;




@Component
public class RiotRateLimiter {

    private static final int MAX_REQUESTS_PER_SECOND = 20;

    private final Semaphore semaphore = new Semaphore(MAX_REQUESTS_PER_SECOND, true);
    private final AtomicLong lastRefillTime = new AtomicLong(System.currentTimeMillis());

    public Mono<Void> acquirePermission() {
        return Mono.defer(() -> {
            refillTokens();
            if (semaphore.tryAcquire()) {
                return Mono.empty();
            } else {
                return Mono.delay(Duration.ofMillis(100))
                        .then(acquirePermission());
            }
        });
    }

    private synchronized void refillTokens() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();

        if (now - lastRefill >= 1000) {
            int availablePermits = semaphore.availablePermits();
            int permitsToAdd = MAX_REQUESTS_PER_SECOND - availablePermits;

            if (permitsToAdd > 0) {
                semaphore.release(permitsToAdd);
            }
            lastRefillTime.set(now);
        }
    }
}