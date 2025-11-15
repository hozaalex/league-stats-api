package com.Alex.RiotTrackerApplication.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Basic summoner profile information")
public class SummonerDto {

    @Schema(description = "PUUID of the summoner", example = "abcd-1234-puuid-example")
    private String puuid;

    @Schema(description = "Summoner's game name", example = "Alex")
    private String gameName;

    @Schema(description = "Summoner tagline", example = "EUW")
    private String tagLine;

    @Schema(description = "ID of profile icon", example = "23")
    private Integer profileIconId;

    @Schema(description = "Last profile update timestamp", example = "1712345678000")
    private Long revisionDate;

    @Schema(description = "Summoner level", example = "527")
    private Integer summonerLevel;

    @Schema(description = "Region of the summoner", example = "EUW1")
    private String region;
}
