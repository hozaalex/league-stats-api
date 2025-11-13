package com.Alex.RiotTrackerApplication.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "ranked_stats")
public class RankedStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String queueType;
    @ManyToOne
    @JoinColumn(name = "summoner_puuid", referencedColumnName = "puuid") //will map to the puuid field in summonerEntity
    private SummonerEntity summoner;

    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;

    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;

    @Column(nullable = false, updatable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        this.capturedAt = LocalDateTime.now();
    }
}
