//package com.Alex.RiotTrackerApplication.service.impl;
//
//import com.Alex.RiotTrackerApplication.service.SummonerProcessingService;
//import com.Alex.RiotTrackerApplication.service.SummonerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.UUID;
//import java.util.logging.Logger;
//
//@Service
//@RequiredArgsConstructor
//public class SummonerUpdateScheduler {
//
//    private final SummonerService summonerService;
//    private final SummonerProcessingService processingService;
//
//    private static final Logger log = Logger.getLogger(SummonerUpdateScheduler.class.getName());
//    //Batches will help reduce the probability of hitting the api limit
//    private static final int BATCH_SIZE = 5;
//    private static final int DELAY_BETWEEN_SUMMONERS_SECONDS = 15;
//
//    @Scheduled(fixedRate = 10 * 60 * 1000)
//    public void updateSummonersBatch() {
//        log.info("=== Starting scheduled summoner batch refresh ===");
//
//        Map<String, String> puuidsAndRegionsMap = summonerService.getAllPuuidsAndRegions();
//        log.info("Total summoners in database: " + puuidsAndRegionsMap.size());
//
//
//        Flux.fromIterable(puuidsAndRegionsMap.entrySet())
//                .flatMap(entry -> {
//                    String puuid = entry.getKey();
//                    String region = entry.getValue();
//
//                    return Mono.fromCallable(() -> summonerService.getSummonerByPuuid(puuid))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMap(opt -> opt.map(summoner ->
//                                    Mono.just(Map.entry(summoner, region))
//                            ).orElseGet(Mono::empty))
//                            .filter(pair -> !summonerService.isDataFresh(pair.getKey()));
//                })
//                .take(BATCH_SIZE)
//                .delayElements(Duration.ofSeconds(DELAY_BETWEEN_SUMMONERS_SECONDS))
//                .flatMap(pair -> {
//                    var summoner = pair.getKey();
//                    var region = pair.getValue();
//
//                    String requestId = "SCHEDULER-" + UUID.randomUUID();
//                    log.info(String.format("Refreshing [%d/%d]: %s#%s",
//                            puuidsAndRegionsMap.size(), BATCH_SIZE,
//                            summoner.getGameName(), summoner.getTagLine()));
//
//                    return processingService.processRequest(
//                                    requestId,
//                                    summoner.getGameName(),
//                                    summoner.getTagLine(),
//                                    region
//                            )
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .timeout(Duration.ofMinutes(2))
//                            .doOnSuccess(success ->
//                                    log.info("Successfully refreshed: " + summoner.getGameName()))
//                            .doOnError(error ->
//                                    log.warning("Failed to refresh " + summoner.getGameName() + ": " + error.getMessage()))
//                            .onErrorResume(e -> Mono.empty());
//                }, 1)
//                .doOnComplete(() -> log.info("=== Batch refresh completed ==="))
//                .doOnError(error -> log.severe("Critical error in batch refresh: " + error.getMessage()))
//                .subscribe();
//    }
//
//}
