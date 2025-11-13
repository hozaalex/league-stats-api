package com.Alex.RiotTrackerApplication.model.dto;

import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedStatsDto {

    private String queueType;
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;

    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;

    private LocalDateTime capturedAt;

    private SummonerEntity summoner;
}
