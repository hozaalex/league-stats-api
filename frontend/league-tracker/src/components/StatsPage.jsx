import React, { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import axios from "axios";
import Navbar from "./Navbar";
import "./StatsPage.css";

function StatsPage() {
  const location = useLocation();
  const { gameName, tag, region } = location.state || {};


  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState(null);
  const [summoner, setSummoner] = useState(null);
  const [overallStats, setOverallStats] = useState(null);
  const [rankedProfile, setRankedProfile] = useState(null);
  const [champions, setChampions] = useState([]);
  const [matches, setMatches] = useState([]);

  useEffect(() => {
    if (!gameName || !tag || !region) {
      console.log("Missing params:", { gameName, tag, region });
      return;
    }

    let isMounted = true;
    let hasStarted = false;

    async function fetchData() {
      if (hasStarted) {
        console.log("Fetch already in progress, skipping...");
        return;
      }

      hasStarted = true;
      console.log("Starting fetch...");
      setLoading(true);
      setError(null);
      setStatus("Submitting request...");

      try {

        console.log("Sending request:", { gameName, tagLine: tag, region });

        const trackResponse = await axios.post(
          "http://localhost:8080/api/v1/summoners/track",
          {
            gameName,
            tagLine: tag,
            region,
          }
        );

        console.log("Track response:", trackResponse.data);

        const responseData = trackResponse.data;
        if (!isMounted) return;

        if (responseData.status === "COMPLETED" && responseData.data) {
          console.log("Received cached data immediately!");

          setSummoner(responseData.data.summoner ?? null);
          setOverallStats(responseData.data.overallStats ?? null);
          setRankedProfile(responseData.data.rankedProfile ?? null);
          setChampions(responseData.data.overallChampionStats ?? []);
          setMatches(responseData.data.recentMatches ?? []);

          setStatus("Completed!");
          setLoading(false);
          return; 
        }

        const { requestId } = trackResponse.data;

        if (!requestId) {
          throw new Error("No request ID received");
        }

        setStatus("Processing... Please wait");


        const result = await pollStatus(requestId, isMounted);

        if (!isMounted) return;

        if (result) {
          console.log("Received data:", result);

          setSummoner(result.summoner ?? null);
          setOverallStats(result.overallStats ?? null);
          setRankedProfile(result.rankedProfile ?? null);
          setChampions(result.overallChampionStats ?? []);
          setMatches(result.recentMatches ?? []);

          setStatus("Completed!");
          console.log("State set successfully");
        } else {
          throw new Error("Failed to fetch summoner data");
        }
      } catch (err) {
        console.error("Error:", err);
        console.error("Error response:", err?.response);

        if (isMounted) {
          if (err?.response?.status === 429) {
            setError("Rate limit exceeded. Please try again later.");
          } else if (err?.response?.status === 404) {
            setError("Summoner not found");
          } else {
            setError(
              err?.response?.data?.message ||
              err?.response?.data?.error ||
              err?.message ||
              "Failed to fetch data"
            );
          }
        }
      } finally {
        console.log("Loading complete");
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    async function pollStatus(requestId, isMounted) {
      const maxAttempts = 60;
      let attempts = 0;

      while (attempts < maxAttempts) {
        if (!isMounted) {
          console.log("Component unmounted, stopping poll");
          return null;
        }

        try {
          console.log(`Polling attempt ${attempts + 1}/${maxAttempts}`);

          const statusResponse = await axios.get(
            `http://localhost:8080/api/v1/summoners/status/${requestId}`
          );

          const statusData = statusResponse.data;
          console.log("Status data:", statusData);

          if (statusData.status === "COMPLETED") {
            console.log("Request completed!");
            return statusData.data;
          } else if (statusData.status === "FAILED") {
            throw new Error(statusData.error || "Request failed");
          } else if (statusData.status === "PROCESSING") {

            setStatus(`Processing... (${attempts + 1}/${maxAttempts})`);
            await new Promise((resolve) => setTimeout(resolve, 2000));
            attempts++;
          }
        } catch (err) {
          if (err?.response?.status === 404) {
            console.log("Request not found, retrying...");
            await new Promise((resolve) => setTimeout(resolve, 2000));
            attempts++;
          } else {
            throw err;
          }
        }
      }

      throw new Error("Request timed out after 2 minutes");
    }

    fetchData();

    return () => {
      isMounted = false;
    };
  }, [gameName, tag, region]);

  console.log("Current state:", {
    summoner,
    overallStats,
    rankedProfile,
    champions: champions?.length,
    matches: matches?.length,
    loading,
    error,
  });

  return (
    <div className="stats-page">
      {loading && (
        <div className="loading">
          <p>Loading summoner data...</p>
          {status && <p className="status">{status}</p>}
        </div>
      )}

      {error && <div className="error">Error: {error}</div>}

      {!loading && !error && !summoner && (
        <div className="no-data">No summoner data available</div>
      )}

      {summoner && (
        <div className="stats-container">
          {/* LEFT PANEL */}
          <aside className="left-panel">
            <div className="profile-card">
              <img
                src={`http://ddragon.leagueoflegends.com/cdn/15.22.1/img/profileicon/${summoner.profileIconId}.png`}
                alt={`${summoner.gameName} profile icon`}
                className="profile-icon"
              />
              <h2>
                {summoner.gameName}#{summoner.tagLine}
              </h2>
              <p>Level {summoner.summonerLevel ?? "-"}</p>
              <p className="muted">Region: {summoner.region}</p>
            </div>

            {/* Overall Stats */}
            {overallStats && (
              <div className="overall-stats-card">
                <h3>Overall Stats</h3>
                <p>Total Games: {overallStats.totalGames ?? 0}</p>
                <p>Wins: {overallStats.wins ?? 0}</p>
                <p>Losses: {overallStats.losses ?? 0}</p>
                <p>
                  Win Rate:{" "}
                  {(Number(overallStats.winRate || 0) * 100).toFixed(1)}%
                </p>
                <p>Avg KDA: {Number(overallStats.kda || 0).toFixed(2)}</p>
                <p>Avg Gold: {Number(overallStats.avgGold || 0).toFixed(0)}</p>
                <p>Avg CS: {Number(overallStats.avgMinions || 0).toFixed(1)}</p>
              </div>
            )}

            {/* Ranked Profile */}
            {rankedProfile && (
              <div className="ranked-card">
                <h3>Ranked Stats</h3>

                {/* Ranked queues */}
                {rankedProfile.rankedStatsDto && rankedProfile.rankedStatsDto.length > 0 ? (
                  rankedProfile.rankedStatsDto.map((queue, index) => (
                    <div key={index} className="rank-item">
                      <p>
                        <strong>{queue.queueType}</strong>
                      </p>

                      <img
                        src={`${queue.tier.toUpperCase()}.png`}
                        alt={queue.tier}
                        className="tier-icon"
                      />

                      <p>
                        {queue.tier} {queue.rank} – {queue.leaguePoints} LP
                      </p>
                      <p>Wins: {queue.wins}</p>
                      <p>Loses: {queue.losses}</p>
                      <p>
                        Win Rate:{" "}
                        {queue.losses + queue.wins > 0
                          ? ((queue.wins / (queue.losses + queue.wins)) * 100).toFixed(2)
                          : "0.00"}
                        %
                      </p>
                    </div>
                  ))
                ) : (
                  <div className="rank-item">
                    <img src="UNRANKED.png" className="tier-icon" alt="Unranked" />
                    <p>Unranked</p>
                  </div>
                )}

                {/* Ranked champion performance */}
                {rankedProfile.championPerformance?.length > 0 ? (
                  rankedProfile.championPerformance.map((champ) => (
                    <div key={champ.championId} className="champ-card">
                      <p>
                        <strong>{champ.championName}</strong>
                      </p>
                      <p className="muted">
                        {champ.totalMatches} games — {champ.winRate} Winrate
                      </p>
                      <p className="muted">
                        Kills: {champ.avgKills} Deaths: {champ.avgDeaths}{" "}
                        Assists: {champ.avgAssists}
                      </p>
                      <p>{champ.kda.toFixed(2)} KDA</p>
                    </div>
                  ))
                ) : (
                  <p className="muted">No ranked champion stats</p>
                )}
              </div>
            )}
          </aside>

          {/* CENTER PANEL */}
          <main className="center-panel">
            <h3>Recent Matches</h3>

            {matches.length === 0 ? (
              <p className="muted">No recent matches</p>
            ) : (
              matches.map((match, index) => (
                <div
                  key={match.matchId ?? index}
                  className={`match-card ${match.win ? "win" : "loss"}`}
                >
                  <p>
                    <strong>{match.championName ?? "Unknown"}</strong> —{" "}
                    {match.kills ?? 0}/{match.deaths ?? 0}/{match.assists ?? 0}
                  </p>
                  <p>
                    KDA:{" "}
                    {Number(
                      match.kda ||
                      (match.kills + match.assists) / (match.deaths || 1)
                    ).toFixed(2)}
                  </p>
                  <p className="muted">{match.queueType ?? "Normal"}</p>
                  <p className="muted">
                    Date:{" "}
                    {match.gameCreation
                      ? new Date(match.gameCreation).toLocaleString()
                      : "-"}
                  </p>
                  <p className="muted">
                    Duration:{" "}
                    {match.gameDuration
                      ? `${Math.floor(match.gameDuration / 60)}m ${match.gameDuration % 60
                      }s`
                      : "-"}
                  </p>
                </div>
              ))
            )}
          </main>

          {/* RIGHT PANEL */}
          <aside className="right-panel">
            <h3>Top Champions</h3>
            {champions.length > 0 ? (
              champions.slice(0, 5).map((champ, index) => (
                <div
                  key={champ.championId ?? champ.championName ?? index}
                  className="champ-card"
                >
                  <p>
                    <strong>{champ.championName ?? "Unknown"}</strong>
                  </p>
                  <p className="muted">
                    {champ.totalMatches ?? 0} games — {champ.winRate ?? "0%"}{" "}
                    Winrate
                  </p>
                  <p className="muted">
                    Kills: {Number(champ.avgKills || 0).toFixed(1)}
                    {"\u00A0"}Deaths: {Number(champ.avgDeaths || 0).toFixed(1)}
                    {"\u00A0"}Assists:{" "}
                    {Number(champ.avgAssists || 0).toFixed(1)}
                  </p>
                  <p>KDA: {Number(champ.kda || 0).toFixed(2)} </p>
                </div>
              ))
            ) : (
              <p className="muted">No champion data</p>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

export default StatsPage;