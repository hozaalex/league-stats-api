package com.Alex.RiotTrackerApplication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FrontEndResponseDto {

    private SummonerDto summoner;

    private SummonerStatsDto overallStats;

    private RankProfileDto rankedProfile;

    private List<ChampionStatsDto> overallChampionStats;

    private List<MatchHistoryDto> recentMatches;
}
