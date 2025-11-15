package com.Alex.RiotTrackerApplication.model.dto;

import com.Alex.RiotTrackerApplication.mappers.impl.ChampionMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Schema(description = "Statistics for a given champion")
public class ChampionStatsDto {

    @Schema(description = "Name of the champion", example = "Ahri")
    String championName;

    @Schema(description = "Unique numerical ID of the champion", example = "103")
    long championId;

    @Schema(description = "Total number of matches played with this champion", example = "125")
    long totalMatches;

    @Schema(description = "Number of games won with this champion", example = "70")
    long wins;

    @Schema(description = "Number of games lost with this champion", example = "55")
    long losses;

    @Schema(description = "Win rate as a formatted percentage string", example = "56.00%")
    String winRate;

    @Schema(description = "Average kills per match", example = "7.3")
    double avgKills;

    @Schema(description = "Average deaths per match", example = "5.2")
    double avgDeaths;

    @Schema(description = "Average assists per match", example = "6.8")
    double avgAssists;

    @Schema(description = "Kill/Death/Assist ratio", example = "2.71")
    double kda;

    @Schema(description = "Average minions killed per match", example = "185.6")
    double avgMinions;

    //Constructor used for JPQL @Query projections
    public ChampionStatsDto(Integer championId, long totalMatches, long wins,
                            double avgKills, double avgDeaths, double avgAssists,
                            double avgMinions) {

        this.championId = championId;
        this.championName = ChampionMapping.getChampionName(championId);
        this.totalMatches = totalMatches;
        this.wins = wins;
        this.losses = totalMatches - wins;
        this.avgKills = avgKills;
        this.avgDeaths = avgDeaths;
        this.avgAssists = avgAssists;
        this.avgMinions = avgMinions;
        Double winrate = totalMatches == 0 ? 0 : (wins * 100.0) / totalMatches;
        this.winRate = String.format("%.2f", winrate) + "%";
        this.kda = avgDeaths == 0 ? (avgKills + avgAssists) : (avgKills + avgAssists) / avgDeaths;
    }
}
