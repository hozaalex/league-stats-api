package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SummonerService {

    public Optional<SummonerEntity> getSummonerByPuuid(String puuid);

    public Optional<SummonerEntity> getSummonerByName(String summonerName);

    public List<SummonerEntity> getAllSummoners();

    public List<String> getAllPuuids();

    SummonerEntity saveOrUpdateSummoner(SummonerEntity summoner);


    Optional<SummonerEntity> findById(String puuid);

    Map<String, String> getAllPuuidsAndRegions();
}
