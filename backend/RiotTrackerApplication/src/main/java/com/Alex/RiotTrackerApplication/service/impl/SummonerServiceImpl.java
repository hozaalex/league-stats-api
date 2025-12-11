package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.repository.SummonerRepository;
import com.Alex.RiotTrackerApplication.service.SummonerService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
@Service
public class SummonerServiceImpl implements SummonerService {

    private final SummonerRepository summonerRepository;

    public SummonerServiceImpl(SummonerRepository summonerRepository) {
        this.summonerRepository = summonerRepository;
    }

    @Override
    public Optional<SummonerEntity> getSummonerByPuuid(String puuid) {
        return summonerRepository.findById(puuid);
    }

    @Override
    public Optional<SummonerEntity> getSummonerByName(String summonerName) {
        return summonerRepository.findByGameName(summonerName);
    }

    @Override
    public List<SummonerEntity> getAllSummoners() {
        return StreamSupport.stream(summonerRepository.findAll().spliterator(),false).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllPuuids() {
        return summonerRepository.findAll().stream()
                .map(SummonerEntity::getPuuid)
                .collect(Collectors.toList());
    }


    @Override
    public SummonerEntity saveOrUpdateSummoner(SummonerEntity summoner) {
        Optional<SummonerEntity> existingOpt = summonerRepository.findById(summoner.getPuuid());

        if (existingOpt.isPresent()) {
            SummonerEntity existing = existingOpt.get();
            existing.setGameName(summoner.getGameName());
            existing.setTagLine(summoner.getTagLine());
            existing.setProfileIconId(summoner.getProfileIconId());
            existing.setRevisionDate(summoner.getRevisionDate());
            existing.setSummonerLevel(summoner.getSummonerLevel());
            existing.setRegion(summoner.getRegion());
            existing.setLastUpdated(System.currentTimeMillis());

            return summonerRepository.save(existing);
        } else {
            summoner.setLastUpdated(System.currentTimeMillis());
            return summonerRepository.save(summoner);
        }
    }

    @Override
    public Optional<SummonerEntity> findById(String puuid) {
        return summonerRepository.findById(puuid);
    }

    @Override
    public Map<String, String> getAllPuuidsAndRegions() {
        Map<String, String> puuidRegionMap = summonerRepository.findAllPuuidsAndRegions().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (String) arr[1]
                ));
        return puuidRegionMap;
    }

    @Override
    public Boolean isDataFresh(SummonerEntity summoner) {
        long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
        return summoner.getLastUpdated() >= tenMinutesAgo;
    }

    @Override
    public Optional<SummonerEntity> findByGameNameAndTagLineAndRegion(String gameName, String tagLine, String region) {
        return summonerRepository.findByGameNameAndTagLineAndRegion(gameName, tagLine, region);
    }
}
