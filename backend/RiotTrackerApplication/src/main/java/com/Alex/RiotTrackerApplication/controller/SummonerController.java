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
import java.util.UUID;
import java.util.logging.Logger;


@RestController
@RequestMapping("api/v1/summoners")
@CrossOrigin(origins = "http://localhost:5173/")
@Tag(name = "Stats", description = "Endpoints for fetching League of Legends summoner data")
public class SummonerController {

    private static final Logger log = Logger.getLogger(SummonerController.class.getName());

    private final UserRateLimiter userRateLimiter;
    private final SummonerService summonerService;
    private final RankedStatsService rankedStatsService;
    private final ParticipantService participantService;
    private final SummonerMapper summonerMapper;
    private final RequestStatusService requestStatusService;
    private final SummonerProcessingService processingService;

    public SummonerController(
            UserRateLimiter userRateLimiter,
            RequestStatusService requestStatusService,
            SummonerService summonerService,
            RankedStatsService rankedStatsService,
            ParticipantService participantService,
            SummonerMapper summonerMapper,
            SummonerProcessingService processingService
    ) {
        this.userRateLimiter = userRateLimiter;
        this.requestStatusService = requestStatusService;
        this.summonerService = summonerService;
        this.rankedStatsService = rankedStatsService;
        this.participantService = participantService;
        this.summonerMapper = summonerMapper;
        this.processingService = processingService;
    }

    @PostMapping("/track")
    @Operation(
            summary = "Gets all of the available information for a given player",
            description = "Fetches summoner information from the Riot Games API by game name tag line and region"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Data retrieved from cache",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FrontendResponseWrapperDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Request accepted and processing in background",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FrontendResponseWrapperDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
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

                        return buildCompletedResponse(summonerOpt.get().getPuuid())
                                .map(ResponseEntity::ok);
                    }


                    log.info(String.format(
                            "Data stale or missing for %s#%s, processing in background",
                            requestDto.getGameName(),
                            requestDto.getTagLine()
                    ));

                    String requestId = UUID.randomUUID().toString();


                    FrontendResponseWrapperDto initialStatus = FrontendResponseWrapperDto.builder()
                            .requestId(requestId)
                            .status("PROCESSING")
                            .data(null)
                            .error(null)
                            .build();

                    return requestStatusService.saveStatus(requestId, initialStatus)
                            .then(Mono.defer(() -> {

                                processingService.processRequest(
                                        requestId,
                                        requestDto.getGameName(),
                                        requestDto.getTagLine(),
                                        requestDto.getRegion()
                                ).subscribeOn(Schedulers.boundedElastic()).subscribe();

                                return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body(initialStatus));
                            }));
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

    private Mono<FrontendResponseWrapperDto> buildCompletedResponse(String puuid) {
        return Mono.fromCallable(() -> {
            SummonerDto summonerDto = summonerService.findById(puuid)
                    .map(summonerMapper::mapTo)
                    .orElseThrow(() -> new RuntimeException("Summoner not found"));

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
        }).subscribeOn(Schedulers.boundedElastic());
    }
}