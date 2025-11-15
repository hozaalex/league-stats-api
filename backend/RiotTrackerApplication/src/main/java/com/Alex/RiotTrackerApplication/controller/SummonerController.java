package com.Alex.RiotTrackerApplication.controller;

import com.Alex.RiotTrackerApplication.mappers.Mapper;
import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.*;
import com.Alex.RiotTrackerApplication.rate.UserRateLimiter;
import com.Alex.RiotTrackerApplication.service.ParticipantService;
import com.Alex.RiotTrackerApplication.service.RankedStatsService;
import com.Alex.RiotTrackerApplication.service.RiotApiService;
import com.Alex.RiotTrackerApplication.service.SummonerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;


//TODO:Add builds for the Champions



@RestController
@RequestMapping("api/v1/summoners")
@CrossOrigin(origins = "http://localhost:5173/")
@Tag(name = "Stats", description = "Endpoints for fetching League of Legends summoner data")
public class SummonerController {

    private static final Logger log = Logger.getLogger(SummonerController.class.getName());

    private final SummonerService summonerService;
    private final Mapper<SummonerEntity, SummonerDto> summonerMapper;
    private final ParticipantService participantService;
    private final RiotApiService riotApiService;
    private final RankedStatsService rankedStatsService;
    private final UserRateLimiter userRateLimiter;

    public SummonerController(SummonerService summonerService,
                              Mapper<SummonerEntity, SummonerDto> summonerMapper,
                              ParticipantService participantService,
                              RiotApiService riotApiService,RankedStatsService rankedStatsService,UserRateLimiter userRateLimiter) {
        this.summonerService = summonerService;
        this.summonerMapper = summonerMapper;
        this.participantService = participantService;
        this.riotApiService = riotApiService;
        this.rankedStatsService = rankedStatsService;
        this.userRateLimiter = userRateLimiter;
    }



    @PostMapping("/track")
    @Operation(
            summary = "Gets all of the available information for a given player",
            description = "Fetches summoner information from the Riot Games API by game name tag line and region"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Summoner Found Succesfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FrontEndResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Summoner not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Internal Server Error, service unavailable",
                    content = @Content

            )


    })
    public Mono<ResponseEntity<FrontEndResponseDto>> trackNewSummoner(
            @RequestBody RiotIdRequestDto requestDto,
            ServerHttpRequest request) {

        //should be changed if I ever use proxies/load balancers
        String ip = request.getRemoteAddress().getAddress().getHostAddress();

        log.info("Tracking summoner: " + requestDto.getGameName() + "#" + requestDto.getTagLine());


        return userRateLimiter.checkAllowed(ip)
                .then(
                        riotApiService.fetchAndMapSummonerEntity(
                                requestDto.getGameName(),
                                requestDto.getTagLine(),
                                requestDto.getRegion())
                )
                .flatMap(summonerDto -> {
                    SummonerEntity entity = summonerMapper.mapFrom(summonerDto);

                    return Mono.fromCallable(() -> summonerService.saveOrUpdateSummoner(entity))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(savedEntity -> {
                                log.info("Saved summoner with PUUID: " + savedEntity.getPuuid());
                                String puuid = savedEntity.getPuuid();

                                riotApiService.triggerInitialMatchFetch(puuid, requestDto.getRegion());
                                Mono<List<RankedStatsDto>> rankedFetch =
                                        riotApiService.fetchRankedStats(puuid, requestDto.getRegion());

                                return Mono.when(rankedFetch)
                                        .then(Mono.fromCallable(() -> {
                                            SummonerDto summoner = summonerMapper.mapTo(savedEntity);
                                            SummonerStatsDto overallStats = participantService.getPlayerStatsSummary(puuid);
                                            RankProfileDto rankedProfile = rankedStatsService.getRankProfile(puuid);
                                            List<ChampionStatsDto> championStats = participantService.findChampionStatsByPlayer(puuid);
                                            List<MatchHistoryDto> recentMatches = participantService.getMatchHistory(puuid, PageRequest.of(0, 10))
                                                    .getContent();

                                            FrontEndResponseDto response = FrontEndResponseDto.builder()
                                                    .summoner(summoner)
                                                    .overallStats(overallStats)
                                                    .rankedProfile(rankedProfile)
                                                    .overallChampionStats(championStats)
                                                    .recentMatches(recentMatches)
                                                    .build();

                                            log.info("Response built: " + response);
                                            return response;
                                        }).subscribeOn(Schedulers.boundedElastic()));
                            });
                })
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                .onErrorResume(IllegalStateException.class, e -> {

                    log.warning("Rate limit exceeded for IP: " + ip);
                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
                })
                .onErrorResume(e -> {
                    log.severe("Error tracking summoner: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                });
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

}