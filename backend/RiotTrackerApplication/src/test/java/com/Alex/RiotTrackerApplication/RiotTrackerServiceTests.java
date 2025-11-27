package com.Alex.RiotTrackerApplication;


import com.Alex.RiotTrackerApplication.mappers.impl.MatchMapper;
import com.Alex.RiotTrackerApplication.mappers.impl.RankedStatsMapper;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.RankedStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.SummonerDto;
import com.Alex.RiotTrackerApplication.rate.RiotRateLimiter;
import com.Alex.RiotTrackerApplication.repository.MatchRepository;
import com.Alex.RiotTrackerApplication.repository.RankedStatsRepository;
import com.Alex.RiotTrackerApplication.repository.SummonerRepository;
import com.Alex.RiotTrackerApplication.service.RiotApiService;
import com.Alex.RiotTrackerApplication.service.impl.RiotApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RiotTrackerServiceTests {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private RiotRateLimiter rateLimiter;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private SummonerRepository summonerRepository;

    @Mock
    private RankedStatsRepository rankedStatsRepository;

    @Mock
    private RankedStatsMapper rankedStatsMapper;

    private RiotApiServiceImpl riotApiService;

    private final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {

        when(webClientBuilder.build()).thenReturn(webClient);

        riotApiService = new RiotApiServiceImpl(
                webClientBuilder,
                rateLimiter,
                matchRepository,
                matchMapper,
                summonerRepository,
                rankedStatsRepository,
                rankedStatsMapper
        );


        ReflectionTestUtils.setField(riotApiService, "apiKey", TEST_API_KEY);

        lenient().when(rateLimiter.acquirePermission()).thenReturn(Mono.empty());


        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }



    @Test
    void shouldReturnCorrectPlatformRouting_forEUW() {

        String result = RiotApiServiceImpl.getPlatformRouting("EUW");


        assertEquals("euw1", result);
    }

    @Test
    void shouldReturnCorrectPlatformRouting_forNA() {

        String result = RiotApiServiceImpl.getPlatformRouting("NA");


        assertEquals("na1", result);
    }

    @Test
    void shouldReturnDefaultRouting_whenRegionIsNull() {

        String result = RiotApiServiceImpl.getPlatformRouting(null);


        assertEquals("euw1", result);
    }

    @Test
    void shouldReturnCorrectRegionalRouting_forEUW() {

        String result = RiotApiServiceImpl.getRegionalRouting("EUW");


        assertEquals("europe", result);
    }

    @Test
    void shouldReturnCorrectRegionalRouting_forNA() {

        String result = RiotApiServiceImpl.getRegionalRouting("NA");


        assertEquals("americas", result);
    }

    @Test
    void shouldReturnCorrectRegionalRouting_forKR() {

        String result = RiotApiServiceImpl.getRegionalRouting("KR");


        assertEquals("asia", result);
    }

    @Test
    void shouldReturnEmpty_whenPuuidNotFoundInAccountResponse(){

        String gameName = "NonExistent";
        String tagLine = "NA1";
        String region = "NA";


        Map<String, Object> accountData = new HashMap<>();

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(accountData));

        Mono<SummonerDto> result = riotApiService.fetchAndMapSummonerEntity(gameName, tagLine, region);

        StepVerifier.create(result)
                .verifyComplete();

        verify(rateLimiter, times(1)).acquirePermission();


    }

    @Test
    void shouldFetchAndMapSummonerEntity_successfully(){

        String gameName= "Faker";
        String tagLine = "KR1";
        String region = "KR";



        Map<String, Object> accountData = new HashMap<>();
        accountData.put("puuid","test-puuid-123");
        accountData.put("gameName",gameName);
        accountData.put("tagline",tagLine);
        accountData.put("region",region);

        Map<String, Object> summonerData = new HashMap<>();
        summonerData.put("puuid", "test-puuid-123");
        summonerData.put("name", gameName);
        summonerData.put("profileIconId", 1234);
        summonerData.put("revisionDate", 1234567890L);
        summonerData.put("summonerLevel", 300);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(accountData))
                .thenReturn(Mono.just(summonerData));

        Mono<SummonerDto> result = riotApiService.fetchAndMapSummonerEntity(gameName, tagLine, region);
        StepVerifier.create(result)
                .expectNextMatches(dto ->
                        dto.getPuuid().equals("test-puuid-123") &&
                                dto.getGameName().equals(gameName) &&
                                dto.getTagLine().equals(tagLine) &&
                                dto.getProfileIconId() == 1234 &&
                                dto.getSummonerLevel() == 300
                )
                .verifyComplete();


        verify(rateLimiter, times(2)).acquirePermission();

    }

    @Test
    void shouldHandleError_whenAccountApiFails(){

        String gameName = "Test";
        String tagLine = "EUW";
        String region = "EUW";

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));
        Mono<SummonerDto> result = riotApiService.fetchAndMapSummonerEntity(gameName, tagLine, region);

        StepVerifier.create(result)
                .verifyComplete();

        verify(rateLimiter, times(1)).acquirePermission();


    }


    @Test
    void shouldFetchMatchIds_successfully(){
        String puuid = "test-puuid";
        String region = "EUW";
        List<String> matchIds = Arrays.asList(
                "EUW1_123456",
                "EUW1_123457",
                "EUW1_123458"
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(matchIds));
        Mono<List<String>> result = riotApiService.fetchMatchIds(puuid, region);


        StepVerifier.create(result)
                .expectNextMatches(matches ->
                        matches.size() == 3 &&
                                matches.contains("EUW1_123456")
                )
                .verifyComplete();

        verify(rateLimiter).acquirePermission();

    }

    @Test
    void shouldReturnEmpty_whenMatchIdsFetchFails() {

        String puuid = "test-puuid";
        String region = "EUW";

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("API Error")));
        Mono<List<String>> result = riotApiService.fetchMatchIds(puuid, region);


        StepVerifier.create(result)
                .verifyComplete();

        verify(rateLimiter).acquirePermission();
    }

    @Test
    void shouldRespectRateLimiter_whenFetchingSummoner() {

        String gameName = "Test";
        String tagLine = "EUW";
        String region = "EUW";

        Map<String, Object> accountData = new HashMap<>();
        accountData.put("puuid", "test-puuid");

        Map<String, Object> summonerData = new HashMap<>();
        summonerData.put("puuid", "test-puuid");
        summonerData.put("name", gameName);
        summonerData.put("profileIconId", 1);
        summonerData.put("revisionDate", 1L);
        summonerData.put("summonerLevel", 30);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(accountData))
                .thenReturn(Mono.just(summonerData));
        riotApiService.fetchAndMapSummonerEntity(gameName, tagLine, region).block();


        verify(rateLimiter, times(2)).acquirePermission();
    }



    @Test
    void shouldSkipSaving_whenMatchAlreadyExists() {

        String matchId = "EUW1_123456";
        String region = "EUW";

        when(matchRepository.existsById(matchId)).thenReturn(true);
        Mono<Void> result = riotApiService.fetchAndSaveMatchDetails(matchId, region);

        StepVerifier.create(result)
                .verifyComplete();
        verify(webClient, never()).get();
        verify(rateLimiter, never()).acquirePermission();
    }





}
