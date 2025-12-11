package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.model.dto.FrontendResponseWrapperDto;
import com.Alex.RiotTrackerApplication.service.RequestStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RequestStatusServiceImpl implements RequestStatusService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "summoner:request:";
    private static final Duration TTL = Duration.ofHours(1);

    public RequestStatusServiceImpl(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> saveStatus(String requestId, FrontendResponseWrapperDto status) {
        try {
            String json = objectMapper.writeValueAsString(status);
            return redisTemplate.opsForValue()
                    .set(KEY_PREFIX + requestId, json, TTL)
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<FrontendResponseWrapperDto> getStatus(String requestId) {
        return redisTemplate.opsForValue()
                .get(KEY_PREFIX + requestId)
                .flatMap(json -> {
                    try {
                        FrontendResponseWrapperDto dto = objectMapper.readValue(
                                json,
                                FrontendResponseWrapperDto.class
                        );
                        return Mono.just(dto);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }
}
