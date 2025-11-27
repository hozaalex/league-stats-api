package com.Alex.RiotTrackerApplication.controller;


import com.Alex.RiotTrackerApplication.mappers.impl.SummonerMapper;

import com.Alex.RiotTrackerApplication.model.dto.*;
import com.Alex.RiotTrackerApplication.rate.UserRateLimiter;
import com.Alex.RiotTrackerApplication.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@RestController
@RequestMapping("api/v1/summoners")
@CrossOrigin(origins = "http://localhost:5173/")
@Tag(name = "Stats", description = "Endpoints for fetching League of Legends summoner data")
public class SummonerController {

    private static final Logger log = Logger.getLogger(SummonerController.class.getName());


    private final UserRateLimiter userRateLimiter;
    private final KafkaProducerService kafkaProducerService;
    private final SummonerService summonerService;
    private final RankedStatsService rankedStatsService;
    private final ParticipantService participantService;
    private final SummonerMapper summonerMapper;


    private final RequestStatusService requestStatusService;

    public SummonerController(
            UserRateLimiter userRateLimiter,
            KafkaProducerService kafkaProducerService,
            RequestStatusService requestStatusService
            ,SummonerService summonerService
            ,RankedStatsService rankedStatsService
            ,ParticipantService participantService
            ,SummonerMapper summonerMapper

    ) {

        this.userRateLimiter = userRateLimiter;
        this.kafkaProducerService = kafkaProducerService;
        this.requestStatusService = requestStatusService;
        this.summonerService = summonerService;
        this.rankedStatsService = rankedStatsService;
        this.participantService = participantService;
        this.summonerMapper = summonerMapper;
    }

    @PostMapping("/track")
    @Operation(
            summary = "Gets all of the available information for a given player",
            description = "Fetches summoner information from the Riot Games API by game name tag line and region"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Request accepted and queued for processing",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable",
                    content = @Content
            )
    })
    public Mono<ResponseEntity<FrontendResponseWrapperDto>> trackNewSummoner(
            @RequestBody RiotIdRequestDto requestDto,
            ServerWebExchange exchange) {

        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        log.info(String.format(
                "Received track request for summoner: %s#%s from IP: %s",
                requestDto.getGameName(),
                requestDto.getTagLine(),
                ip
        ));

        return userRateLimiter.checkAllowed(ip)
                .then(Mono.fromCallable(() ->
                        summonerService.findByGameNameAndTagLineAndRegion(
                                requestDto.getGameName(),
                                requestDto.getTagLine(),
                                requestDto.getRegion()
                        )
                ).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(summonerOpt -> {

                    if (summonerOpt.isPresent() && summonerService.isDataFresh(summonerOpt.get())) {
                        log.info(String.format(
                                "Returning cached data for: %s#%s",
                                requestDto.getGameName(),
                                requestDto.getTagLine()
                        ));

                        return Mono.fromCallable(() -> {
                                    String puuid = summonerOpt.get().getPuuid();

                                    SummonerDto summonerDto = summonerMapper.mapTo(summonerOpt.get());
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
                                            .requestId(null)
                                            .status("COMPLETED")
                                            .data(responseData)
                                            .error(null)
                                            .build();
                                }).subscribeOn(Schedulers.boundedElastic())
                                .map(ResponseEntity::ok);
                    }


                    log.info(String.format(
                            "Data stale or missing for %s#%s, queuing to Kafka",
                            requestDto.getGameName(),
                            requestDto.getTagLine()
                    ));

                    return Mono.fromCallable(() ->
                                    kafkaProducerService.sendSummonerRequest(
                                            requestDto.getGameName(),
                                            requestDto.getTagLine(),
                                            requestDto.getRegion(),
                                            ip
                                    )
                            )
                            .flatMap(requestId -> {
                                FrontendResponseWrapperDto initialStatus = FrontendResponseWrapperDto.builder()
                                        .requestId(requestId)
                                        .status("PROCESSING")
                                        .data(null)
                                        .error(null)
                                        .build();

                                return requestStatusService.saveStatus(requestId, initialStatus)
                                        .thenReturn(initialStatus);
                            })
                            .map(wrapper -> {
                                log.info("Queued summoner request [" + wrapper.getRequestId() + "]");
                                return ResponseEntity.status(HttpStatus.ACCEPTED).body(wrapper);
                            });
                })
                .onErrorResume(IllegalStateException.class, e -> {
                    log.warning("Rate limit exceeded for IP: " + ip);

                    FrontendResponseWrapperDto errorResponse = FrontendResponseWrapperDto.builder()
                            .requestId(null)
                            .status("RATE_LIMIT_EXCEEDED")
                            .data(null)
                            .error("Too many requests. Please try again later.")
                            .build();

                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse));
                })
                .onErrorResume(e -> {
                    log.severe("Error processing summoner request: " + e.getMessage());

                    FrontendResponseWrapperDto errorResponse = FrontendResponseWrapperDto.builder()
                            .requestId(null)
                            .status("SERVICE_UNAVAILABLE")
                            .data(null)
                            .error("Unable to process request at this time.")
                            .build();

                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
                });
    }

    @GetMapping("/status/{requestId}")
    @Operation(
            summary = "Check the status of a summoner request",
            description = "Returns the current processing status and data if available"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FrontendResponseWrapperDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Request ID not found",
                    content = @Content
            )
    })
    public Mono<ResponseEntity<FrontendResponseWrapperDto>> getSummonerStatus(@PathVariable String requestId) {
        log.info("Checking status for request: " + requestId);

        return requestStatusService.getStatus(requestId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }
}







    //Maybe they will be useful once I have multiple windows


//    @PostMapping("/track")
//    public Mono<ResponseEntity<SummonerDto>> trackNewSummoner(@RequestBody RiotIdRequestDto requestDto) {
//        log.info("Tracking summoner: " + requestDto.getGameName() + "#" + requestDto.getTagLine());
//
//        return riotApiService.fetchAndMapSummonerEntity(requestDto.getGameName(),requestDto.getTagLine(),requestDto.getRegion())
//                .flatMap(summonerDto -> {
//
//                    SummonerEntity entity = summonerMapper.mapFrom(summonerDto);
//
//
//                    return Mono.fromCallable(() -> summonerService.saveOrUpdateSummoner(entity))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .doOnSuccess(savedEntity -> {
//                                log.info("Saved summoner with PUUID: " + savedEntity.getPuuid());
//
//                                riotApiService.triggerInitialMatchFetch(savedEntity.getPuuid(),requestDto.getRegion());
//                                riotApiService.fetchRankedStats(savedEntity.getPuuid(),requestDto.getRegion());
//                            })
//                            .map(summonerMapper::mapTo);
//                })
//                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
//                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
//                .onErrorResume(e -> {
//                    log.severe("Error tracking summoner: " + e.getMessage());
//                    e.printStackTrace();
//                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
//
//                });
//    }
//    @GetMapping("/{puuid}/stats")
//    public Mono<ResponseEntity<?>> getSummonerStats(@PathVariable String puuid) {
//        log.info("Fetch summoner stats: " + puuid);
//
//        return Mono.fromCallable(() -> participantService.getPlayerStatsSummary(puuid))
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMap(summoner -> {
//                    if (summoner == null) {
//                        log.warning("No stats found for PUUID: " + puuid);
//                        return Mono.just(ResponseEntity.<SummonerStatsDto>status(HttpStatus.NOT_FOUND).build());
//                    }
//                    log.info("Retrieved stats for PUUID: {} - Games " + puuid + summoner.getTotalGames());
//                    return Mono.just(ResponseEntity.ok(summoner));
//                })
//                .onErrorResume(e -> {
//                    log.severe("Error fetching stats for " + puuid + ": " + e.getMessage());
//                    return Mono.just(ResponseEntity.<SummonerStatsDto>status(HttpStatus.INTERNAL_SERVER_ERROR).build());
//                });
//    }
//
//    @GetMapping("/{puuid}/champions")
//    public ResponseEntity<List<ChampionStatsDto>> getAllChampionStats(@PathVariable String puuid) {
//        log.info("Fetch champion stats: " + puuid);
//        List<ChampionStatsDto> stats = participantService.findChampionStatsByPlayer(puuid);
//        if(stats == null) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<>(stats, HttpStatus.OK);
//    }
//
//    @GetMapping("/{puuid}/champions/{championId}/stats")
//    public ResponseEntity<ChampionStatsDto> getChampionStats(@PathVariable String puuid, @PathVariable long championId) {
//        log.info("Fetch champion stats {} for champion {}: " + puuid+ championId);
//        ChampionStatsDto champion= participantService.getChampionStatsSummary(puuid,championId);
//        if(champion == null) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<>(champion, HttpStatus.OK);
//
//
//    }
//
//    @GetMapping("/{puuid}/matches")
//    public ResponseEntity<Page<MatchHistoryDto>> getMatches(@PathVariable String puuid, Pageable pageable) {
//        Page<MatchHistoryDto> page = participantService.getMatchHistory(puuid, pageable);
//        if(page == null) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//        return new ResponseEntity<>(page,HttpStatus.OK);
//    }

//    @GetMapping("/{puuid}/ranked/profile")
//    public Mono<ResponseEntity<?>> getRankedProfile(@RequestBody RiotIdRequestDto requestDto) {
//
//        return Mono.fromCallable(() -> summonerService.findById(puuid))
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMap(summonerOpt -> {
//                    if (summonerOpt.isEmpty()) {
//                        log.warning("Summoner not found in database: " + puuid);
//                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
//                    }
//
//
//                    return riotApiService.fetchRankedStats(puuid,region)
//                            .flatMap(rankedStatsList -> {
//                                if (rankedStatsList.isEmpty()) {
//                                    log.info("Player is unranked: " + puuid);
//                                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
//                                }
//
//
//                                return Mono.fromCallable(() -> rankedStatsService.getRankProfile(puuid))
//
//                                        .map(ResponseEntity::ok)
//                                        .subscribeOn(Schedulers.boundedElastic());
//                            });
//                })
//                .onErrorResume(EntityNotFoundException.class, e -> {
//                    log.warning("Ranked stats not found: " + e.getMessage());
//                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
//                })
//                .onErrorResume(e -> {
//                    log.warning("Error fetching ranked profile: " + e.getMessage());
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
//                });
//    }

