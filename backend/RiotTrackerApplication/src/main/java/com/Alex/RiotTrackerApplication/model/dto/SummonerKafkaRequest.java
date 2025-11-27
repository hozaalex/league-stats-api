package com.Alex.RiotTrackerApplication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerKafkaRequest {
    private String requestId;
    private String gameName;
    private String tagLine;
    private String region;
    private String userIp;
    private long timestamp;
}
