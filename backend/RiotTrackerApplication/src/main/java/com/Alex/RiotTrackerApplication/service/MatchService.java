package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.MatchEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

public interface MatchService {

    public Optional<MatchEntity> getMatchById(String matchId);
    public List<MatchEntity> getAllMatches();







}
