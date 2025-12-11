package com.Alex.RiotTrackerApplication.service;


import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.RankedStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.RiotIdRequestDto;
import com.Alex.RiotTrackerApplication.model.dto.SummonerDto;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


public interface RiotApiService {

    Mono<SummonerDto> fetchAndMapSummonerEntity(String gameName, String tagLine, String region);
    Mono<List<String>> fetchMatchIds(String puuid,String region);
    Mono<?> fetchAndSaveMatchDetails(String matchId,String region);
    Mono<Void> triggerInitialMatchFetch(String puuid,String region);
    Mono<Boolean> fetchAndSaveSummoner(String puuid,String region);
    Mono<List<RankedStatsDto>> fetchRankedStats(String puuid,String region);

}

