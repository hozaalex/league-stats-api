package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.mappers.impl.MatchMapper;
import com.Alex.RiotTrackerApplication.mappers.impl.RankedStatsMapper;
import com.Alex.RiotTrackerApplication.model.MatchEntity;
import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.*;
import com.Alex.RiotTrackerApplication.rate.RiotRateLimiter;
import com.Alex.RiotTrackerApplication.repository.MatchRepository;
import com.Alex.RiotTrackerApplication.repository.RankedStatsRepository;
import com.Alex.RiotTrackerApplication.repository.SummonerRepository;
import com.Alex.RiotTrackerApplication.service.RiotApiService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RiotApiServiceImpl implements RiotApiService {

    private static final Logger log = Logger.getLogger(RiotApiServiceImpl.class.getName());
    private final WebClient webClient;
    private final RiotRateLimiter rateLimiter;
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final SummonerRepository summonerRepository;
    private final RankedStatsRepository rankedStatsRepository;
    private final RankedStatsMapper rankedStatsMapper;


    @Value("${riot.api.key}")
    private String apiKey;

    public RiotApiServiceImpl(WebClient.Builder webClientBuilder,
                              RiotRateLimiter rateLimiter,
                              MatchRepository matchRepository,
                              MatchMapper matchMapper, SummonerRepository summonerRepository,RankedStatsRepository rankedStatesRepository,
                              RankedStatsMapper rankedStatsMapper) {
        this.webClient = webClientBuilder.build();
        this.rateLimiter = rateLimiter;
        this.matchRepository = matchRepository;
        this.matchMapper = matchMapper;
        this.summonerRepository = summonerRepository;
        this.rankedStatsRepository = rankedStatesRepository;
        this.rankedStatsMapper = rankedStatsMapper;


    }


    public static String getPlatformRouting(String region) {
        if (region == null) return "euw1";

        switch (region.toUpperCase()) {
            case "EUW":
                return "euw1";
            case "EUNE":
                return "eun1";
            case "NA":
                return "na1";
            case "KR":
                return "kr";
            case "JP":
                return "jp1";
            case "BR":
                return "br1";
            case "TR":
                return "tr1";
            case "RU":
                return "ru";
            case "OCE":
                return "oc1";
            case "LAN":
                return "la1";
            case "LAS":
                return "la2";
            default:
                return "euw1";
        }
    }

    public static String getRegionalRouting(String region) {
        if (region == null) return "europe";

        switch (region.toUpperCase()) {
            case "EUW":
            case "EUNE":
            case "TR":
            case "RU":
                return "europe";

            case "NA":
            case "LAN":
            case "LAS":
            case "BR":
                return "americas";

            case "OCE":
                return "sea";

            case "KR":
            case "JP":
                return "asia";

            default:
                return "europe";
        }
    }

    public Mono<Boolean> fetchAndSaveSummoner(String puuid,String region) {

        String platformRouting = getPlatformRouting(region);
        log.info("platform routing: " + platformRouting);
        String summonerUrl = String.format(
                "https://%s.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s",
                platformRouting,puuid
        );
        log.info(">>> fetchAndSaveSummoner called for: " + puuid);
        return rateLimiter.acquirePermission()
                .then(webClient.get()
                        .uri(summonerUrl)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToMono(Map.class)
                )
                .flatMap(summonerData -> {
                    SummonerEntity entity = SummonerEntity.builder()
                            .puuid((String) summonerData.get("puuid"))
                            .gameName((String) summonerData.get("name"))
                            .tagLine((String) summonerData.get("tagLine"))
                            .profileIconId(((Number) summonerData.get("profileIconId")).intValue())
                            .revisionDate(((Number) summonerData.get("revisionDate")).longValue())
                            .summonerLevel(((Number) summonerData.get("summonerLevel")).intValue())
                            .lastUpdated(System.currentTimeMillis())

                            .build();

                    return Mono.fromCallable(() -> {
                        log.info("==== INSIDE fromCallable for puuid: " + puuid);
                        Optional<SummonerEntity> existingOpt = summonerRepository.findById(puuid);


                        if (existingOpt.isPresent()) {

                            SummonerEntity existing = existingOpt.get();

                            if (existing.getRevisionDate() != entity.getRevisionDate()) {

                                existing.setProfileIconId(entity.getProfileIconId());
                                existing.setRevisionDate(entity.getRevisionDate());
                                existing.setSummonerLevel(entity.getSummonerLevel());
                                existing.setSummonerLevel(System.currentTimeMillis());


                                summonerRepository.save(existing);

                                return true;
                            }
                            return false;
                        }
                        else {
                            entity.setLastUpdated(System.currentTimeMillis());
                            summonerRepository.save(entity);
                            return true;
                        }

                    }).subscribeOn(Schedulers.boundedElastic())
                            .doFinally(result -> {
                                log.info("==== doOnSuccess called with result: " + result);
                                log.info("About to trigger match fetch for: " + puuid);
                                triggerInitialMatchFetch(puuid,region);
                                log.info("triggerInitialMatchFetch called!");

                            });

                })
                .onErrorResume(e -> {
                    log.severe(">>> ERROR in fetchAndSaveSummoner: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(false);
                });
    }


    @Override
    public Mono<SummonerDto> fetchAndMapSummonerEntity(String gameName, String tagLine, String region) {

        //check if summoner exists before
        String regionalRouting = getRegionalRouting(region);
        String platformRouting = getPlatformRouting(region);

        String accountUrl = String.format("https://%s.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
               regionalRouting , gameName, tagLine);


        return rateLimiter.acquirePermission()
                .then(webClient.get()
                        .uri(accountUrl)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .flatMap(accountData -> {
                            String puuid = (String) accountData.get("puuid");

                            if (puuid == null) {
                                log.warning("No PUUID found for " + gameName + "#" + tagLine);
                                return Mono.empty();
                            }

                            String summonerUrl = String.format("https://%s.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s",
                                    platformRouting, puuid);

                            return rateLimiter.acquirePermission()
                                    .then(webClient.get()
                                            .uri(summonerUrl)
                                            .header("X-Riot-Token", apiKey)
                                            .retrieve()
                                            .bodyToMono(Map.class)
                                            .map(summonerData -> {
                                                return SummonerDto.builder()
                                                        .puuid((String) summonerData.get("puuid"))
                                                        .gameName(String.valueOf(gameName))
                                                        .tagLine(String.valueOf(tagLine))
                                                        .region(String.valueOf(region))
                                                        .profileIconId(((Number) summonerData.get("profileIconId")).intValue())
                                                        .revisionDate(((Number) summonerData.get("revisionDate")).longValue())
                                                        .summonerLevel(((Number) summonerData.get("summonerLevel")).intValue())


                                                        .build();
                                            })
                                    );
                        })
                        .onErrorResume(e -> {
                            log.severe("Failed to fetch summoner: " + e.getMessage());
                            return Mono.empty();
                        })
                );
    }

    @Override
    public Mono<List<RankedStatsDto>> fetchRankedStats(String puuid,String region){

        String platformRouting = getPlatformRouting(region);
        String Url = String.format("https://%s.api.riotgames.com/lol/league/v4/entries/by-puuid/%s", platformRouting, puuid);

        return rateLimiter.acquirePermission()
                .then(webClient.get()
                        .uri(Url)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .flatMapMany(Flux::fromIterable)
                        .flatMap(rankedStats -> Mono.fromCallable(() -> {
                            RankedStatsDto rankedStatsDto = RankedStatsDto.builder()
                                    .queueType((String) rankedStats.get("queueType"))
                                    .tier((String) rankedStats.get("tier"))
                                    .rank((String) rankedStats.get("rank"))
                                    .leaguePoints((Integer) rankedStats.get("leaguePoints"))
                                    .wins((Integer) rankedStats.get("wins"))
                                    .losses((Integer) rankedStats.get("losses"))
                                    .veteran((Boolean) rankedStats.get("veteran"))
                                    .inactive((Boolean) rankedStats.get("inactive"))
                                    .freshBlood((Boolean) rankedStats.get("freshBlood"))
                                    .hotStreak((Boolean) rankedStats.get("hotStreak"))
                                    .build();

                            RankedStatsEntity rankedStatsEntity = rankedStatsMapper.mapFrom(rankedStatsDto);


                            Optional<SummonerEntity> summonerOpt = summonerRepository.findById(puuid);
                            if (summonerOpt.isEmpty()) {
                                log.severe("Summoner not found in database for puuid: " + puuid);
                                throw new EntityNotFoundException("Summoner not found for puuid: " + puuid);
                            }

                            SummonerEntity summoner = summonerOpt.get();
                            log.info("Saving summoner for RankedStatsEntity: " + summoner);
                            rankedStatsEntity.setSummoner(summoner);

                            log.info("Saving rankedStatsEntity: " + rankedStatsEntity);
                            rankedStatsRepository.save(rankedStatsEntity);
                            return rankedStatsDto;

                        }).subscribeOn(Schedulers.boundedElastic()))
                        .collectList()
                        .onErrorResume(e -> {
                            log.severe("Failed to fetch ranked stats: " + e.getMessage());
                            log.severe("Error type: " + e.getClass().getName());
                            e.printStackTrace();
                            return Mono.error(e);
                        })
                );
    }




    @Override
    public Mono<List<String>> fetchMatchIds(String puuid,String region) {
        String regionalRouting = getRegionalRouting(region);

        String matchesUrl = String.format("https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=40",
                regionalRouting, puuid);

        return rateLimiter.acquirePermission()
                .then(webClient.get()
                        .uri(matchesUrl)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                        .onErrorResume(e -> {
                            log.warning("Failed to fetch match IDs for " + puuid + ": " + e.getMessage());
                            return Mono.empty();
                        })
                );
    }


    @Override
    public Mono<Void> fetchAndSaveMatchDetails(String matchId, String region) {

        String regionalRouting = getRegionalRouting(region);

        return Mono.fromCallable(() -> matchRepository.existsById(matchId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Match already exists: " + matchId);
                        return Mono.empty();
                    }


                    String url = String.format("https://%s.api.riotgames.com/lol/match/v5/matches/%s", regionalRouting, matchId);

                    return rateLimiter.acquirePermission()
                            .then(webClient.get()
                                    .uri(url)
                                    .header("X-Riot-Token", apiKey)
                                    .retrieve()
                                    .bodyToMono(Map.class)
                                    .flatMap(raw -> {
                                        Map<String, Object> info = (Map<String, Object>) raw.get("info");
                                        if (info == null) {
                                            log.warning("No info found in match data for " + matchId);
                                            return Mono.empty();
                                        }


                                        MatchDto matchDto = MatchDto.builder()
                                                .matchId(matchId)
                                                .gameMode((String) info.get("gameMode"))
                                                .gameDuration(((Number) info.get("gameDuration")).longValue())
                                                .gameCreation(((Number) info.get("gameCreation")).longValue())
                                                .queueId(((Number) info.get("queueId")).intValue())
                                                .build();


                                        List<Map<String, Object>> rawParticipants = (List<Map<String, Object>>) info.get("participants");
                                        if (rawParticipants == null || rawParticipants.isEmpty()) {
                                            log.warning("No participants found for match " + matchId);
                                            return Mono.empty();
                                        }

                                        List<ParticipantDto> participants = rawParticipants.stream()
                                                .map(p -> ParticipantDto.builder()
                                                        .name((String) p.get("name"))
                                                        .puuid((String) p.get("puuid"))
                                                        .championId(((Number) p.get("championId")).intValue())
                                                        .win((Boolean) p.get("win"))
                                                        .kills(((Number) p.get("kills")).intValue())
                                                        .deaths(((Number) p.get("deaths")).intValue())
                                                        .assists(((Number) p.get("assists")).intValue())
                                                        .totalMinionsKilled(((Number) p.get("totalMinionsKilled")).intValue())
                                                        .goldEarned(((Number) p.get("goldEarned")).intValue())
                                                        .build())
                                                .collect(Collectors.toList());

                                        matchDto.setParticipants(participants);


                                        MatchEntity matchEntity = matchMapper.mapFrom(matchDto);
                                        if (matchEntity.getParticipants() != null) {
                                            matchEntity.getParticipants().forEach(p -> p.setMatch(matchEntity));
                                        }


                                        return Mono.fromCallable(() -> {
                                                    log.info(">>> SAVING match to database: " + matchEntity.getMatchId());
                                                    return matchRepository.saveAndFlush(matchEntity);
                                                })
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .doOnSuccess(saved -> log.info(">>> SAVED successfully! Match ID: " + saved.getMatchId()))
                                                .onErrorResume(DataIntegrityViolationException.class, e -> {
                                                    log.warning("Duplicate match or participant for " + matchEntity.getMatchId());
                                                    return Mono.empty();
                                                })
                                                .then();
                                    }));
                });
    }


    @Override
    public Mono<Void> triggerInitialMatchFetch(String puuid, String region) {
        log.info("Starting background match fetch for PUUID: " + puuid);

        return fetchMatchIds(puuid, region)
                .flatMapMany(matchIds -> {
                    log.info("Found " + matchIds.size() + " matches for PUUID: " + puuid);
                    return Flux.fromIterable(matchIds);
                })
                .flatMap(matchId ->
                                fetchAndSaveMatchDetails(matchId, region)
                                        .doOnNext(v -> log.info("Successfully saved match: " + matchId))
                                        .onErrorResume(e -> {
                                            log.warning("Failed to save match " + matchId + ": " + e);
                                            return Mono.empty();
                                        }),
                        1
                )
                .then()
                .doOnSuccess(v -> log.info("Completed match fetch for PUUID: " + puuid))
                .doOnError(error -> log.severe("Critical error in match fetch pipeline for " + puuid + ": " + error));
    }



}