package com.Alex.RiotTrackerApplication.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;




@Component
public class RiotRateLimiter {

    //Using Bucket4j instead of semaphore to make it more scalable

    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(20, Duration.ofSeconds(1)))
            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(2)))
            .build();

    public Mono<Void> acquirePermission() {
        return Mono.fromCallable(() -> {
                    bucket.asBlocking().consume(1);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
