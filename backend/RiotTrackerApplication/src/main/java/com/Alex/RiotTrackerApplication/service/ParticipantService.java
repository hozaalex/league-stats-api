package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.MatchHistoryDto;
import com.Alex.RiotTrackerApplication.model.dto.SummonerStatsDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface ParticipantService {

    public List<ParticipantEntity>  getParticipantsByPuuid(String puuid);


    public SummonerStatsDto getPlayerStatsSummary(String puuid);

    List<ChampionStatsDto> findChampionStatsByPlayer(String puuid);

    ChampionStatsDto getChampionStatsSummary(String puuid,long championId);

    Page<MatchHistoryDto> getMatchHistory(String puuid, Pageable pageable);

    List<ChampionStatsDto> findRankedChampionStatsByPlayer(String puuid);





}
