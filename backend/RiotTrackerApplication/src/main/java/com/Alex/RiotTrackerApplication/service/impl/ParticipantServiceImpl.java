package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import com.Alex.RiotTrackerApplication.model.dto.ChampionStatsDto;
import com.Alex.RiotTrackerApplication.model.dto.MatchHistoryDto;
import com.Alex.RiotTrackerApplication.model.dto.SummonerStatsDto;
import com.Alex.RiotTrackerApplication.repository.ParticipantRepository;
import com.Alex.RiotTrackerApplication.repository.SummonerRepository;
import com.Alex.RiotTrackerApplication.service.ParticipantService;
import jakarta.servlet.http.Part;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class ParticipantServiceImpl implements ParticipantService {

   private final ParticipantRepository participantRepository;
   private final SummonerRepository summonerRepository;

   public ParticipantServiceImpl(ParticipantRepository participantRepository,SummonerRepository summonerRepository) {
       this.participantRepository = participantRepository;
       this.summonerRepository= summonerRepository;
   }

    @Override
    public List<ParticipantEntity> getParticipantsByPuuid(String puuid) {

        return participantRepository.findAll().stream()
                .filter(participant -> participant.getPuuid().equals(puuid))
                .collect(Collectors.toList());

    }

    @Override
    @Cacheable(value = "summonerStats", key="#puuid")
    public SummonerStatsDto getPlayerStatsSummary(String puuid) {

        List <ParticipantEntity> participants = getParticipantsByPuuid(puuid);



        int totalKills = participants.stream().mapToInt(ParticipantEntity::getKills).sum();
        int totalDeaths = participants.stream().mapToInt(ParticipantEntity::getDeaths).sum();
        int totalAssists = participants.stream().mapToInt(ParticipantEntity::getAssists).sum();
        long wins = participants.stream().filter(ParticipantEntity::getWin).count();
        int totalGames = participants.size();
        long losses = totalGames - wins;
        double winRate = totalGames == 0 ? 0 : (double) wins / totalGames;
        double avgGold= participants.stream().mapToDouble(ParticipantEntity::getGoldEarned).sum()/totalGames;
        double avgMinions= participants.stream().mapToDouble(ParticipantEntity::getTotalMinionsKilled).sum()/totalGames;
        String summonerName=summonerRepository.findById(puuid).get().getGameName();


        double kda = totalDeaths == 0 ? (double) (totalKills + totalAssists)
                : (double) (totalKills + totalAssists) / totalDeaths;



        return SummonerStatsDto.builder()
                .puuid(puuid)
                .totalGames(totalGames)
                .wins(wins)
                .avgGold(avgGold)
                .avgMinions(avgMinions)
                .summonerName(summonerName)
                .losses(losses)
                .winRate(winRate)
                .totalKills(totalKills)
                .totalDeaths(totalDeaths)
                .totalAssists(totalAssists)
                .kda(kda)

                .build();
    }

    @Override
    @Cacheable(value = "championStats:player", key = "#puuid")
    public List<ChampionStatsDto> findChampionStatsByPlayer(String puuid) {
        List<ChampionStatsDto> champions= participantRepository.findChampionStatsByPlayer(puuid);
        champions.sort(Comparator.comparing(ChampionStatsDto::getTotalMatches).reversed());
        return champions;
    }

    @Override
    @Cacheable(value = "championStats:player", key = "#puuid + ':champion:' + #championId")
    public ChampionStatsDto getChampionStatsSummary(String puuid,long championId) {

        List<ChampionStatsDto> championStats = participantRepository.findChampionStatsByPlayer(puuid);
        return championStats.stream().filter(dto -> dto.getChampionId()==championId).findFirst().orElse(null);
    }

    @Override
    public Page<MatchHistoryDto> getMatchHistory(String puuid, Pageable pageable){
       return participantRepository.findMatchHistoryByPlayer(puuid,pageable);
    }

    @Override
    public List<ChampionStatsDto> findRankedChampionStatsByPlayer(String puuid) {
        return participantRepository.findRankedChampionStatsByPlayer(puuid);
    }
}
