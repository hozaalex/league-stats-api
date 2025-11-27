package com.Alex.RiotTrackerApplication.service.impl;


import com.Alex.RiotTrackerApplication.service.KafkaProducerService;
import com.Alex.RiotTrackerApplication.service.RiotApiService;
import com.Alex.RiotTrackerApplication.service.SummonerService;
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
    private final KafkaProducerService kafkaProducerService;
    private static final Logger log = Logger.getLogger(SummonerUpdateScheduler.class.getName());


    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void updateSummoners() {
        log.info("=== Starting scheduled summoner refresh ===");

        Map<String, String> puuidsAndRegionsMap = summonerService.getAllPuuidsAndRegions();
        log.info("Queueing " + puuidsAndRegionsMap.size() + " summoners for refresh");

        puuidsAndRegionsMap.forEach((puuid, region) -> {
            summonerService.getSummonerByPuuid(puuid).ifPresent(summoner -> {
                kafkaProducerService.sendSummonerRequest(
                        summoner.getGameName(),
                        summoner.getTagLine(),
                        region,
                        "SCHEDULER"
                );
            });
        });

        log.info("=== Scheduled refresh queued to Kafka ===");
    }


}
