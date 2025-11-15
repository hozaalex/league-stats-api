package com.Alex.RiotTrackerApplication.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile containing ranked stats and champion performance data")
public class RankProfileDto {

    @Schema(description = "List of ranked queue statistics")
    private List<RankedStatsDto> rankedStatsDto;

    @Schema(description = "Champion performance metrics for ranked modes")
    private List<ChampionStatsDto> championPerformance;
}
