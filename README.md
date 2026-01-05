# Oneria Mod

Oneria Mod is a comprehensive utility mod designed specifically for Roleplay (RP) servers. It provides advanced immersion tools, server management features, and staff utilities to create a professional and controlled environment.

## Key Features

### Roleplay Obfuscation (Blur System)

* Proximity-based Names: Player names in the TabList and overhead become blurred (obfuscated) when they are too far away.
* Immersion: Enhances RP by preventing metagaming (knowing someone's name without meeting them).
* LuckPerms Support: Option to hide or show prefixes based on distance.
* Whitelist: Specific players or staff members can be whitelisted to always see names clearly.

### Advanced Schedule System

* Automated Opening: Define specific hours (HH:mm) when the server is accessible to players.
* Smart Kick: Automatically kicks non-staff players when the server closes, with customizable countdown warnings.
* Staff Bypass: Staff members (defined by tags, OP level, or LuckPerms groups) can join even when the server is closed.

### Staff and Moderation Tools

* Silent Commands: Execute gamemode changes, teleports, and effects without broadcasting to the whole server.
* Staff Logging: Every staff action is logged to the console and broadcasted to other online staff members for transparency.
* Teleport Platforms: Define custom teleportation points (platforms) to move players quickly to specific RP zones.

### Welcome and Integration

* Custom Welcome: Display multi-line welcome messages and play specific sounds when a player joins.
* LuckPerms Ready: Deep integration with LuckPerms for permissions and prefix handling.

## Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| /oneria config reload | OP Level 2 | Reloads the configuration file and clears caches. |
| /oneria config status | OP Level 2 | Displays the current status of the blur and schedule systems. |
| /oneria whitelist <add/remove/list> | OP Level 2 | Manages the name-blur bypass list. |
| /oneria staff gamemode <mode> [player] | Staff | Silently changes gamemode. |
| /oneria staff tp <player> | Staff | Silently teleports to a player. |
| /oneria staff platform <player> [id] | Staff | Teleports a player to a predefined platform. |
| /schedule | Everyone | Displays the server opening/closing times and time left. |

## Configuration

The configuration file is located at serverconfig/oneriamod-server.toml. You can customize:

* Distances: Adjust how far the blur effect starts.
* Times: Set the openingTime and closingTime (e.g., 19:00 to 23:59).
* Permissions: Define which Scoreboard Tags or LuckPerms groups are considered Staff.
* Messages: Fully customize kick messages, warnings, and welcome texts.

## Installation

1. Ensure you are using the latest NeoForge for Minecraft 1.21.
2. Drop the oneriamod-1.0.1.jar into your mods folder.
3. (Optional) Install LuckPerms for advanced permission management.
4. Restart your server to generate the configuration file.

## License

This mod is private and intended for use on the Oneria RP server, you can still use it but it won't be updated unless we need it. All rights reserved.
