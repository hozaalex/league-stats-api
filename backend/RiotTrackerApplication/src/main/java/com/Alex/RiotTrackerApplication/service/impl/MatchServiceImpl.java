package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.model.MatchEntity;
import com.Alex.RiotTrackerApplication.repository.MatchRepository;
import com.Alex.RiotTrackerApplication.service.MatchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service

public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;

    public MatchServiceImpl(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    public Optional<MatchEntity> getMatchById(String matchId) {
        return matchRepository.findById(matchId);
    }

    @Override
    public List<MatchEntity> getAllMatches() {
        return StreamSupport.stream(matchRepository.findAll().spliterator(),false).collect(Collectors.toList());
    }
}
