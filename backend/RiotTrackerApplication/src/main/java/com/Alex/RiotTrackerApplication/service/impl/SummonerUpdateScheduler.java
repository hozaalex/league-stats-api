package com.Alex.RiotTrackerApplication.service.impl;


import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.service.RankedStatsService;
import com.Alex.RiotTrackerApplication.service.RiotApiService;
import com.Alex.RiotTrackerApplication.service.SummonerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.Map;
import java.util.logging.Logger;


@Service
@RequiredArgsConstructor
public class SummonerUpdateScheduler {

    private final SummonerService summonerService;
    private final RiotApiService riotApiService;
    private static final Logger log = Logger.getLogger(SummonerUpdateScheduler.class.getName());


    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void updateSummoners(){
        log.info("=== Starting summoner update task ===");

        Map<String,String> puuidsAndRegionsMap = summonerService.getAllPuuidsAndRegions();
        log.info("Found " + puuidsAndRegionsMap.size() + " summoners to update");

        Flux.fromIterable(puuidsAndRegionsMap.entrySet())
                .concatMap(entry -> {
                    String puuid = entry.getKey();
                    String region = entry.getValue();

                    return riotApiService.fetchAndSaveSummoner(puuid, region)
                            .doOnNext(updated -> {
                                if (updated) {
                                    log.info("Updated summoner " + puuid);

                                    riotApiService.fetchRankedStats(puuid, region)
                                            .doOnError(e -> log.warning("Failed to store ranked stats for " + puuid + ": " + e.getMessage()))
                                            .subscribe();
                                } else {
                                    log.info("No changes for summoner " + puuid);
                                }
                            })
                            .onErrorResume(e -> {
                                log.warning("Error updating summoner: " + puuid + " - " + e.getMessage());
                                return Mono.just(false);
                            });
                })
                .collectList()
                .doOnSuccess(results -> log.info("Summoner update task complete. Total processed: " + results.size()))
                .subscribe();
    }


}
