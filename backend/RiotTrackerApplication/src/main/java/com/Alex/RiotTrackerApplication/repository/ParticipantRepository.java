package com.Alex.RiotTrackerApplication.repository;

import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.MatchHistoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, Integer> {

    List<ParticipantEntity> findAllByPuuid(String puuid);
    @Query("""
    SELECT new com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto(
        p.championId,
        COUNT(p) as totalMatches,
        SUM(CASE WHEN p.win = true THEN 1 ELSE 0 END) as wins,
        AVG(p.kills) as avgKills,
        AVG(p.deaths) as avgDeaths,
        AVG(p.assists) as avgAssists,
        AVG(p.totalMinionsKilled) as avgMinions
    )
    FROM ParticipantEntity p
    WHERE p.puuid = :puuid
    GROUP BY p.championId
    """)
    List<ChampionStatsDto> findChampionStatsByPlayer(@Param("puuid") String puuid);

    @Query("""
    SELECT new com.Alex.RiotTrackerApplication.model.dto.MatchHistoryDto(
        p.match.matchId,
        p.match.gameMode,
        p.match.gameDuration,
        p.match.gameCreation,
        p.championId,
        p.win,
        p.kills,
        p.deaths,
        p.assists,
        p.totalMinionsKilled,
        p.goldEarned
        )
        FROM ParticipantEntity p
        WHERE p.puuid = :puuid
        ORDER BY p.match.gameCreation DESC
    """)
    Page<MatchHistoryDto> findMatchHistoryByPlayer(@Param("puuid") String puuid, Pageable pageable);

    @Query("""
        SELECT new com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto(
            p.championId,
            COUNT(p),
            SUM(CASE WHEN p.win = true THEN 1 ELSE 0 END),
            AVG(p.kills),
            AVG(p.deaths),
            AVG(p.assists),
            AVG(p.totalMinionsKilled)
        )
        FROM ParticipantEntity p
        WHERE p.puuid = :puuid
          AND p.match.queueId IN (420, 440)
        GROUP BY p.championId
    """)
    List<ChampionStatsDto> findRankedChampionStatsByPlayer(String puuid);
}
