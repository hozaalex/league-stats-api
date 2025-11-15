package com.Alex.RiotTrackerApplication.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Schema(description = "Overall lifetime gameplay statistics of the summoner")
public class SummonerStatsDto {

    @Schema(description = "Summoner name", example = "Alex")
    private String summonerName;

    @Schema(description = "PUUID of the summoner", example = "abcd-1234-puuid-example")
    private String puuid;

    @Schema(description = "Total number of games played", example = "2500")
    private int totalGames;

    @Schema(description = "Total wins", example = "1300")
    private long wins;

    @Schema(description = "Total losses", example = "1200")
    private long losses;

    @Schema(description = "Overall win rate", example = "52.0")
    private double winRate;

    @Schema(description = "Total kills", example = "15000")
    private int totalKills;

    @Schema(description = "Total deaths", example = "8500")
    private int totalDeaths;

    @Schema(description = "Total assists", example = "18000")
    private int totalAssists;

    @Schema(description = "Average KDA ratio", example = "3.12")
    private double kda;

    @Schema(description = "Average gold earned per match", example = "12345.7")
    private double avgGold;

    @Schema(description = "Average minions killed per match", example = "185.3")
    private double avgMinions;

}
