package com.Alex.RiotTrackerApplication.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Schema(description = "Full player information containing the profile, overall stats, ranked stats, champion stats and recent matches")
public class FrontEndResponseDto {

    @Schema(description = "Basic information about the summoner")
    private SummonerDto summoner;

    @Schema(description = "Overall gameplay statistics of the summoner")
    private SummonerStatsDto overallStats;

    @Schema(description = "Ranked profile information including tiers and LP")
    private RankProfileDto rankedProfile;

    @Schema(description = "Champion statistics across all modes")
    private List<ChampionStatsDto> overallChampionStats;

    @Schema(description = "List of recently played matches")
    private List<MatchHistoryDto> recentMatches;
}
