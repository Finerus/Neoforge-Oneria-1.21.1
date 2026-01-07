# Oneria Mod

**Oneria Mod** is a comprehensive utility mod designed specifically for immersive Roleplay (RP) servers. It provides advanced proximity-based obfuscation, automated schedule management, staff moderation tools, and seamless LuckPerms integration to create a professional and controlled RP environment.

---

## Key Features

### Roleplay Obfuscation (Proximity Blur System)

Create true immersion by hiding player identities based on distance:

* **Proximity-Based Names:** Player names in the TabList and overhead become obfuscated when they are beyond a configurable distance.
* **Anti-Metagaming:** Prevents players from knowing someone's name without meeting them in-game.
* **LuckPerms Prefix Support:** Option to hide or show rank prefixes based on distance.
* **Whitelist System:** Specific players or staff members can bypass the blur system entirely.
* **Nickname Support:** Full integration with custom nicknames set via `/oneria nick`.
* **Highly Configurable:** Adjust obfuscation distance, length, and behavior.

### Advanced Schedule System

Automated server management with smart opening and closing:

* **Automated Opening/Closing:** Define specific hours (HH:MM format) when the server is accessible.
* **Smart Kick System:** Automatically kicks non-staff players when the server closes.
* **Countdown Warnings:** Customizable warnings at 45, 30, 10, and 1 minute before closing.
* **Staff Bypass:** Staff members (defined by tags, OP level, or LuckPerms groups) can join even when closed.
* **Real-Time Status:** Players can check server status with `/schedule` or `/horaires`.

### Staff & Moderation Tools

Professional moderation tools with stealth and transparency:

* **Silent Commands:** Execute gamemode changes, teleports, and effects without broadcasting to the whole server.
* **Staff Logging:** Every staff action is logged to the console and broadcasted to other online staff members.
* **Teleport Platforms:** Define custom teleportation points (platforms) to move players quickly to specific RP zones.
* **Permission System:** Multi-layered permission detection (Scoreboard tags, OP levels, LuckPerms groups).
* **Nickname Management:** Set custom nicknames for players with full color code support.

### Welcome & Integration

Enhance player experience from the moment they join:

* **Custom Welcome Messages:** Display multi-line welcome messages with color codes and variable support.
* **Sound Effects:** Play specific sounds when a player joins (fully customizable).
* **LuckPerms Ready:** Deep integration with LuckPerms for permissions, prefixes, and group management.
* **Schedule Integration:** Welcome messages include server status and time remaining.

### Data Persistence

Smart data management for reliability:

* **Nickname Storage:** All nicknames are automatically saved to `world/data/oneriamod/nicknames.json`.
* **Auto-Save:** Every nickname change is instantly persisted to disk.
* **JSON Format:** Human-readable format allows manual editing if needed.
* **UUID-Based:** Nicknames tied to UUIDs, not usernames, for reliability.
* **Lazy Loading:** Efficient initialization only when needed.

---

## Commands

### Configuration Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria config reload` | OP Level 2 | Reloads the configuration file and clears all caches. |
| `/oneria config status` | OP Level 2 | Displays the current status of all mod systems. |
| `/oneria config set <option> <value>` | OP Level 2 | Modify any configuration option in real-time. |

### Staff Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria staff gamemode <mode> [target]` | Staff | Silently changes gamemode (survival, creative, adventure, spectator). |
| `/oneria staff tp <target>` | Staff | Silently teleports to a player. |
| `/oneria staff effect <target> <effect> <duration> <amplifier>` | Staff | Silently applies an effect to a player. |
| `/oneria staff platform [target] [id]` | Staff | Teleports player(s) to a predefined platform. |

### Whitelist Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria whitelist add <player>` | OP Level 2 | Adds a player to the blur bypass whitelist. |
| `/oneria whitelist remove <player>` | OP Level 2 | Removes a player from the whitelist. |
| `/oneria whitelist list` | OP Level 2 | Lists all whitelisted players. |

### Nickname Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria nick <player> <nickname>` | OP Level 2 | Sets a custom nickname (supports § and & color codes). |
| `/oneria nick <player>` | OP Level 2 | Resets nickname to the original name. |
| `/oneria nick list` | OP Level 2 | Displays all active nicknames with a formatted list. |

### Public Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria schedule` or `/schedule` or `/horaires` | Everyone | Displays server opening/closing times and status. |

### Convenient Aliases

| Alias | Equivalent Command | Description |
|:------|:-------------------|:------------|
| `/platform [target] [id]` | `/oneria staff platform [target] [id]` | Quick access to platform teleportation for staff. |
| `/schedule` | `/oneria schedule` | Quick access to server schedule information. |
| `/horaires` | `/oneria schedule` | French alias for schedule command. |

---

## ⚙Configuration

The configuration file is located at `serverconfig/oneriamod-server.toml`. It's fully documented and organized into logical sections:

### Configuration Sections

#### Obfuscation Settings
* `proximityDistance` - Distance (in blocks) before names are obfuscated (default: 8).
* `enableBlur` - Master switch for the obfuscation system.
* `obfuscatedNameLength` - Number of characters in the obfuscated name (default: 5).
* `obfuscatePrefix` - Whether to hide rank prefixes when obfuscating.
* `opsSeeAll` - Operators always see all names clearly.
* `debugSelfBlur` - Enable self-blur for testing purposes.
* `whitelist` - List of players who bypass the blur system.

#### Permissions System
* `staffTags` - Scoreboard tags considered as staff (default: `["admin", "modo", "staff", "builder"]`).
* `opLevelBypass` - Minimum OP level to be considered staff (default: 2).
* `useLuckPermsGroups` - Enable LuckPerms group detection.
* `luckPermsStaffGroups` - LuckPerms groups considered as staff.

#### Schedule System
* `enableSchedule` - Enable automated opening/closing.
* `openingTime` - Server opening time (format: HH:MM, e.g., "19:00").
* `closingTime` - Server closing time (format: HH:MM, e.g., "23:59").
* `warningTimes` - Minutes before closing to send warnings (default: [45, 30, 10, 1]).
* `kickNonStaff` - Automatically kick non-staff at closing time.

#### Messages
* `serverClosedMessage` - Message displayed when kicking non-staff.
* `serverOpenedMessage` - Message sent to staff when server opens.
* `warningMessage` - Warning message template (`{minutes}` replaced automatically).
* `closingImminentMessage` - Final warning 1 minute before closing.

#### Welcome Message
* `enableWelcome` - Display welcome message on login.
* `welcomeLines` - Multi-line message (supports `{player}` and `{nickname}` variables).
* `welcomeSound` - Sound to play on login (e.g., "minecraft:entity.player.levelup").
* `welcomeSoundVolume` - Sound volume (0.0 to 1.0).
* `welcomeSoundPitch` - Sound pitch (0.5 to 2.0).

#### Teleportation Platforms
* `enablePlatforms` - Enable platform teleportation system.
* `platforms` - List of platforms (format: `id;DisplayName;dimension;x;y;z`).

#### Silent Commands
* `enableSilentCommands` - Enable staff silent command system.
* `logToStaff` - Notify other staff members when commands are used.
* `logToConsole` - Log silent commands to the server console.
* `notifyTarget` - Notify the target player (default: false for stealth).

---

## Installation

### Requirements
* **Minecraft:** 1.21.1
* **NeoForge:** 21.1.215 or higher
* **Optional:** LuckPerms (for advanced permissions and prefix support)

### Steps
1. Download the latest `oneriamod-X.X.X.jar` from the releases page.
2. Place the JAR file into your server's `mods` folder.
3. **(Optional)** Install [LuckPerms](https://luckperms.net/) for advanced permission management.
4. Start your server to generate the configuration file.
5. Edit `serverconfig/oneriamod-server.toml` to customize your settings.
6. Reload with `/oneria config reload` or restart the server.

---

## Technical Details

### Performance
* **Permission Caching:** Results cached for 30 seconds to minimize overhead.
* **Tick Optimization:** Schedule system checks only every 20 seconds (400 ticks).
* **Efficient Packet Handling:** Minimal performance impact on TabList updates.

### Compatibility
* **Server-Side Only:** No client-side installation required.
* **Mixin-Based:** Uses Mixin for clean packet interception.
* **API Ready:** Public API available for other mods (`OneriaPermissions.isStaff()`, etc.).

---

## License

This mod is developed for the **Oneria RP Server** and is provided as-is for community use. You are free to use it on your own server, but official support and updates are prioritized for the Oneria server.

**All Rights Reserved © 2026 Oneria Team**

---

## Credits

* **Development:** Finerus, OneriaTeam
* **Special Thanks:** The Oneria RP community for testing and feedback
* **Libraries:** NeoForge, Mixin, LuckPerms API

---

## Support

For issues, suggestions, or questions:
* **Discord:** Join the Oneria Discord server
* **GitHub Issues:** Report bugs on our repository

**Note:** This mod is actively maintained for the Oneria server. Community contributions and feedback are welcome!