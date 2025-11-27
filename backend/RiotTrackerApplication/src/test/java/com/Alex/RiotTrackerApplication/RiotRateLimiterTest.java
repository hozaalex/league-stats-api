package com.Alex.RiotTrackerApplication;

import com.Alex.RiotTrackerApplication.rate.RiotRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RiotRateLimiterTest {

    private RiotRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RiotRateLimiter();
    }

    @Test
    void shouldAllow20RequestsImmediately() {
        for (int i = 0; i < 20; i++) {
            StepVerifier.create(rateLimiter.acquirePermission())
                    .expectComplete()
                    .verify(Duration.ofMillis(500));
        }
    }



    @Test
    void shouldRefillTokensAfterOneSecond() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }

        Thread.sleep(1100);

        StepVerifier.create(rateLimiter.acquirePermission())
                .expectComplete()
                .verify(Duration.ofMillis(500));
    }

    @Test
    void shouldNotBlockIndefinitely() {
        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }

        Mono<Void> result = rateLimiter.acquirePermission();

        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void shouldHandleRapidBurstThenRefill() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }

        Thread.sleep(1100);

        for (int i = 0; i < 20; i++) {
            StepVerifier.create(rateLimiter.acquirePermission())
                    .expectComplete()
                    .verify(Duration.ofMillis(500));
        }
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        int threadCount = 25;
        CountDownLatch latch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    rateLimiter.acquirePermission().block(Duration.ofSeconds(3));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All requests should complete within timeout");
    }

    @Test
    void shouldAllowSteadyRateOverTime() throws InterruptedException {
        for (int second = 0; second < 3; second++) {
            for (int i = 0; i < 15; i++) {
                StepVerifier.create(rateLimiter.acquirePermission())
                        .expectComplete()
                        .verify(Duration.ofSeconds(1));
            }
            Thread.sleep(1100);
        }
    }


    @Test
    void shouldAllowBurstUpToCapacity() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission().block();
        }

        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(elapsed < 500,
                "20 requests should complete quickly (burst), but took " + elapsed + "ms");
    }
}