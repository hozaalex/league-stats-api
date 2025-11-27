package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.mappers.impl.SummonerMapper;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.*;
import com.Alex.RiotTrackerApplication.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.logging.Logger;

@Service
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

    private static final Logger log = Logger.getLogger(KafkaConsumerServiceImpl.class.getName());

    private final RiotApiService riotApiService;
    private final SummonerService summonerService;
    private final SummonerMapper summonerMapper;
    private final KafkaProducerServiceImpl kafkaProducerService;
    private final RequestStatusService requestStatusService;
    private final ParticipantService participantService;
    private final RankedStatsService rankedStatsService;

    private static final int MAX_RETRIES = 3;
    private static final String SUMMONER_DLQ = "summoner-requests-dlq";

    public KafkaConsumerServiceImpl(
            RiotApiService riotApiService,
            SummonerService summonerService,
            SummonerMapper summonerMapper,
            KafkaProducerServiceImpl kafkaProducerService,
            RequestStatusService requestStatusService,
            ParticipantService participantService,
            RankedStatsService rankedStatsService) {

        this.riotApiService = riotApiService;
        this.summonerService = summonerService;
        this.summonerMapper = summonerMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.requestStatusService = requestStatusService;
        this.participantService = participantService;
        this.rankedStatsService = rankedStatsService;
    }

    @KafkaListener(
            topics = "${riot.kafka.topics.summoner-requests}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSummonerRequest(
            @Payload SummonerKafkaRequest request,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info(String.format(
                "Consuming summoner request [%s] for %s#%s from partition %d offset %d",
                request.getRequestId(),
                request.getGameName(),
                request.getTagLine(),
                partition,
                offset
        ));

        try {

            Mono<SummonerDto> summonerMono = riotApiService.fetchAndMapSummonerEntity(
                    request.getGameName(),
                    request.getTagLine(),
                    request.getRegion()
            );
            SummonerDto summonerDto = summonerMono.block();

            if (summonerDto == null) {
                log.warning(String.format(
                        "No summoner found for %s#%s",
                        request.getGameName(),
                        request.getTagLine()
                ));

                updateStatusFailed(request.getRequestId(), "Summoner not found");
                return;
            }


            SummonerEntity entity = summonerMapper.mapFrom(summonerDto);
            SummonerEntity savedEntity = summonerService.saveOrUpdateSummoner(entity);

            log.info(String.format(
                    "Successfully saved summoner - PUUID: %s",
                    savedEntity.getPuuid()
            ));

            String puuid = savedEntity.getPuuid();


            Mono<List<RankedStatsDto>> rankedStatsMono =
                    riotApiService.fetchRankedStats(puuid, request.getRegion());
            List<RankedStatsDto> rankedStats = rankedStatsMono.block();

            if (rankedStats != null && !rankedStats.isEmpty()) {
                log.info(String.format("Fetched ranked stats: %d queues", rankedStats.size()));
            } else {
                log.info("Player is unranked");
            }


            riotApiService.triggerInitialMatchFetch(puuid, request.getRegion())
                    .block();

            log.info(String.format(
                    "Successfully completed ALL processing for request [%s]",
                    request.getRequestId()
            ));


            updateStatusCompleted(request.getRequestId(), puuid);

        } catch (Exception e) {
            log.severe(String.format(
                    "Error processing summoner request [%s]: %s",
                    request.getRequestId(),
                    e.getMessage()
            ));

            updateStatusFailed(request.getRequestId(), e.getMessage());

            kafkaProducerService.sendToDeadLetterQueue(
                    "summoner-requests-dlq",
                    request,
                    e.getMessage()
            );
        }
    }

    private void updateStatusCompleted(String requestId, String puuid) {
        try {

            SummonerDto summonerDto = summonerService.findById(puuid)
                    .map(summonerMapper::mapTo)
                    .orElse(null);


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

            FrontendResponseWrapperDto completedStatus = FrontendResponseWrapperDto.builder()
                    .requestId(requestId)
                    .status("COMPLETED")
                    .data(responseData)
                    .error(null)
                    .build();

            requestStatusService.saveStatus(requestId, completedStatus)
                    .subscribe(
                            unused -> log.info("Status updated to COMPLETED for request: " + requestId),
                            error -> log.severe("Failed to update status to COMPLETED: " + error.getMessage())
                    );

        } catch (Exception e) {
            log.severe("Error building response data: " + e.getMessage());
            e.printStackTrace();
            updateStatusFailed(requestId, "Failed to build response: " + e.getMessage());
        }
    }

    private void updateStatusFailed(String requestId, String errorMessage) {
        FrontendResponseWrapperDto failedStatus = FrontendResponseWrapperDto.builder()
                .requestId(requestId)
                .status("FAILED")
                .data(null)
                .error(errorMessage)
                .build();

        requestStatusService.saveStatus(requestId, failedStatus)
                .subscribe(
                        unused -> log.info("Status updated to FAILED for request: " + requestId),
                        error -> log.severe("Failed to update status to FAILED: " + error.getMessage())
                );
    }
}