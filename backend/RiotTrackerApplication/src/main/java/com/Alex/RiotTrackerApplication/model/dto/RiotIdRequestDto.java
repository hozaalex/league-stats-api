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
@Schema(description = "Request payload containing Riot ID and region information")
public class RiotIdRequestDto {

    @Schema(description = "Riot game name of the player", example = "Alex")
    private String gameName;

    @Schema(description = "Tagline of the Riot ID", example = "EUW or 0000")
    private String tagLine;

    @Schema(description = "Region from which to fetch data", example = "europe")
    private String region;
}
