package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.controller.SummonerController;
import com.Alex.RiotTrackerApplication.mappers.Mapper;
import com.Alex.RiotTrackerApplication.mappers.impl.RankedStatsMapper;
import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.RankProfileDto;
import com.Alex.RiotTrackerApplication.model.dto.RankedStatsDto;
import com.Alex.RiotTrackerApplication.repository.RankedStatsRepository;
import com.Alex.RiotTrackerApplication.service.ParticipantService;
import com.Alex.RiotTrackerApplication.service.RankedStatsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RankedStatsServiceImpl implements RankedStatsService {

    private static final Logger log = Logger.getLogger(RankedStatsServiceImpl.class.getName());
    private RankedStatsRepository rankedStatsRepository;
    private ParticipantService participantService;
    private Mapper<RankedStatsEntity, RankedStatsDto> rankedStatsMapper;

    public RankedStatsServiceImpl(RankedStatsRepository rankedStatsRepository, ParticipantService participantService, Mapper<RankedStatsEntity, RankedStatsDto> rankedStatsMapper) {
        this.rankedStatsRepository = rankedStatsRepository;
        this.participantService = participantService;
        this.rankedStatsMapper = rankedStatsMapper;

    }

    @Override
    @Cacheable(value = "rankedProfile",key = "#puuid")
    public RankProfileDto getRankProfile(String puuid) {

        List<ChampionStatsDto> championPerformance = participantService.findRankedChampionStatsByPlayer(puuid);

        List<RankedStatsEntity> rankedStatsEntities = rankedStatsRepository.findBySummonerPuuid(puuid);
        if(rankedStatsEntities == null) {
            throw new EntityNotFoundException("No ranked stats found for puuid: " + puuid);
        }
        Map<String, RankedStatsEntity> latestByQueueType = rankedStatsEntities.stream()
                .collect(Collectors.groupingBy(
                        RankedStatsEntity::getQueueType,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(RankedStatsEntity::getCapturedAt)),
                                Optional::get
                        )
                ));

        List<RankedStatsDto> rankedStatsDto = latestByQueueType.values().stream()
                .map(rankedStatsMapper::mapTo)
                .collect(Collectors.toList());

        return RankProfileDto.builder()
                .rankedStatsDto(rankedStatsDto)
                .championPerformance(championPerformance)
                .build();
    }
}
