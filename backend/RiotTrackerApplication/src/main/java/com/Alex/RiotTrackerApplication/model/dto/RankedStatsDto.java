package com.Alex.RiotTrackerApplication.model.dto;

import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ranked gameplay statistics for a summoner")
public class RankedStatsDto {

    @Schema(description = "Queue type (e.g. RANKED_SOLO_5x5)", example = "RANKED_SOLO_5x5")
    private String queueType;

    @Schema(description = "Current tier of the summoner", example = "DIAMOND")
    private String tier;

    @Schema(description = "Rank within the tier", example = "II")
    private String rank;

    @Schema(description = "League points", example = "78")
    private int leaguePoints;

    @Schema(description = "Total wins in this queue", example = "150")
    private int wins;

    @Schema(description = "Total losses in this queue", example = "132")
    private int losses;

    @Schema(description = "Whether the player is a veteran", example = "false")
    private boolean veteran;

    @Schema(description = "Whether the player is inactive", example = "false")
    private boolean inactive;

    @Schema(description = "Whether the player is fresh blood", example = "true")
    private boolean freshBlood;

    @Schema(description = "Whether the player is on a hot streak", example = "true")
    private boolean hotStreak;

    @Schema(description = "Timestamp when stats were captured", example = "2024-03-12T15:30:00")
    private LocalDateTime capturedAt;

    @Schema(description = "Reference to summoner entity")
    private SummonerEntity summoner;
}
