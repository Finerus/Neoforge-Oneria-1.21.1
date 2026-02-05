# Oneria Mod

**Oneria Mod** is a comprehensive utility mod designed specifically for immersive Roleplay (RP) servers. It provides a complete profession and license system with craft/mining/equipment restrictions, advanced proximity-based obfuscation, sneak stealth mechanics, automated schedule management, staff moderation tools, advanced chat formatting, world border warnings, custom join/leave messages, and seamless LuckPerms integration to create a professional and controlled RP environment.

## Key Features

### Profession & License System

Complete profession and restriction management system for roleplay economies and job systems:

* **Profession Definitions:** Define unlimited professions with custom names, colors, and permissions.
* **License Items:** Physical license items given to players with profession colors and metadata.
* **Restriction Types:**
    - **Crafting Restrictions:** Block specific items from being crafted
    - **Mining Restrictions:** Prevent breaking certain blocks and ores
    - **Item Usage Restrictions:** Block right-click interactions with specific items
    - **Equipment Restrictions:** Prevent equipping unauthorized armor and weapons
    - **Attack Restrictions:** Cancel damage from unauthorized weapons
* **Permission System:** Global restrictions with profession-specific overrides.
* **Wildcard Support:** Use patterns like `minecraft:*_pickaxe` to block entire item categories.
* **Persistent Storage:** Licenses stored in `world/data/oneriamod/licenses.json`.
* **Smart Autocomplete:** Commands suggest only relevant professions based on context.
* **Client Tooltips:** Items display required professions directly in their tooltips.
* **Action Bar Notifications:** Non-intrusive warnings shown in action bar instead of chat spam.
* **Whitelist Exemption:** Whitelisted players bypass all profession restrictions.
* **RP Licenses:** Temporary decorative licenses for roleplay events (no actual permissions).
* **Default Professions:** 8 example professions included (Hunter, Fisher, Miner, Lumberjack, Blacksmith, Alchemist, Merchant, Guard).

### Roleplay Obfuscation (Proximity Blur System)

Create true immersion by hiding player identities based on distance and stealth:

* **Proximity-Based Names:** Player names in the TabList and overhead become obfuscated when they are beyond a configurable distance.
* **Nametag Hiding:** Optional server-controlled hiding of all player nametags above heads (default: disabled).
* **Nickname Display:** Nicknames visible above player heads with optional LuckPerms prefix/suffix.
* **Sneak Stealth Mode:** Players who are crouching become significantly harder to detect (2 blocks vs 8 blocks by default).
* **Dynamic Distance Calculation:** System automatically adjusts detection range based on player crouch state.
* **Anti-Metagaming:** Prevents players from knowing someone's name without meeting them in-game.
* **LuckPerms Prefix Support:** Option to hide or show rank prefixes based on distance.
* **Whitelist System:** Specific players or staff members can bypass the blur system entirely.
* **Blacklist System:** Specific players can be permanently hidden regardless of distance (stealth staff, NPCs).
* **Always Visible List:** NEW! Force specific players to always appear in TabList (never blurred, even at distance).
* **Spectator Blur:** NEW! Automatically hide players in spectator mode from TabList (bypassed by always visible list).
* **Nickname Support:** Full integration with custom nicknames set via `/oneria nick`.
* **Highly Configurable:** Adjust obfuscation distance, sneak distance, length, and behavior.

### Advanced Chat System

Professional chat formatting with markdown and color support:

* **Custom Chat Format:** Fully customizable chat message template with variables (`$time`, `$name`, `$msg`).
* **LuckPerms Integration:** Display prefixes and suffixes from LuckPerms in chat (optional - works without LuckPerms).
* **Markdown Support:** Real-time formatting with `**bold**`, `*italic*`, `__underline__`, `~~strikethrough~~`.
* **Color Codes:** Full support for both `&` and `§` color codes in chat messages.
* **Timestamp System:** Optional timestamps with customizable Java SimpleDateFormat.
* **Global Message Color:** Set a default color for all chat messages.
* **Colors Command:** `/colors` displays all available colors and formatting codes with visual preview.

### Join/Leave Messages System

Fully customizable player connection messages:

* **Custom Join Messages:** Define your own join message with color codes and variables.
* **Custom Leave Messages:** Personalized disconnect messages for players.
* **Variable Support:** Use `{player}` for real username and `{nickname}` for custom nicknames.
* **Disable Option:** Set messages to "none" to completely disable announcements.
* **Color Code Support:** Full `§` and `&` color code support in messages.
* **Server-Wide Broadcast:** All players see the custom join/leave messages.

### World Border Warning System

Automatic distance-based warnings to keep players within boundaries:

* **Configurable Distance:** Set warning trigger distance from spawn (default: 2000 blocks).
* **One-Time Warnings:** Players receive ONE warning when exceeding limit, preventing spam.
* **Auto-Reset:** Warning resets when player returns to safe zone, allows re-warning if they go out again.
* **Custom Messages:** Fully customizable warning message with variables (`{distance}`, `{player}`).
* **Sound Effects:** Warning sound (note block bass) plays when triggered.
* **Performance Optimized:** Configurable check interval (default: every 2 seconds).
* **2D Distance:** Uses X/Z coordinates only for realistic border detection.

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
* **Permission System:** Multi-layered permission detection (Scoreboard tags, OP levels, LuckPerms groups - LuckPerms optional).
* **Nickname Management:** Set custom nicknames for players with full color code support.
* **Blacklist Management:** Hide specific players permanently for stealth operations.

### Welcome & Integration

Enhance player experience from the moment they join:

* **Custom Welcome Messages:** Display multi-line welcome messages with color codes and variable support.
* **Sound Effects:** Play specific sounds when a player joins (fully customizable).
* **LuckPerms Ready:** Deep integration with LuckPerms for permissions, prefixes, suffixes, and group management (fully optional - works without LuckPerms).
* **Schedule Integration:** Welcome messages include server status and time remaining.

### Data Persistence

Smart data management for reliability:

* **Nickname Storage:** All nicknames are automatically saved to `world/data/oneriamod/nicknames.json`.
* **License Storage:** All profession licenses are automatically saved to `world/data/oneriamod/licenses.json`.
* **Auto-Save:** Every nickname and license change is instantly persisted to disk.
* **JSON Format:** Human-readable format allows manual editing if needed.
* **UUID-Based:** Nicknames and licenses tied to UUIDs, not usernames, for reliability.
* **Lazy Loading:** Efficient initialization only when needed.

---

## Commands

### Configuration Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria config reload` | OP Level 2 | Reloads the configuration file and clears all caches. |
| `/oneria config status` | OP Level 2 | Displays the current status of all mod systems. |
| `/oneria config set <option> <value>` | OP Level 2 | Modify any configuration option in real-time. |
| `/oneria config set hideNametags <true/false>` | OP Level 2 | Hide all player nametags above heads. |
| `/oneria config set showNametagPrefixSuffix <true/false>` | OP Level 2 | Show LuckPerms prefix/suffix with nicknames in nametags. |
| `/setplatform <name> <dimension> <x> <y> <z>` | OP Level 2 | Create or update a teleportation platform. |

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
| `/oneria whitelist add <player>` | OP Level 2 | Adds a player to the blur bypass whitelist (always see all names). |
| `/oneria whitelist remove <player>` | OP Level 2 | Removes a player from the whitelist. |
| `/oneria whitelist list` | OP Level 2 | Lists all whitelisted players. |

### Blacklist Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria blacklist add <player>` | OP Level 2 | Adds a player to the always-hidden blacklist (always obfuscated). |
| `/oneria blacklist remove <player>` | OP Level 2 | Removes a player from the blacklist. |
| `/oneria blacklist list` | OP Level 2 | Lists all blacklisted players. |

### License Commands

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria license give <player> <profession>` | OP Level 2 | Grant functional license with full access to profession abilities. |
| `/oneria license giverp <player> <profession> <days>` | OP Level 2 | Grant temporary RP license (visual only, no permissions, expires after X days). |
| `/oneria license revoke <player> <profession>` | OP Level 2 | Revoke license and automatically remove from player inventory. |
| `/oneria license list` | OP Level 2 | Display all licenses for all players across the server. |
| `/oneria license list <player>` | OP Level 2 | Display licenses for specific player. |
| `/oneria license check <player> <profession>` | OP Level 2 | Verify if player has specific license. |

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
| `/colors` | Everyone | Displays all available color codes and formatting options. |

### Convenient Aliases

| Alias | Equivalent Command | Description |
|:------|:-------------------|:------------|
| `/platform [target] [id]` | `/oneria staff platform [target] [id]` | Quick access to platform teleportation for staff. |
| `/schedule` | `/oneria schedule` | Quick access to server schedule information. |
| `/horaires` | `/oneria schedule` | French alias for schedule command. |
| `/colors` | N/A | Displays color and formatting reference guide. |

---

## Configuration

The mod uses two configuration files for better organization:

* **`serverconfig/oneriamod-server.toml`** - Core mod features (obfuscation, schedule, chat, etc.)
* **`serverconfig/oneria-professions.toml`** - Profession and restriction settings

Both files are fully documented with inline examples and organized into logical sections.

### Configuration Sections

#### Obfuscation Settings
* `proximityDistance` - Distance (in blocks) before names are obfuscated (default: 8).
* `enableBlur` - Master switch for the obfuscation system.
* `obfuscatedNameLength` - Number of characters in the obfuscated name (default: 5).
* `obfuscatePrefix` - Whether to hide rank prefixes when obfuscating.
* `opsSeeAll` - Operators always see all names clearly.
* `debugSelfBlur` - Enable self-blur for testing purposes.
* `hideNametags` - Hide all player nametags above heads (default: false).
* `whitelist` - List of players who bypass the blur system (always see all names).
* `blacklist` - List of players who are always hidden (stealth mode).
* `alwaysVisibleList` - NEW! List of players who are ALWAYS visible in TabList (never blurred).
* `blurSpectators` - NEW! Automatically hide players in spectator mode from TabList (default: true).
* `whitelistExemptProfessions` - NEW! Whitelist bypasses all profession restrictions (default: true).
* `enableSneakStealth` - Enable stealth mode for sneaking players (default: true).
* `sneakProximityDistance` - Detection distance for sneaking players in blocks (default: 2, range: 1-32).

#### Profession System (oneria-professions.toml)
* `professions` - List of profession definitions (format: `id;DisplayName;ColorCode`).
* `globalBlockedCrafts` - Items blocked from crafting for everyone by default.
* `globalUnbreakableBlocks` - Blocks that cannot be broken by anyone by default.
* `globalBlockedItems` - Items that cannot be used/interacted with by default.
* `globalBlockedEquipment` - Armor/weapons that cannot be equipped by default.
* `professionAllowedCrafts` - Profession-specific craft permissions (format: `profession;item1,item2`).
* `professionAllowedBlocks` - Profession-specific mining permissions.
* `professionAllowedItems` - Profession-specific item usage permissions.
* `professionAllowedEquipment` - Profession-specific equipment permissions.
* `craftBlockedMessage` - Message shown when craft is blocked (supports `{item}`, `{profession}`).
* `blockBreakBlockedMessage` - Message shown when block break is blocked.
* `itemUseBlockedMessage` - Message shown when item use is blocked.
* `equipmentBlockedMessage` - Message shown when equipment is blocked.

#### Chat System
* `enableChatFormat` - Enable custom chat formatting system.
* `playerNameFormat` - Player name format in chat (variables: `$prefix`, `$name`, `$suffix`).
* `chatMessageFormat` - Complete chat message format (variables: `$time`, `$name`, `$msg`).
* `chatMessageColor` - Global color for chat messages (16 Minecraft colors available).
* `enableTimestamp` - Show timestamp in chat messages.
* `timestampFormat` - Timestamp format (Java SimpleDateFormat, default: "HH:mm").
* `markdownEnabled` - Enable markdown styling (`**bold**`, `*italic*`, etc.).
* `enableColorsCommand` - Enable `/colors` command.

#### Join and Leave Messages
* **`enableCustomJoinLeave`** - Enable custom join/leave messages (default: true).
* **`joinMessage`** - Join message template (variables: `{player}`, `{nickname}`, default: "§e{player} §7joined the game").
* **`leaveMessage`** - Leave message template (variables: `{player}`, `{nickname}`, default: "§e{player} §7left the game").

#### World Border Warning
* **`enableWorldBorderWarning`** - Enable distance-based warnings (default: true).
* **`worldBorderDistance`** - Warning trigger distance from spawn in blocks (default: 2000, range: 100-100000).
* **`worldBorderMessage`** - Warning message template (variables: `{distance}`, `{player}`).
* **`worldBorderCheckInterval`** - Check frequency in ticks (default: 40 = 2 seconds, range: 20-200).

#### Permissions System
* `staffTags` - Scoreboard tags considered as staff (default: `["admin", "modo", "staff", "builder"]`).
* `opLevelBypass` - Minimum OP level to be considered staff (default: 2).
* `useLuckPermsGroups` - Enable LuckPerms group detection (optional - mod works without LuckPerms).
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
* **Optional:** LuckPerms (for advanced permissions, prefix/suffix support, and chat integration - NOT required)

### Steps
1. Download the latest `oneriamod-X.X.X.jar` from the releases page.
2. Place the JAR file into your server's `mods` folder.
3. **(Optional)** Install [LuckPerms](https://luckperms.net/) for advanced permission management and chat prefixes/suffixes (mod works without it).
4. Start your server to generate the configuration files.
5. Edit `serverconfig/oneriamod-server.toml` and `serverconfig/oneria-professions.toml` to customize your settings.
6. Reload with `/oneria config reload` or restart the server.

---

## Technical Details

### Performance
* **Permission Caching:** Results cached for 30 seconds to minimize overhead.
* **Profession Checks:** License verification uses 30-second cache for efficient lookups.
* **Tick Optimization:** Schedule system checks only every 20 seconds (400 ticks).
* **Efficient Packet Handling:** Minimal performance impact on TabList updates.
* **Optimized Color Parsing:** Segment-based parsing for complex formatting with minimal overhead.
* **Sneak Detection:** Native `isCrouching()` check with zero overhead.
* **World Border Checks:** Configurable interval (default 2 seconds) with 2D distance calculation.
* **One-Time Warnings:** Prevents message spam with efficient state tracking.
* **Action Bar Notifications:** Anti-spam cooldowns (1-3 seconds) prevent message flooding.
* **Cache Cleanup:** Automatic cleanup every 20 seconds prevents memory leaks.

### Compatibility
* **Server-Side Only:** No client-side installation required.
* **Mixin-Based:** Uses Mixin for clean packet interception and chat formatting.
* **API Ready:** Public API available for other mods (`OneriaPermissions.isStaff()`, etc.).
* **LuckPerms Integration:** Full support for prefixes, suffixes, and group-based permissions (optional - gracefully disabled when not present).
* **No Dependencies:** Mod functions fully without any required dependencies.

### New in 2.0.1

* **Complete Profession & License System:** Full craft/mining/equipment restriction system with physical license items.
* **Profession Configuration:** Dedicated config file `oneria-professions.toml` for all profession settings.
* **License Commands:** Grant, revoke, list, and check licenses with smart autocomplete.
* **RP Licenses:** Temporary decorative licenses for roleplay events (no actual permissions).
* **Restriction Enforcement:** Real-time blocking of crafts, mining, item usage, equipment, and attacks.
* **Client Tooltips:** Items show required professions directly in tooltips.
* **Always Visible List:** Force specific players to always appear in TabList.
* **Spectator Blur:** Automatically hide spectator mode players from TabList.
* **Whitelist Exemption:** Whitelisted players bypass all profession restrictions.
* **Persistent Storage:** Licenses stored in `world/data/oneriamod/licenses.json`.
* **Performance Optimizations:** Caching system, anti-spam cooldowns, and efficient permission checks.

For detailed changelog, see CHANGELOG.md.

### New in 1.2.2
* **Client-Side Nametag Hiding:** Event-based nametag visibility control using NeoForge's `RenderNameTagEvent`.
* **Enhanced LuckPerms Compatibility:** Fixed crashes in singleplayer and servers without LuckPerms.
* **Network Synchronization:** Custom packet system for syncing nametag config from server to client.

### New in 1.2.0
* **Join/Leave Messages:** Fully customizable connection announcements with variables and color codes.
* **World Border Warnings:** Automatic distance-based warnings from spawn with configurable triggers.
* **Blacklist System:** Permanently hide specific players for stealth operations.
* **Enhanced LuckPerms Compatibility:** Mod no longer crashes when LuckPerms is absent.
* **Improved Config Handling:** Better error handling and initialization timing.

---

## License

This mod is developed for the **Oneria RP Server** and is provided as-is for community use. You are free to use it on your own server, but official support and updates are prioritized for the Oneria server.

**All Rights Reserved © 2026 Oneria Team**

---

## Credits

* **Development:** Finerus, OneriaTeam
* **Special Thanks:** The Oneria RP community for testing and feedback
* **Libraries:** NeoForge, Mixin, LuckPerms API (optional)

---

## Support

For issues, suggestions, or questions:
* **Discord:** Join the Oneria Discord server
* **GitHub Issues:** Report bugs on our repository

**Note:** This mod is actively maintained for the Oneria server. Community contributions and feedback are welcome!