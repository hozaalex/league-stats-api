package com.Alex.RiotTrackerApplication.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name="summoners")

public class SummonerEntity {


    @Id
    private String puuid;

    private String tagLine;

    private String gameName;

    private int profileIconId;

    private long revisionDate;

    private long summonerLevel;

    private String region;

    private long lastUpdated;
}
