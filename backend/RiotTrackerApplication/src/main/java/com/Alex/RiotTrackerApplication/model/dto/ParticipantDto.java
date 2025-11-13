package com.Alex.RiotTrackerApplication.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ParticipantDto {

    private String name;
    private String puuid;
    private MatchDto match;
    private Integer championId;
    private Boolean win;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer totalMinionsKilled;
    private Integer goldEarned;
}

