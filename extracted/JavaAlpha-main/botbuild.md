# JAVA DISCORD BOT BUILD — PHASED PROMPT STRUCTURE FOR REPLIT AI

## OBJECTIVE
Build the most advanced multi-guild, multi-server Deadside-themed Discord bot possible in Java using JDA 5.x (Java Discord API) (Java-compatible command logic API or equivalent via JDA). The bot must support SFTP integration, CSV and log parsing, real-time statistics, a premium system, factions, economy, and dynamic Discord features—all following modern architecture and UI/UX standards.

---

## GLOBAL BOT FEATURES & DESIGN TARGETS

- **Multi-Guild Support**: 100+ guilds, each supporting multiple servers.
- **Premium System**: Per-server premium gates. Only killfeed is free. Tip4serv integrated.
- **SFTP Integration**: Pull server logs from user-provided servers. Auto-directory logic.
- **Log & CSV Parsing**:
  - Killfeed (every 5 min)
  - Historical data on demand or on server creation
  - Real-time `Deadside.log` monitoring (joins, events, disconnections)
- **Stat Engine**:
  - Player-based and weapon-based stats
  - Leaderboards and personal stats
  - Discord↔Game player linking with support for alts
- **Factions System**: Player-created factions, stat aggregation per faction, faction leaderboards.
- **Economy**: Currency rewards for time and kills, admin-modifiable, gambling system.
- **Theme**: Emerald-styled, Deadside-themed embeds using latest JDA/JDA formatting.

---

## PHASE 0 — PROJECT EXTRACTION & ENV SETUP

1. Unzip the provided `.zip` archive
2. Move contents into project root
3. Install dependencies (Maven or Gradle)
4. Prepare `.env` or `config.properties`
5. Log detected structure: main class, modules, SFTP tools, command handlers

---

## PHASE 1 — CORE BOT STRUCTURE

- Build bot entrypoint (main class)
- Initialize event system, slash command registry
- Register global command parsing logic
- Register cog-equivalent modular command routers
- Verify connection to Discord gateway
- Add home guild & owner identification

---

## PHASE 2 — DATABASE & SFTP INTEGRATION

- Connect to MongoDB or preferred NoSQL database
- Load or create server state models (guild, user, player, faction, economy)
- Build SFTP connector using user-supplied credentials
- Logic to navigate structured SFTP logs by Host_SERVER folder naming convention

---

## PHASE 3 — CSV PARSERS

- Build killfeed CSV parser (run every 5 minutes)
- Build historical CSV parser (on server add or command)
- Logic to:
  - Scan `actual1/deathlogs/**` for all `.csv` files
  - Track last-parsed line per parser
  - Handle line-by-line data conversion and stat storage
- Stats: kills, deaths, K/D, weapons, suicides, most killed/killer
- Ensure multi-server and multi-guild safety

---

## PHASE 4 — LOG PARSER

- Build `Deadside.log` parser to:
  - Detect new log rollover
  - Track joins, leaves, queues, events
- Post embeds to Discord channels with Deadside theme
- Maintain last-line index and rollover strategy
- Event detection for:
  - Missions
  - Airdrops
  - Trader Events
  - Helicrashes

---

## PHASE 5 — STATS SYSTEM

- Track:
  - Kills/Deaths/KD ratio
  - Most-used weapon
  - Player matchups
  - Weapon leaderboards
- Embed personal stats and global/faction leaderboards

---

## PHASE 6 — PLAYER LINKING & FACTIONS

- Link Discord user to main/alt in-game accounts
- Sync all stats under linked profile
- Faction system:
  - Create, join, leave factions
  - Aggregate faction stats
  - Faction leaderboards
  - Admin tools for faction management

---

## PHASE 7 — ECONOMY SYSTEM

- Currency tracking per Discord user
- Reward logic:
  - Kills
  - Online time
  - Admin commands to give/remove
- Gambling:
  - Slots, blackjack, roulette
  - Daily/work rewards (customizable)
- Admin control panel for economy balance and limits

---

## PHASE 8 — PREMIUM SYSTEM & GUILD ISOLATION

- Tip4serv integration
- Premium status toggle (per-server only by home guild admin)
- Gate advanced features:
  - Economy
  - Stats
  - Leaderboards
  - Faction and log/event processing
- Ensure full guild/server state isolation

---

## PHASE 9 — UI/EMBED POLISHING

- Theme all embeds:
  - Emerald Deadside color palette
  - Right-aligned icons/thumbnails to suppress transparent background highlight
- Use JDA 5.x (Java Discord API) advanced features (modals, views, selects)

---

## CHECKPOINT MINIMIZATION STRATEGY (REPLIT OPTIMIZED)

- Group related code into single-phase output
- Delay writing files until phase completion
- Use dry-run summaries before full commits
- Use file tagging to support resume logic
- Output must contain summary + code + file change confirmation block

---

## PHASE RECOVERY LOGIC

- If interrupted, reinitialize by reading phase marker tags:
  - `.phase_complete` for each phase
- Resume from next untagged phase
- Always log active and completed phases to console

---

If at any point system constraints are violated, return:
`CONSTRAINT ESCALATION REQUIRED — BUILD ABORTED`