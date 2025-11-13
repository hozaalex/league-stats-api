package com.Alex.RiotTrackerApplication.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerDto {


    private String puuid;
    private String gameName;
    private String tagLine;
    private Integer profileIconId;
    private Long revisionDate;
    private Integer summonerLevel;
    private String region;
}
