package com.Alex.RiotTrackerApplication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedStatsKafkaRequest {
    private String requestId;
    private String puuid;
    private String region;
    private long timestamp;
}
