package com.Alex.RiotTrackerApplication;


import com.Alex.RiotTrackerApplication.rate.RiotRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RiotRateLimiterTest {

    private RiotRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {

        rateLimiter = new RiotRateLimiter();
    }

    @Test
    void shouldAllowRequestsWithinLimit() {

        for (int i = 0; i < 20; i++) {
            Mono<Void> result = rateLimiter.acquirePermission();


            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Test
    void shouldBlockOrDelay_whenLimitExceeded() {

        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }


        long startTime = System.currentTimeMillis();
        rateLimiter.acquirePermission().block();
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;


        assertTrue(elapsed >= 900, // Allow some margin
                "Request should have been delayed, but took only " + elapsed + "ms");
    }

    @Test
    void shouldRefillPermitsAfterInterval() throws InterruptedException {

        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }


        Thread.sleep(1100);


        StepVerifier.create(rateLimiter.acquirePermission())
                .verifyComplete();
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {

        int threadCount = 30;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger delayedCount = new AtomicInteger(0);


        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    long start = System.currentTimeMillis();
                    rateLimiter.acquirePermission().block();
                    long elapsed = System.currentTimeMillis() - start;

                    if (elapsed < 100) {
                        successCount.incrementAndGet();
                    } else {
                        delayedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);


        assertTrue(successCount.get() <= 20,
                "Expected at most 20 immediate successes, got " + successCount.get());
        assertTrue(delayedCount.get() >= 10,
                "Expected at least 10 delayed requests, got " + delayedCount.get());
    }

    @Test
    void shouldNotBlockIndefinitely() {

        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }


        Mono<Void> result = rateLimiter.acquirePermission()
                .timeout(Duration.ofSeconds(2));


        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void shouldHandleRapidSuccessiveRequests() {

        List<Long> delays = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            long start = System.currentTimeMillis();
            rateLimiter.acquirePermission().block();
            long elapsed = System.currentTimeMillis() - start;
            delays.add(elapsed);
        }


        long fastRequests = delays.stream().filter(d -> d < 100).count();
        long slowRequests = delays.stream().filter(d -> d >= 100).count();

        assertFalse(fastRequests <= 20, "Too many fast requests: " + fastRequests);
        assertFalse(slowRequests >= 5, "Expected delayed requests: " + slowRequests);
    }
}
