package com.Alex.RiotTrackerApplication.service;



public interface KafkaProducerService {

    public String sendSummonerRequest(String gameName, String tagLine, String region, String userIp);

}
