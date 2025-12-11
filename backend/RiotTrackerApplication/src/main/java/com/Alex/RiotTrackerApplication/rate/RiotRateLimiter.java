package com.Alex.RiotTrackerApplication.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;


@Component
public class RiotRateLimiter {

    //Using Bucket4j instead of semaphore to make it more scalable

    private static final Logger log = Logger.getLogger(RiotRateLimiter.class.getName());

    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(20, Duration.ofSeconds(1)))
            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(2)))
            .build();

    public Mono<Void> acquirePermission() {
        return Mono.fromCallable(() -> {
                    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                    if (probe.isConsumed()) {
                        return probe;
                    }


                    long waitNanos = probe.getNanosToWaitForRefill();
                    long waitMillis = waitNanos / 1_000_000;

                    if (waitMillis > 5000) {
                        log.warning("Rate limit requires long wait: " + waitMillis + "ms");
                    } else if (waitMillis > 100) {
                        log.info("Rate limit wait: " + waitMillis + "ms");
                    }


                    bucket.asBlocking().consume(1);
                    return probe;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
