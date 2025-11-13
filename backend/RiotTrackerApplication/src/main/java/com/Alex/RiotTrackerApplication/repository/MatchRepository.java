package com.Alex.RiotTrackerApplication.repository;

import com.Alex.RiotTrackerApplication.model.MatchEntity;
import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity,String > {


}
