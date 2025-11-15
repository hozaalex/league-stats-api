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
@Schema(description = "Information for a single participant in a match")
public class ParticipantDto {

    @Schema(description = "Summoner name", example = "Alex")
    private String name;

    @Schema(description = "PUUID of the summoner", example = "abcd-1234-puuid-example")
    private String puuid;

    @Schema(description = "Match the participant belongs to")
    private MatchDto match;

    @Schema(description = "Champion ID used in the match", example = "157")
    private Integer championId;

    @Schema(description = "Whether the participant won the game", example = "false")
    private Boolean win;

    @Schema(description = "Kills achieved by the participant", example = "12")
    private Integer kills;

    @Schema(description = "Deaths suffered by the participant", example = "3")
    private Integer deaths;

    @Schema(description = "Assists provided", example = "9")
    private Integer assists;

    @Schema(description = "Total minions killed", example = "210")
    private Integer totalMinionsKilled;

    @Schema(description = "Gold earned during the match", example = "13250")
    private Integer goldEarned;
}
