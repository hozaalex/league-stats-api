# League of Legends Stats Tracker

A full-stack web application that tracks and visualizes **League of Legends** player statistics using Riot Games API.

## Features
- Fetches player profile, level, and region.
- Displays ranked stats, division emblems, and win rates.
- Shows champion performance with KDA, matches played, and win rate.
- Lists recent matches with game duration, KDA, and queue type.
- Interval-based data fetching (30-minute refresh) with caching and error handling.
- Frontend: React.js | Backend: Java Spring Boot
- Database: PostgreSQL for persistent match and player data.

## Tech Stack
- **Backend:** Java, Spring Boot, WebFlux, Mono, PostgreSQL
- **Frontend:** React.js, Axios, CSS
- **External APIs:** Riot Games API, CommunityDragon assets
- **Tools:** Docker, Maven, Git

## Setup
1. Clone the repository: `git clone https://github.com/hozaalex/league-stats-api`
2. Configure Riot API key in `application.properties`
3. Setup database properties in `application.properties`
4. Run the backend (either from your IDE or by using Maven).
5. Run frontend: `npm install && npm run dev`
6. Navigate to `http://localhost:5173/`

## Contribution
This is a personal project for showcasing full-stack skills, API integration, and data visualization for competitive gaming statistics.
