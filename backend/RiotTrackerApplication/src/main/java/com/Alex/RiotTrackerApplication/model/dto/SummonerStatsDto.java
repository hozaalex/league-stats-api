package com.Alex.RiotTrackerApplication.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SummonerStatsDto {

    private String summonerName;
    private String puuid;
    private int totalGames;
    private long wins;
    private long losses;
    private double winRate;
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;
    private double kda;
    private double avgGold;
    private double avgMinions;

}
