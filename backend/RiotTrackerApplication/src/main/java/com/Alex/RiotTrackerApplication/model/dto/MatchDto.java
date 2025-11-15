package com.Alex.RiotTrackerApplication.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Schema(description = "Full match data including mode, duration and participants")
public class MatchDto {

    @Schema(description = "Unique match identifier", example = "EUW1_6789123456")
    private String matchId;

    @Schema(description = "Game mode of the match", example = "CLASSIC")
    private String gameMode;

    @Schema(description = "Duration of the match in seconds", example = "1800")
    private long gameDuration;

    @Schema(description = "Timestamp of when the match was created", example = "1712345678000")
    private long gameCreation;

    @Schema(description = "List of participants who played in the match")
    private List<ParticipantDto> participants;

    @Schema(description = "Queue ID corresponding to match type", example = "420")
    private Integer queueId;
}
