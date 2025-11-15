package com.Alex.RiotTrackerApplication;


import com.Alex.RiotTrackerApplication.rate.UserRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Fail.fail;

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
    void shouldBlockSecondRequestWithinCooldown() {
        userRateLimiter.checkAllowed("192.168.1.1").block();
        Mono<Void> result = userRateLimiter.checkAllowed("192.168.1.1");
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                                throwable.getMessage().contains("Try again in") &&
                                throwable.getMessage().contains("seconds")
                )
                .verify();
    }

    @Test
    void shouldAllowDifferentIPs() {

        userRateLimiter.checkAllowed("192.168.1.1").block();
        Mono<Void> result = userRateLimiter.checkAllowed("192.168.1.2");
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldAllowSameIPAfterCooldown() throws InterruptedException {

        userRateLimiter.checkAllowed("192.168.1.1").block();
        Thread.sleep(10100);
        Mono<Void> result = userRateLimiter.checkAllowed("192.168.1.1");
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldShowCorrectRemainingTime() {

        String ip = "192.168.1.1";
        userRateLimiter.checkAllowed(ip).block();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        Mono<Void> result = userRateLimiter.checkAllowed(ip);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("7") ||
                                throwable.getMessage().contains("8")
                )
                .verify();
    }

    @Test
    void shouldHandleNullIP() {

        Mono<Void> result = userRateLimiter.checkAllowed(null);
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void shouldHandleMultipleIPsConcurrently() throws InterruptedException {

        String[] ips = {"192.168.1.1", "192.168.1.2", "192.168.1.3"};
        for (String ip : ips) {
            StepVerifier.create(userRateLimiter.checkAllowed(ip))
                    .verifyComplete();
        }

        StepVerifier.create(userRateLimiter.checkAllowed(ips[0]))
                .expectError(IllegalStateException.class)
                .verify();

        StepVerifier.create(userRateLimiter.checkAllowed(ips[1]))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
