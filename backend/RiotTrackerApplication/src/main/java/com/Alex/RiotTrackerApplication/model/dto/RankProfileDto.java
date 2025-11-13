package com.Alex.RiotTrackerApplication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankProfileDto {

    private List<RankedStatsDto> rankedStatsDto;
    private List<ChampionStatsDto> championPerformance;
}
