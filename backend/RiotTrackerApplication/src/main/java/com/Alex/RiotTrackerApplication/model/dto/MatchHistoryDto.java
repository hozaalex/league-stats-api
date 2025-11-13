package com.Alex.RiotTrackerApplication.model.dto;


import com.Alex.RiotTrackerApplication.mappers.impl.ChampionMapping;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchHistoryDto {

    private String matchId;
    private String gameMode;
    private Long gameDuration;
    private Long gameCreation;

    private String championName;
    @JsonIgnore
    private Integer championId;

    private Boolean win;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer totalMinionsKilled;
    private Integer goldEarned;


    public MatchHistoryDto(String matchId,String gameMode,Long gameDuration,Long gameCreation,
    Integer championId,Boolean win, Integer kills, Integer deaths, Integer assists, Integer totalMinionsKilled,Integer goldEarned) {
        this.matchId = matchId;
        this.gameMode = gameMode;
        this.gameDuration = gameDuration;
        this.gameCreation = gameCreation;
        this.championName = ChampionMapping.getChampionName(championId);
        this.win = win;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.totalMinionsKilled = totalMinionsKilled;
        this.goldEarned = goldEarned;
    }
}

//Todo: ranked statistics and frontend 