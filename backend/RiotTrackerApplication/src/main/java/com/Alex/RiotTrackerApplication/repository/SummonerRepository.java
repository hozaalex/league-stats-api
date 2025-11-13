package com.Alex.RiotTrackerApplication.repository;

import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface SummonerRepository extends JpaRepository<SummonerEntity,String> {

    Optional<SummonerEntity> findByGameName(String name);

    @Query("SELECT s.puuid AS puuid, s.region AS region FROM SummonerEntity s")
    List<Object[]> findAllPuuidsAndRegions();
}
