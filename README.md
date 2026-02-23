# Oneria Mod

**Oneria Mod** is a comprehensive server-side utility mod built for immersive Roleplay servers running Minecraft 1.21.1 on NeoForge. It provides a complete suite of RP tools including proximity-based name obfuscation, a profession and license system, private messaging, schedule automation, staff moderation, advanced chat formatting, world border warnings, named zones, and deep LuckPerms integration — all configurable in real-time without restarts.

> Current version: **2.1.1** — See CHANGELOG for full history.

---

## Requirements

| Dependency | Version | Required |
|:-----------|:--------|:---------|
| Minecraft | 1.21.1 | ✅ |
| NeoForge | 21.1.215+ | ✅ |
| LuckPerms | Any | ⬜ Optional |
| ImmersiveMessages | neoforge-1.21.1 | ⬜ Optional (client-side) |
| TxniLib | neoforge-1.21.1 | ⬜ Optional (client-side) |

> ImmersiveMessages and TxniLib are only required on the **client** if you use `zoneMessageMode = IMMERSIVE`. The server runs without them.

---

## Installation

1. Download the latest `oneriamod-X.X.X.jar` from the releases page.
2. Place the JAR in your server's `mods/` folder.
3. *(Optional)* Install [LuckPerms](https://luckperms.net/) for prefix/suffix and group-based permissions.
4. Start your server — config files are generated automatically.
5. Edit `serverconfig/oneriamod-server.toml` and `serverconfig/oneria-professions.toml`.
6. Reload with `/oneria config reload` or restart.

---

## Features

### Proximity Obfuscation

Prevents metagaming by hiding player identities based on distance:

- Player names in the TabList and above heads are replaced with `?????` beyond a configurable distance (default: 8 blocks).
- **Sneak Stealth Mode:** Crouching players are only detectable at 2 blocks (configurable).
- **Nametag Hiding:** Optional server-side hiding of all nametags above heads.
- **Whitelist:** Players who always see all names clearly (useful for admins).
- **Blacklist:** Players who are always hidden regardless of distance (stealth staff, NPCs).
- **Always Visible List:** Players who are never obfuscated to anyone (event coordinators, important NPCs).
- **Spectator Blur:** Players in spectator mode are automatically hidden from TabList.
- LuckPerms prefix/suffix hidden during obfuscation to prevent rank-based identification.

### Profession & License System

A complete job and economy restriction system for RP servers:

- Define unlimited professions with custom names and colors.
- Physical **license items** are given to players with profession metadata and issuance date.
- **RP Licenses:** Decorative-only licenses for events with expiration dates (no actual permissions).
- **Restriction types:** crafting, block breaking, item usage, equipment, and attacks.
- **Global restrictions** apply to all players by default, with **profession-specific overrides**.
- Wildcard pattern support: `minecraft:*_pickaxe` blocks entire item categories.
- Tooltips show required professions directly on restricted items (client-side sync).
- Whitelist players bypass all profession restrictions.
- Licenses persist in `world/data/oneriamod/licenses.json` with UUID + MC username as key.

### Private Messaging

Custom `/msg` system replacing vanilla messaging:

- `/msg <player> <message>`, `/tell`, `/w`, `/whisper` — send private messages.
- `/r <message>` — reply to the last person who messaged you.
- Sender sees: `[MP] Vous écrivez à [RecipientNick] : message`
- Recipient sees: `[MP] [SenderNick] vous écrit : message`
- Nicknames are clickable — hover for prompt, click to auto-fill `/msg <player> `.
- Last interlocutor tracked per player and reset on logout.
- Fully replaces vanilla `/msg`, `/tell`, `/w`, `/whisper`.

### Whois Command

Identity lookup tool for staff:

- `/whois <nickname>` — find the real MC username and UUID behind any nickname.
- Case-insensitive search, strips color codes for comparison.
- UUID is **clickable** — opens the player's [NameMC](https://namemc.com) profile on hover/click.
- Works on offline players via the profile cache.
- Requires OP Level 2.

### Named Zones

Admin-configurable geographic zones with entry/exit messages:

- Define zones in config: `name;centerX;centerZ;radius;messageEnter;messageExit`.
- Entry and exit messages are sent via action bar, chat, or ImmersiveMessageAPI overlay (configurable).
- Multiple simultaneous zones supported.
- Per-player state tracking — no message spam on repeated checks.
- Zone distance uses 2D (X/Z) calculation — altitude is ignored.

### World Border Warnings

Automatic distance-based warnings to keep players within boundaries:

- Players receive a warning when exceeding the configured distance from spawn (default: 2000 blocks).
- Warning resets when the player returns to the safe zone, allowing re-warning if they leave again.
- Sound notification on warning.
- Message mode configurable: `ACTION_BAR` (default), `CHAT`, or `IMMERSIVE`.

### Schedule System

Automated server opening and closing:

- Define opening and closing times in `HH:MM` format.
- Automatic warnings sent X minutes before closing (configurable intervals).
- Non-staff players are automatically kicked at closing time.
- Staff receive a notification when the server opens.
- Schedule status viewable by all players via `/oneria schedule`.

### Advanced Chat System

Professional chat formatting with markdown and LuckPerms support:

- Fully customizable message format with variables (`$time`, `$name`, `$msg`).
- LuckPerms prefix and suffix integration (optional).
- Markdown support: `**bold**`, `*italic*`, `__underline__`, `~~strikethrough~~`.
- Full `&` and `§` color code support.
- Optional timestamp with customizable Java `SimpleDateFormat`.
- `/colors` command displays all available colors and formatting codes.

### Join / Leave Messages

- Fully customizable join and leave messages with `{player}` and `{nickname}` variables.
- Full color code support.
- Can be disabled entirely.

### Staff Moderation Tools

Silent staff commands with logging:

- `/oneria staff gamemode`, `tp`, `effect`, `platform` — actions are invisible to other players.
- All silent commands are logged to the console and optionally to other online staff.
- Target player notification is disabled by default (stealth mode).

### Nickname System

Persistent RP nicknames:

- `/oneria nick <player> <nickname>` — set a custom nickname with full color code support.
- Nicknames appear in TabList, above heads, chat, and `/msg`.
- Stored in `world/data/oneriamod/nicknames.json` with UUID + MC username as key.
- `/whois` allows staff to reverse-lookup any nickname.

### Teleportation Platforms

- Define named teleportation platforms across dimensions.
- Staff can teleport players to platforms via `/oneria staff platform`.

---

## Commands

### Configuration

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria config reload` | OP 2 | Reload config and clear all caches. |
| `/oneria config status` | OP 2 | Display status of all mod systems. |
| `/oneria config set <option> <value>` | OP 2 | Modify any config option in real-time. |

### Staff

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria staff gamemode <mode> [player]` | Staff | Silently change gamemode. |
| `/oneria staff tp <player>` | Staff | Silently teleport to a player. |
| `/oneria staff effect <player> <effect> <duration> <amplifier>` | Staff | Silently apply an effect. |
| `/oneria staff platform [player] [id]` | Staff | Teleport player(s) to a platform. |

### Whitelist / Blacklist

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria whitelist add/remove/list` | OP 2 | Manage the blur bypass whitelist. |
| `/oneria blacklist add/remove/list` | OP 2 | Manage the always-hidden blacklist. |

### Licenses

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria license give <player> <profession>` | OP 2 | Grant a functional license. |
| `/oneria license giverp <player> <profession> <days>` | OP 2 | Grant a decorative RP license. |
| `/oneria license revoke <player> <profession>` | OP 2 | Revoke a license and remove the item. |
| `/oneria license list [player]` | OP 2 | List all licenses (or for a specific player). |
| `/oneria license check <player> <profession>` | OP 2 | Check if a player has a specific license. |

### Nicknames

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria nick <player> <nickname>` | OP 2 | Set a nickname (supports color codes). |
| `/oneria nick <player>` | OP 2 | Reset a nickname. |
| `/oneria nick list` | OP 2 | List all active nicknames. |
| `/whois <nickname>` | OP 2 | Find the MC username behind a nickname. |

### Messaging

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/msg <player> <message>` | Everyone | Send a private message. |
| `/tell`, `/w`, `/whisper` | Everyone | Aliases for `/msg`. |
| `/r <message>` | Everyone | Reply to the last person who messaged you. |

### Public

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/oneria schedule` / `/schedule` / `/horaires` | Everyone | View server opening/closing times. |
| `/colors` | Everyone | Display all color codes and formatting options. |
| `/setplatform <id> <dimension> <x> <y> <z>` | OP 2 | Create or update a teleportation platform. |

---

## Configuration

The mod uses two config files:

- **`serverconfig/oneriamod-server.toml`** — Core features (obfuscation, schedule, chat, messaging, etc.)
- **`serverconfig/oneria-professions.toml`** — Profession definitions and restrictions

Both files are fully documented with inline comments and examples.

### Key Options — oneriamod-server.toml

#### Obfuscation Settings
| Option | Default | Description |
|:-------|:--------|:------------|
| `proximityDistance` | `8` | Distance in blocks before names are obfuscated. |
| `enableBlur` | `true` | Master switch for the obfuscation system. |
| `obfuscatedNameLength` | `5` | Number of `?` characters in the obfuscated name. |
| `obfuscatePrefix` | `true` | Hide LuckPerms rank prefix when obfuscating. |
| `opsSeeAll` | `true` | Operators always see all names clearly. |
| `hideNametags` | `false` | Hide all player nametags above heads. |
| `showNametagPrefixSuffix` | `true` | Show LuckPerms prefix/suffix in nametags. |
| `enableSneakStealth` | `true` | Crouching players are harder to detect. |
| `sneakProximityDistance` | `2` | Detection distance for sneaking players (blocks). |
| `blurSpectators` | `true` | Automatically hide spectators from TabList. |
| `whitelist` | `[]` | Players who always see all names clearly. |
| `blacklist` | `[]` | Players who are always hidden. |
| `alwaysVisibleList` | `[]` | Players who are never obfuscated to anyone. |
| `whitelistExemptProfessions` | `true` | Whitelist players bypass all profession restrictions. |

#### Schedule System
| Option | Default | Description |
|:-------|:--------|:------------|
| `enableSchedule` | `true` | Enable automated opening/closing. |
| `openingTime` | `"19:00"` | Server opening time (HH:MM). Use quotes in-game: `"19:00"`. |
| `closingTime` | `"23:59"` | Server closing time (HH:MM). |
| `warningTimes` | `[45,30,10,1]` | Minutes before closing to send warnings. |
| `kickNonStaff` | `true` | Kick non-staff players at closing time. |

#### World Border Warning
| Option | Default | Description |
|:-------|:--------|:------------|
| `enableWorldBorderWarning` | `true` | Enable distance-based warnings. |
| `worldBorderDistance` | `2000` | Warning trigger distance from spawn (blocks). |
| `worldBorderMessage` | `"..."` | Warning message (`{distance}`, `{player}` variables). |
| `worldBorderCheckInterval` | `40` | Check frequency in ticks (40 = 2 seconds). |
| `zoneMessageMode` | `ACTION_BAR` | Display mode: `ACTION_BAR`, `CHAT`, or `IMMERSIVE`. |
| `namedZones` | `[]` | Named zones (format: `name;cx;cz;radius;msgEnter;msgExit`). |

#### Chat System
| Option | Default | Description |
|:-------|:--------|:------------|
| `enableChatFormat` | `true` | Enable custom chat formatting. |
| `playerNameFormat` | `"$prefix $name $suffix"` | Name format in chat. |
| `chatMessageFormat` | `"$time \| $name: $msg"` | Full message template. |
| `enableTimestamp` | `true` | Show timestamp in messages. |
| `timestampFormat` | `"HH:mm"` | Java SimpleDateFormat for timestamps. |
| `markdownEnabled` | `true` | Enable markdown styling. |

#### Join / Leave Messages
| Option | Default | Description |
|:-------|:--------|:------------|
| `enableCustomJoinLeave` | `true` | Enable custom join/leave messages. |
| `joinMessage` | `"§e{player} §7joined the game"` | Join message (`{player}`, `{nickname}`). |
| `leaveMessage` | `"§e{player} §7left the game"` | Leave message (`{player}`, `{nickname}`). |

### Key Options — oneria-professions.toml

| Option | Description |
|:-------|:------------|
| `professions` | Profession definitions (format: `id;DisplayName;§ColorCode`). |
| `globalBlockedCrafts` | Items blocked from crafting for all players. |
| `globalUnbreakableBlocks` | Blocks that cannot be broken by anyone. |
| `globalBlockedItems` | Items that cannot be used/interacted with. |
| `globalBlockedEquipment` | Armor/weapons that cannot be equipped. |
| `professionAllowedCrafts` | Profession-specific craft overrides (format: `profession;item1,item2`). |
| `professionAllowedBlocks` | Profession-specific block breaking overrides. |
| `professionAllowedItems` | Profession-specific item usage overrides. |
| `professionAllowedEquipment` | Profession-specific equipment overrides. |

---

## Data Storage

All persistent data is stored in the world folder:

| File | Contents |
|:-----|:---------|
| `world/data/oneriamod/nicknames.json` | Player nicknames, keyed by `UUID (McUsername)`. |
| `world/data/oneriamod/licenses.json` | Player licenses, keyed by `UUID (McUsername)`. |

Both files are human-readable JSON and can be edited manually. Keys include both the UUID and the MC username for readability. Old UUID-only keys from previous versions are still loaded correctly.

---

## Technical Details

### Architecture
- **Server-Side Only:** No client-side installation required for core features.
- **Mixin-Based:** Low-level packet interception for TabList obfuscation and chat formatting.
- **Event-Driven:** NeoForge event system for profession restrictions, nametags, and player lifecycle.
- **JSON Storage:** Gson-based serialization with automatic directory creation and error handling.

### Performance
- Permission checks cached for 30 seconds.
- Profession restriction checks use 30-second cache with lazy initialization.
- Schedule system checks every 20 seconds (400 ticks).
- World border checks configurable (default: 40 ticks).
- Anti-spam cooldown maps on all action bar notifications (1–3 seconds).
- Cache cleanup every 20 seconds to prevent memory leaks.

### Compatibility
- **LuckPerms:** Full prefix, suffix, and group-based permission support. Gracefully disabled when absent.
- **ImmersiveMessages:** Optional overlay messages for zones and border warnings. Falls back to action bar when unavailable.
- **API:** `OneriaPermissions.isStaff()` available for other mods.

---

## License

Developed for the **Oneria RP Server**. All Rights Reserved © 2026 Oneria Team.

Free to use on your own server. Official support is prioritized for the Oneria server.

## Credits

- **Development:** Finerus, OneriaTeam, Echollim
- **Special Thanks:** The Oneria RP community for testing and feedback
- **Libraries:** NeoForge, Mixin, LuckPerms API, ImmersiveMessages, TxniLib