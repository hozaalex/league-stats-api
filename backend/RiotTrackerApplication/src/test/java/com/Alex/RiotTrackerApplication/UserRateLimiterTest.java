package com.Alex.RiotTrackerApplication;

import com.Alex.RiotTrackerApplication.rate.UserRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserRateLimiterTest {

    private UserRateLimiter userRateLimiter;

    @BeforeEach
    void setUp() {
        userRateLimiter = new UserRateLimiter();
    }

    @Test
    void shouldAllowFirstRequest() {
        Mono<Void> result = userRateLimiter.checkAllowed("192.168.1.1");
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldAllow20RequestsWithinMinute() {
        String ip = "192.168.1.1";

        for (int i = 0; i < 20; i++) {
            StepVerifier.create(userRateLimiter.checkAllowed(ip))
                    .verifyComplete();
        }
    }

    @Test
    void shouldBlock21stRequestWithinMinute() {
        String ip = "192.168.1.1";

        for (int i = 0; i < 20; i++) {
            userRateLimiter.checkAllowed(ip).block();
        }

        Mono<Void> result = userRateLimiter.checkAllowed(ip);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                                throwable.getMessage().contains("Rate limit exceeded")
                )
                .verify();
    }

    @Test
    void shouldAllowDifferentIPs() {
        for (int i = 0; i < 20; i++) {
            userRateLimiter.checkAllowed("192.168.1.1").block();
        }

        Mono<Void> result = userRateLimiter.checkAllowed("192.168.1.2");
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldResetAfterOneMinute() throws InterruptedException {
        String ip = "192.168.1.1";

        for (int i = 0; i < 20; i++) {
            userRateLimiter.checkAllowed(ip).block();
        }

        Thread.sleep(61000);

        Mono<Void> result = userRateLimiter.checkAllowed(ip);
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleNullIP() {
        Mono<Void> result = userRateLimiter.checkAllowed(null);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                )
                .verify();
    }

    @Test
    void shouldHandleEmptyIP() {
        Mono<Void> result = userRateLimiter.checkAllowed("");
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                )
                .verify();
    }

    @Test
    void shouldHandleMultipleIPsIndependently() {
        String[] ips = {"192.168.1.1", "192.168.1.2", "192.168.1.3"};

        for (String ip : ips) {
            for (int i = 0; i < 20; i++) {
                StepVerifier.create(userRateLimiter.checkAllowed(ip))
                        .verifyComplete();
            }
        }

        for (String ip : ips) {
            StepVerifier.create(userRateLimiter.checkAllowed(ip))
                    .expectError(IllegalStateException.class)
                    .verify();
        }
    }

    @Test
    void shouldIncrementCountWithinSameWindow() {
        String ip = "192.168.1.1";

        for (int i = 0; i < 10; i++) {
            userRateLimiter.checkAllowed(ip).block();
        }

        for (int i = 0; i < 10; i++) {
            userRateLimiter.checkAllowed(ip).block();
        }

        StepVerifier.create(userRateLimiter.checkAllowed(ip))
                .expectError(IllegalStateException.class)
                .verify();
    }
}