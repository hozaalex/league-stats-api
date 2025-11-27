package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.dto.MatchFetchKafkaRequest;
import com.Alex.RiotTrackerApplication.model.dto.RankedStatsKafkaRequest;
import com.Alex.RiotTrackerApplication.model.dto.SummonerKafkaRequest;

public interface KafkaConsumerService {


    void consumeSummonerRequest(SummonerKafkaRequest request,int partition,long offset);



}
