package com.Alex.RiotTrackerApplication.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RiotIdRequestDto {

    private String gameName;
    private String tagLine;
    private String region;
}
