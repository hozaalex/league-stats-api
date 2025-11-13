package com.Alex.RiotTrackerApplication.repository;

import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankedStatsRepository extends JpaRepository<RankedStatsEntity,Long> {

    List<RankedStatsEntity> findBySummonerPuuid(String puuid);
    RankedStatsEntity findTopBySummonerPuuid(String puuid);
}
