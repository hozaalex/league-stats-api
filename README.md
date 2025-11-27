# League of Legends Stats Tracker

A production-grade full-stack application providing **League of Legends** player analytics with a cache-first reactive architecture, asynchronous processing, and intelligent rate limiting. This platform delivers fast, reliable insights into player profiles, ranked statistics, champion performance, and match history.

---

##  Version Release 2.0

This release introduces the core full-stack architecture with advanced performance features. It includes:

- Cache-first design using PostgreSQL to eliminate redundant API calls (10-minute TTL).  
- Asynchronous processing with **Kafka** for decoupling API requests from data handling.  
- Dual-layer rate limiting:
  - **Bucket4j** token bucket for Riot API compliance (20 req/s + 100 req/2min).  
  - Custom sliding window (20 req/min) for user IP protection.  
- Non-blocking I/O using Spring WebFlux + Project Reactor.  
- Redis for real-time request status tracking with TTL-based cleanup.  
- Scheduled Spring jobs (`@Scheduled`) to maintain cache freshness.


---

## Features

- **Player Profiles:** Summoner info, level, region, and profile icons.  
- **Ranked Statistics:** Division emblems, LP, and win rates across all queues.  
- **Champion Analytics:** Per-champion KDA, matches played, and win rate trends.  
- **Match History:** Recent games with detailed stats.  
- **Cache-First Updates with WebFlux Streaming:** Player data is cached for 10 minutes for fast responses. Requests for cached players return instantly, while new players trigger an asynchronous fetch. Data is refreshed automatically using a scheduled task (@Scheduled) so the frontend receives up-to-date results.

---

## Tech Stack

**Backend:**  
Java 17, Spring Boot, Spring WebFlux, Spring Data JPA, Kafka (message queuing), Redis (request tracking), PostgreSQL (persistent storage), Bucket4j (rate limiting), Project Reactor (reactive streams)  

**Frontend:**  
React.js, Axios, CSS3 — responsive design, loading states, error handling  

**External APIs:**  
Riot Games API (player data, matches, ranked stats), CommunityDragon (champion assets, profile icons)  

**DevOps:**  
Docker, Docker Compose, Maven, Git  

---

## Architecture Flow

```
User Request → Controller → Cache Check (PostgreSQL)
    ↓ (if stale/missing)
Kafka Producer → Message Queue → Kafka Consumer
    ↓
Riot API (rate limited) → Parse & Store → Redis Status Update
    ↓
Frontend Polls Status → Returns Complete Data
```

---

## Setup

1. Clone the repository: `git clone https://github.com/hozaalex/league-stats-api/tree/updated-version-1`
2. Configure Riot API key in `application.properties`
3. Setup database properties in `application.properties`
4. Start required services with Docker (PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI): `docker-compose up`
5. Run backend: `./mvnw verify`
6. Run frontend: `npm install && npm run dev`


