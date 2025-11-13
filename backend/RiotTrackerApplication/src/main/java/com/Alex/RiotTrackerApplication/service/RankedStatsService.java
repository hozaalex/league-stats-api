package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.dto.RankProfileDto;

public interface RankedStatsService {

    RankProfileDto getRankProfile(String puuid);

}
