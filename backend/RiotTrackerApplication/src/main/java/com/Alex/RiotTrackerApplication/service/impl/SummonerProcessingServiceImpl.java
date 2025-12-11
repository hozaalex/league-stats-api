package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.mappers.impl.SummonerMapper;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.*;
import com.Alex.RiotTrackerApplication.rate.RiotRateLimiter;
import com.Alex.RiotTrackerApplication.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.logging.Logger;

@Service
public class SummonerProcessingServiceImpl implements SummonerProcessingService {

    private static final Logger log = Logger.getLogger(SummonerProcessingServiceImpl.class.getName());

    private final RiotApiService riotApiService;
    private final SummonerService summonerService;
    private final SummonerMapper summonerMapper;
    private final RequestStatusService requestStatusService;
    private final ParticipantService participantService;
    private final RankedStatsService rankedStatsService;
    private final RiotRateLimiter riotRateLimiter;

    public SummonerProcessingServiceImpl(
            RiotApiService riotApiService,
            SummonerService summonerService,
            SummonerMapper summonerMapper,
            RequestStatusService requestStatusService,
            ParticipantService participantService,
            RankedStatsService rankedStatsService,RiotRateLimiter riotRateLimiter) {
        this.riotApiService = riotApiService;
        this.summonerService = summonerService;
        this.summonerMapper = summonerMapper;
        this.requestStatusService = requestStatusService;
        this.participantService = participantService;
        this.rankedStatsService = rankedStatsService;
        this.riotRateLimiter = riotRateLimiter;
    }

    @Override
    public Mono<Boolean> processRequest(String requestId, String gameName, String tagLine, String region) {
        log.info(String.format("Processing summoner request [%s] for %s#%s", requestId, gameName, tagLine));

        return riotRateLimiter.acquirePermission()
                .then(riotApiService.fetchAndMapSummonerEntity(gameName, tagLine, region))
                .flatMap(summonerDto -> {
                    if (summonerDto == null) {
                        log.warning(String.format("No summoner found for %s#%s", gameName, tagLine));
                        return updateStatusFailed(requestId, "Summoner not found").thenReturn(false);
                    }

                    return Mono.fromCallable(() -> summonerService.saveOrUpdateSummoner(summonerMapper.mapFrom(summonerDto)))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(savedEntity -> {
                                log.info("Successfully saved summoner - PUUID: " + savedEntity.getPuuid());
                                String puuid = savedEntity.getPuuid();

                                return riotRateLimiter.acquirePermission()
                                        .then(riotApiService.fetchRankedStats(puuid, region))
                                        .doOnSuccess(stats -> log.info("Fetched ranked stats for " + puuid))
                                        .flatMap(rs -> riotRateLimiter.acquirePermission()
                                                .then(riotApiService.triggerInitialMatchFetch(puuid, region)))
                                        .then(updateStatusCompleted(requestId, puuid))
                                        .thenReturn(true);
                            });
                })
                .onErrorResume(e -> {
                    log.severe("Error processing summoner request [" + requestId + "]: " + e.getMessage());
                    e.printStackTrace();
                    return updateStatusFailed(requestId, e.getMessage()).thenReturn(false);
                });
    }


    private Mono<Void> updateStatusCompleted(String requestId, String puuid) {
        return Mono.fromCallable(() -> {
                    SummonerDto summonerDto = summonerService.findById(puuid).map(summonerMapper::mapTo).orElse(null);
                    SummonerStatsDto overallStats = participantService.getPlayerStatsSummary(puuid);
                    RankProfileDto rankedProfile = rankedStatsService.getRankProfile(puuid);
                    List<ChampionStatsDto> championStats = participantService.findChampionStatsByPlayer(puuid);
                    Pageable pageable = PageRequest.of(0, 10);
                    Page<MatchHistoryDto> matchPage = participantService.getMatchHistory(puuid, pageable);
                    List<MatchHistoryDto> recentMatches = matchPage.getContent();

                    FrontEndResponseDto responseData = FrontEndResponseDto.builder()
                            .summoner(summonerDto)
                            .overallStats(overallStats)
                            .rankedProfile(rankedProfile)
                            .overallChampionStats(championStats)
                            .recentMatches(recentMatches)
                            .build();

                    return FrontendResponseWrapperDto.builder()
                            .requestId(requestId)
                            .status("COMPLETED")
                            .data(responseData)
                            .error(null)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(completedStatus ->
                        requestStatusService.saveStatus(requestId, completedStatus)
                                .doOnSuccess(unused -> log.info("Status updated to COMPLETED for request: " + requestId))
                                .doOnError(error -> log.severe("Failed to update status to COMPLETED: " + error.getMessage()))
                )
                .onErrorResume(e -> {
                    log.severe("Error building response data: " + e.getMessage());
                    e.printStackTrace();
                    return updateStatusFailed(requestId, "Failed to build response: " + e.getMessage());
                });
    }

    private Mono<Void> updateStatusFailed(String requestId, String errorMessage) {
        FrontendResponseWrapperDto failedStatus = FrontendResponseWrapperDto.builder()
                .requestId(requestId)
                .status("FAILED")
                .data(null)
                .error(errorMessage)
                .build();

        return requestStatusService.saveStatus(requestId, failedStatus)
                .doOnSuccess(unused -> log.info("Status updated to FAILED for request: " + requestId))
                .doOnError(error -> log.severe("Failed to update status to FAILED: " + error.getMessage()))
                .then();
    }
}
