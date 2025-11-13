package com.Alex.RiotTrackerApplication.model.dto;


import com.Alex.RiotTrackerApplication.mappers.impl.ChampionMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChampionStatsDto {


    String championName;
    long championId;
    long totalMatches;
    long wins;
    long losses;
    String winRate;
    double avgKills;
    double avgDeaths;
    double avgAssists;
    double kda;
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
        Double winrate= totalMatches == 0 ? 0 : (wins * 100.0) / totalMatches;
        this.winRate = String.format("%.2f", winrate)+"%";
        this.kda = avgDeaths == 0 ? (avgKills + avgAssists) : (avgKills + avgAssists) / avgDeaths;
    }







}
