# Deadside Discord Bot - Build Documentation

## Overview
This document tracks the progress of our Deadside Discord bot development, focusing on enhancements to the log parser for better event detection and notification.

## Completed Features

### Player Tracking
- ✅ Improved detection of player join/leave events using updated log patterns
- ✅ Fixed player count tracking to accurately show online players
- ✅ Added timeout detection for players who disconnect abnormally

### Event Notifications
- ✅ Modified airdrop notifications to only trigger when in "Flying" state (initial state)
- ✅ Updated mission reporting to only show high-level (level 3+) missions when they become ACTIVE
- ✅ Added detection for helicopter crash events via GameplayEvent pattern
- ✅ Added roaming trader event detection via GameplayEvent pattern
- ✅ Suppressed encounter event notifications as they're too numerous
- ✅ Enhanced mission name normalization for better readability

### CSV Parsing
- ✅ Killfeed CSV parser fully operational (runs every 5 minutes)
- ✅ Historical CSV parser implemented for stat analysis
- ✅ Player stats tracking working correctly (kills, deaths, K/D)
- ✅ Weapon-specific statistics tracking
- ✅ Player matchup tracking (most killed/most killed by)

### Event Detection
- ✅ Added new pattern matching for GameplayEvents
- ✅ Created specific handler for helicopter crash events
- ✅ Implemented detection of Level 3+ missions
- ✅ Added efficient state tracking to prevent duplicate notifications

## In Progress / To Do

### Further Enhancements
- ⏳ Add convoy event detection and notification
- ⏳ Test all event types with live server data
- ⏳ Fine-tune event notification formatting

### Quality of Life
- ⏳ Add more detailed debugging and logging for server operators
- ⏳ Create command to test log parser against sample logs
- ⏳ Implement customizable notification settings

## Pattern Reference

For developers maintaining this codebase, here are the current regex patterns used:

```java
// Player Events
PLAYER_JOIN_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully registered!");
PLAYER_LEAVE_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully unregistered from the session.");

// In-Game Events
AIRDROP_PATTERN = Pattern.compile("LogSFPS: AirDrop switched to (\\w+)");
MISSION_PATTERN = Pattern.compile("LogSFPS: Mission (.+?) switched to (\\w+)");
GAMEPLAY_EVENT_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (.+?) switched to (\\w+)");
HELICRASH_EVENT_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (HelicrashManager.+?)HelicrashEvent.+? switched to (\\w+)");
ROAMING_TRADER_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (RoamingTraderManager.+?)RoamingTraderEvent.+? switched to (\\w+)");
```

## Event Notification Strategy

As requested, we've implemented these notification rules:
1. For airdrops: Only notify when they first appear (Flying state)
2. For missions: Only notify when high-level missions (3+) become ACTIVE
3. For special events: Only notify when they become ACTIVE or STARTED
4. Encounters are excluded from notifications due to their frequency

## Testing Data

Our testing shows the following event frequencies in a typical log file:
- Airdrops: 66 flying events, 167 total airdrop state changes
- Missions: 167 active missions (47 completed)
- Gameplay Events: 149 total events
- Player Traffic: 174 joins, 167 leaves
- Special Events: 4 roaming trader events, multiple helicopter crashes