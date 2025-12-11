package com.Alex.RiotTrackerApplication.service;

import reactor.core.publisher.Mono;

public interface SummonerProcessingService {

    Mono<Boolean> processRequest(String requestId, String gameName, String tagLine, String region);
}
