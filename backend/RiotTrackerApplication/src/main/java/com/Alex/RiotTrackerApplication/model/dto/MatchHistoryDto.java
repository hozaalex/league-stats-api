package com.Alex.RiotTrackerApplication.model.dto;

import com.Alex.RiotTrackerApplication.mappers.impl.ChampionMapping;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Match information for displaying player match history")
public class MatchHistoryDto {

    @Schema(description = "Match identifier", example = "EUW1_6789123456")
    private String matchId;

    @Schema(description = "Game mode played", example = "ARAM")
    private String gameMode;

    @Schema(description = "Match duration in seconds", example = "1450")
    private Long gameDuration;

    @Schema(description = "Match creation timestamp", example = "1712345678000")
    private Long gameCreation;

    @Schema(description = "Name of the champion played", example = "Ashe")
    private String championName;

    @JsonIgnore
    @Schema(description = "Champion ID (internally mapped)", example = "22")
    private Integer championId;

    @Schema(description = "Whether the player won the match", example = "true")
    private Boolean win;

    @Schema(description = "Kills achieved in the match", example = "10")
    private Integer kills;

    @Schema(description = "Deaths in the match", example = "2")
    private Integer deaths;

    @Schema(description = "Assists in the match", example = "8")
    private Integer assists;

    @Schema(description = "Total minions killed", example = "190")
    private Integer totalMinionsKilled;

    @Schema(description = "Total gold earned", example = "14500")
    private Integer goldEarned;

    public MatchHistoryDto(String matchId, String gameMode, Long gameDuration, Long gameCreation,
                           Integer championId, Boolean win, Integer kills, Integer deaths,
                           Integer assists, Integer totalMinionsKilled, Integer goldEarned) {

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
