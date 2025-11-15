package com.Alex.RiotTrackerApplication.rate;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserRateLimiter {

    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<String, Long>();
    private static final long COOLDOWN_TIME = 10000;

    public Mono<Void> checkAllowed(String ip){

        if(ip == null || ip.isEmpty()){
            return Mono.error(new NullPointerException("Ip is null or empty"));
        }
        long now = System.currentTimeMillis();
        Long last = lastRequestTimes.get(ip);

        if (last != null) {
            long elapsed = now - last;
            if (elapsed < COOLDOWN_TIME) {
                long remaining = (COOLDOWN_TIME - elapsed) / 1000;
                return Mono.error(new IllegalStateException("Try again in " + remaining + " seconds"));
            }
        }



        lastRequestTimes.put(ip, now);
        return Mono.empty();
    }
}
