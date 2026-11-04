# Dragons of Mugloar

A full-stack take on the [Dragons of Mugloar](https://dragonsofmugloar.com/) game: a web app to play the game with an
**ad advisor**, and a **bot** that plays on its own and reliably scores well over 1,000 points.

- **Backend:** Java 21, Spring Boot 4 (REST + Server-Sent Events), Gradle
- **Frontend:** Vue 3, TypeScript, Pinia, Vite
- **Tests:** JUnit 5 / AssertJ / Mockito / MockMvc, and Vitest / Vue Test Utils

```
bigbank/
├── backend/    Spring Boot API, game strategy, bot (web + CLI)
├── frontend/   Vue single-page app
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Quick start

Requirements: **Java 21** and **Node 20+** (or just Docker).

```bash
# 1. backend on http://localhost:8080
cd backend && ./gradlew bootRun

# 2. frontend on http://localhost:5173 (proxies /api to the backend)
cd frontend && npm install && npm run dev
```

Open http://localhost:5173, press **Start a new game**, and either solve ads yourself or press **Let the bot play**.

### With Docker

```bash
docker compose up --build
```

The app is then served on http://localhost:8081 (nginx serves the frontend and proxies `/api` to the backend).

### Bot from the command line

Plays N games back to back and prints the score of each, plus min / avg / max:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=bot --bot.games=10'
```

Add `--logging.level.eu.bigbank.mugloar.cli=DEBUG` to see every move.

## Features

**Playing**
- Start a game, see score, gold, lives, level and turn at all times.
- Message board with every ad. Encrypted ads (Base64 / ROT13) are decoded on the server, so they can be solved
  like any other.
- Shop with all items. Items you can't afford are disabled.
- Reputation check, activity log, game-over dialog. The current game survives a page reload (saved in `localStorage`).

**Decision tools ("advisor")**
- Each ad shows an estimated **success chance**, **expected value** (chance × reward), a **risk level** and a
  success meter.
- The ad the bot would pick is marked **★ Advisor's pick**, with a one-click **Solve advisor's pick** button.
- Warnings on ads that are worse than they look ("Steal" jobs, see below).
- Sort by advice / expected value / reward / chance / expiry, filter by maximum risk, search by text.
- Shop hint to buy a healing potion when lives are low.
- **Auto-play:** the server-side bot takes over the current game and streams each move live to the UI, with a score
  chart. You can stop it at any point and continue by hand.

## How the bot plays

Code: `backend/src/main/java/eu/bigbank/mugloar/strategy/`.

Each turn:

1. **Shop** (`ShopPolicy`). If lives < 3 and a healing potion is affordable, buy it. Otherwise, if gold covers an
   upgrade *plus* a potion reserve, buy an upgrade, preferring items not bought yet. Upgrades raise the dragon's
   level, which makes the bigger-paying ads succeed more often.
2. **Pick an ad** (`AdEvaluator`). Each ad's label ("Piece of cake", "Gamble", ...) is mapped to an estimated success
   chance (`Probability`). Among ads with at least 75% chance (85% on the last life), take the highest expected value.
   Ties go to the ad that expires first.
3. **If nothing is safe enough**, buy a healing potion instead of gambling. That uses up a turn, so the board
   refreshes, and banks an extra life. Only when that's not affordable does it take the safest ad on the board.
4. Repeat until out of lives.

The estimates were tuned from logs of real games. The main finding: **"Steal ..." ads fail far more often than their
label says** (a "Sure thing" steal succeeded only ~55% of the time) and they hurt reputation. Their chance is
scaled by 0.6, which removed most early-game deaths.

Typical results (10 games in a row against the live API):

| min | average | max | games ≥ 1,000 |
|-----|---------|-----|---------------|
| 2,897 | 4,943 | 6,874 | 10 / 10 |

## Backend

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/games` | Start a game |
| `GET`  | `/api/games/{gameId}/ads` | Decoded ads with advice, best first |
| `POST` | `/api/games/{gameId}/ads/{adId}/solve` | Solve an ad |
| `GET`  | `/api/games/{gameId}/shop` | Shop items |
| `POST` | `/api/games/{gameId}/shop/{itemId}` | Buy an item |
| `POST` | `/api/games/{gameId}/reputation` | Investigate reputation |
| `GET`  | `/api/games/{gameId}/autoplay` | Bot plays the game; moves streamed as SSE (`started`, `purchased`, `solved`, `finished`, `failed`) |

Packages:

- `client`: `GameApi` port and the `RestClient` adapter. The game API returns HTML error pages, so the adapter
  turns them into `MugloarApiException` with the upstream status. Read calls are retried on 5xx and network
  errors. Writes are not retried, because they are not idempotent.
- `strategy`: pure game logic (decoder, evaluator, shop policy, bot). It has no Spring dependency, so it is easy
  to unit-test with a fake `GameApi`.
- `service`: use cases plus `GameRegistry`, an in-memory store of each game's last known state (the game API has
  no "get game" endpoint, and auto-play needs to know lives and gold). Updates are atomic. A game the server
  forgot, for example after a restart, is learned again from the next move's response.
- `api`: controllers, response DTOs and `ApiExceptionHandler`.
- `cli`: `BotRunner` for the `bot` profile.

**Validation and errors.** Path ids are validated (`^[A-Za-z0-9_-]{1,64}$`) before anything is sent upstream.
All errors are RFC 9457 `application/problem+json`:

| Situation | Status |
|-----------|--------|
| Invalid id | 400 |
| Game API rejected the request (e.g. expired ad) | 400 |
| Game / ad / item not found | 404 |
| Game is over | 410 |
| Auto-play already running for this game (also blocks manual moves meanwhile) | 409 |
| Too many bots running on the server (limit 10) | 503 |
| Anything unexpected (details logged, not exposed) | 500 |
| Game API down or timing out | 502 |

Configuration lives in `backend/src/main/resources/application.yml` (`mugloar.api.*` for base URL, timeouts and
retries, `mugloar.web.allowed-origins` for CORS, `mugloar.bot.max-turns`). Every value can be overridden with
environment variables, e.g. `MUGLOAR_API_BASE_URL`.

## Frontend

- `src/api`: typed `fetch` client. It parses problem responses into `ApiError`, maps network failures, and checks
  ids on the client too.
- `src/stores` (Pinia):
  - `game`: game state, ads, shop and reputation, with all actions and `localStorage` persistence.
  - `board`: sort, filter and search.
  - `autoplay`: the `EventSource` lifecycle. It stops instead of auto-reconnecting, so a dropped connection never
    starts a second bot.
  - `log`: activity history.
  - `notifications`: toasts.
- `src/components`: small presentational components that read from the stores.

**Responsive and cross-browser.** Plain CSS with grid and flexbox and no UI framework. A two-column desktop layout (board + side panels)
collapses to one column on phones. Light and dark themes follow the OS setting. Only standard web APIs are used
(`fetch`, `EventSource`, `localStorage`), and the build targets the last ~2 years of Chrome, Edge, Firefox and
Safari.

## Tests

```bash
cd backend && ./gradlew test        # strategy, HTTP client (MockRestServiceServer), controllers (MockMvc), service
cd frontend && npm test             # api client, stores, components
cd frontend && npm run typecheck && npm run lint
```

CI (GitHub Actions) runs both test suites, the linter and the frontend build on every push.

## Use of AI tools

I used Claude Code (an AI coding assistant) throughout this task. It generated most of the code, tests and
documentation, and analyzed bot logs to tune the strategy (for example, it found that "Steal" ads fail more often
than their labels suggest).

My part was directing and reviewing the work: choosing the stack and the overall approach (Spring Boot + Vue in one
repo, the bot running on the backend with live streaming to the UI), reviewing the code and asking for fixes,
checking the results against the live game, and deploying it to my own server.
