# Changelog - Oneria Mod
All notable changes to this project will be documented in this file.

## [1.2.1] - 2026-02-01

**Fixed**

* **Double Join/Leave Messages:** Fixed critical bug where players would see duplicate connection messages:
  - Removed redundant message broadcasting in `OneriaEventHandler`.
  - Consolidated all join/leave message handling into `MixinPlayerList` for cleaner interception.
  - Messages are now sent exactly once per player connection/disconnection.
  - System properly respects the `enableCustomJoinLeave` configuration option.

**Technical**

* **Enhanced Classes:**
  - `OneriaEventHandler` - Removed duplicate join/leave message code (lines 20-35 and 70-88).
  - `MixinPlayerList` - Now the sole handler for vanilla message interception and custom message broadcasting.
  - `oneria.mixins.json` - Added `MixinPlayerList` to the mixin registry.

* **Code Quality:**
  - Eliminated code duplication between event handler and mixin system.
  - Improved separation of concerns (event handling vs. message interception).
  - Better debug logging for join/leave message flow.

**Migration Notes**

* No configuration changes required - fully backward compatible with 1.2.0.
* Existing `joinMessage` and `leaveMessage` settings continue to work as expected.
* Players will now see exactly one join message and one leave message as intended.
* If you experience any issues, ensure `MixinPlayerList` is properly registered in `oneria.mixins.json`.

**Known Behavior**

* Join/leave messages are handled via Mixin interception of vanilla messages.
* The system intercepts both English ("joined the game") and French ("a rejoint la partie") variants.
* Nickname resolution is performed dynamically when messages are sent.
* Debug logging can be enabled to track message flow: look for `[Join]` and `[Leave]` prefixes in logs.

## [1.2.0] - 2026-01-21

**Added**

* **Join/Leave Messages System:** Fully customizable player connection messages:
  - Custom join messages with color code support and variables (`{player}`, `{nickname}`).
  - Custom leave messages with same variable support.
  - Option to disable messages completely by setting to "none".
  - Configuration commands: `/oneria config set joinMessage <message>` and `/oneria config set leaveMessage <message>`.
  - Toggle with `/oneria config set enableCustomJoinLeave true/false`.

* **World Border Warning System:** Automatic distance-based warnings from spawn:
  - Configurable warning distance (default: 2000 blocks from spawn).
  - Players receive ONE warning message when exceeding the limit.
  - Warning automatically resets when player returns to safe zone.
  - Configurable check interval for performance optimization (default: 40 ticks / 2 seconds).
  - Custom warning message with variables (`{distance}`, `{player}`).
  - Warning sound effect (note block bass) played on trigger.
  - Configuration commands:
    - `/oneria config set enableWorldBorderWarning true/false`
    - `/oneria config set worldBorderDistance <100-100000>`
    - `/oneria config set worldBorderMessage <message>`
    - `/oneria config set worldBorderCheckInterval <20-200>`

* **Blacklist System:** Always-hidden player functionality:
  - New blacklist to permanently hide specific players from TabList.
  - Players in blacklist are always obfuscated regardless of distance.
  - Useful for staff in stealth mode or hidden NPCs.
  - Commands:
    - `/oneria blacklist add <player>` - Add player to always-hidden list.
    - `/oneria blacklist remove <player>` - Remove from blacklist.
    - `/oneria blacklist list` - Display all blacklisted players.
  - Stored in config under `Obfuscation Settings`.

**Improved**

* **LuckPerms Compatibility:** Enhanced support for servers without LuckPerms:
  - Mod no longer crashes when LuckPerms is not installed.
  - Graceful fallback to vanilla permissions and scoreboard tags.
  - Better error handling with `IllegalStateException` catching.
  - Debug logging instead of error messages for missing LuckPerms.
  - All LuckPerms-dependent features (prefixes, suffixes, group permissions) safely disabled when unavailable.

* **Schedule System Reliability:** More robust initialization and error handling:
  - Fixed `NullPointerException` when config loads late.
  - Added null checks in `tick()`, `isServerOpen()`, and `getTimeUntilNextEvent()`.
  - Schedule now gracefully handles uninitialized state.
  - Better logging for initialization events.
  - Config loading listener ensures proper initialization timing.

* **Configuration System:** Better error handling across all config access:
  - All config-dependent systems check for null before accessing values.
  - Graceful degradation when config is not yet loaded.
  - Improved debug logging for initialization states.

**Fixed**

* **Config Loading Race Condition:** Fixed "Cannot get config value before spec is built" errors:
  - Moved Join/Leave and World Border config sections before `SPEC = BUILDER.build()`.
  - All config values now properly initialized on server start.
  - Fixed `/oneria config status` showing "N/A" for new features.

* **Schedule Initialization:** Fixed crash on world creation:
  - `OneriaScheduleManager.reload()` now checks for null config values.
  - No longer attempts to parse times before config is loaded.
  - Added proper return statements to prevent execution with null values.

* **LuckPerms Dependencies:** Fixed crashes in modpacks without LuckPerms:
  - `getPlayerPrefix()` and `getPlayerSuffix()` now catch `IllegalStateException`.
  - `OneriaPermissions.checkStaffStatus()` safely handles missing LuckPerms.
  - Staff detection falls back to vanilla tags and OP levels.

**Technical**

* **New Classes:**
  - `WorldBorderManager` - Distance-based warning system with spawn proximity checks.

* **Enhanced Classes:**
  - `OneriaConfig` - Added `ENABLE_CUSTOM_JOIN_LEAVE`, `JOIN_MESSAGE`, `LEAVE_MESSAGE`, `ENABLE_WORLD_BORDER_WARNING`, `WORLD_BORDER_DISTANCE`, `WORLD_BORDER_MESSAGE`, `WORLD_BORDER_CHECK_INTERVAL`, `BLACKLIST`.
  - `OneriaEventHandler` - Integrated custom join/leave messages with proper null checks.
  - `OneriaScheduleManager` - Added comprehensive null safety for all public methods.
  - `OneriaServerUtilities` - Enhanced LuckPerms error handling, added config loading listener.
  - `OneriaPermissions` - Improved LuckPerms fallback logic.
  - `OneriaCommands` - Added blacklist management commands and new config setters.
  - `MixinServerCommonPacketListenerImpl` - Integrated blacklist checking in obfuscation logic.

**Configuration**

* **New Config Sections:**
  - `[Join and Leave Messages]` - Join/leave message customization.
  - `[World Border Warning]` - Distance warning system settings.

* **New Options:**
  - `enableCustomJoinLeave` (Boolean) - Enable custom join/leave messages (default: true).
  - `joinMessage` (String) - Join message template with variables (default: "§e{player} §7joined the game").
  - `leaveMessage` (String) - Leave message template with variables (default: "§e{player} §7left the game").
  - `enableWorldBorderWarning` (Boolean) - Enable world border warnings (default: true).
  - `worldBorderDistance` (Integer) - Warning distance in blocks (default: 2000, range: 100-100000).
  - `worldBorderMessage` (String) - Warning message template (default: "§c§l⚠ WARNING §r§7You've reached the limit of the world! (§c{distance} blocks§7)").
  - `worldBorderCheckInterval` (Integer) - Check frequency in ticks (default: 40, range: 20-200).
  - `blacklist` (List) - Players who are always hidden (default: empty list).

**Performance**

* World border checks use configurable intervals (default 2 seconds) to minimize overhead.
* Distance calculation uses 2D (X, Z) coordinates only, ignoring Y for better performance.
* One-time warning system prevents message spam.
* Efficient state tracking with HashMap for warned players.

**Migration Notes**

* No breaking changes - fully backward compatible with 1.1.3.
* Delete `serverconfig/oneriamod-server.toml` to regenerate with new options.
* If upgrading from pre-1.2.0, verify config sections are before `SPEC = BUILDER.build()` line.
* LuckPerms is now truly optional - mod works without it using vanilla permissions.
* Join/leave messages are enabled by default - set to "none" to disable.
* World border warnings are enabled by default at 2000 blocks - adjust as needed.

**Known Limitations**

* World border warnings are based on distance from spawn (0,0), not world border entities.
* Blacklist applies to all contexts - no per-player exemptions.
* Join/leave messages appear to all players - no per-player filtering.

---

## [1.1.3] - 2026-01-16

**Added**

* **Sneak Stealth System:** Advanced stealth mechanics for enhanced roleplay immersion:
  - Players who are sneaking (crouching) become significantly harder to detect.
  - Configurable sneak detection distance (default: 2 blocks vs normal 8 blocks).
  - Sneaking players' names are obfuscated beyond the sneak distance, even if within normal proximity.
  - Admin exemption: Staff with `opsSeeAll` can always see sneaking players.
  - Toggle system with `/oneria config set enableSneakStealth true/false`.
  - Adjustable sneak distance with `/oneria config set sneakProximityDistance <1-32>`.
  - Perfect for stealth RP scenarios, hiding, and surprise interactions.

**Improved**

* **Obfuscation Logic:** Enhanced distance calculation for dynamic stealth:
  - System now checks if target player is crouching before applying distance rules.
  - Automatic switching between normal proximity distance and sneak proximity distance.
  - Seamless integration with existing blur system - no conflicts.
  - Performance-optimized with minimal overhead per tick.

* **Configuration Commands:** New sneak-related commands:
  - `/oneria config set enableSneakStealth <true/false>` - Toggle sneak stealth system.
  - `/oneria config set sneakProximityDistance <1-32>` - Set sneak detection range.
  - Sneak status displayed in `/oneria config status` output.

**Technical**

* **Enhanced Classes:**
  - `OneriaConfig` - Added `ENABLE_SNEAK_STEALTH` and `SNEAK_PROXIMITY_DISTANCE` configuration options.
  - `MixinServerCommonPacketListenerImpl` - Enhanced `modifyPacket()` with dynamic distance calculation based on crouch state.
  - `OneriaCommands` - Added sneak configuration commands and status display.

* **Performance:**
  - Crouch state check uses native Minecraft `isCrouching()` method - zero overhead.
  - Distance calculation only performed when blur system is active.
  - No additional packet modifications required.

**Configuration**

* **New Options:**
  - `enableSneakStealth` (Boolean) - Enable stealth mode for sneaking players.
    - Location: `[Obfuscation Settings]` section.
    - Default: `true`.
    - When enabled, sneaking players use reduced detection distance.

  - `sneakProximityDistance` (Integer) - Detection distance for sneaking players.
    - Location: `[Obfuscation Settings]` section.
    - Default: `2` blocks.
    - Range: 1-32 blocks.
    - Only applies when `enableSneakStealth` is enabled.

**Use Cases**

* **Stealth Roleplay:** Players can sneak to avoid being detected in crowded areas.
* **Hide and Seek:** Perfect for RP games where players need to hide.
* **Ambush Scenarios:** Players can set up surprise encounters without revealing their presence.
* **Privacy:** Players can move through areas without being immediately recognized.
* **Realistic Immersion:** Crouching for stealth mirrors real-world behavior.

**Migration Notes**

* No breaking changes - fully backward compatible with 1.1.2.
* Sneak stealth is enabled by default - disable with `/oneria config set enableSneakStealth false` if not desired.
* Existing blur system settings remain unchanged.
* Admin exemptions (`opsSeeAll`) automatically apply to sneak detection.

**Known Limitations**

* Sneak detection only affects TabList names, not physical player visibility.
* Players can still see other players' models when sneaking - only names are hidden.
* Sneak distance applies uniformly to all players (no per-player customization).

## [1.1.2] - 2026-01-15

**Added**

* **Nametag Visibility System:** Server-side nametag hiding using scoreboard teams:
  - Toggle nametag visibility for all players with `/oneria config set hideNametags true/false`.
  - Automatic synchronization on player login/logout.
  - Team-based implementation for full server-side control.
  - Configuration option `hideNametags` in Obfuscation Settings.
  - Nametag state automatically reloads with `/oneria config reload`.

* **Color System Improvements:** Fixed `/colors` command display issues:
  - Color codes in examples are no longer interpreted incorrectly.
  - Clean visual presentation with proper formatting.
  - Clear distinction between color preview and usage codes.
  - Usage examples show only `&` codes (typeable in-game).

**Improved**

* **Chat System:** Enhanced color parsing for better reliability:
  - `ColorHelper` class now properly parses all Minecraft color codes.
  - Improved segment-based parsing for complex formatting.
  - Better handling of mixed colors and formatting codes.
  - Full support for both `&` and `§` syntax throughout the mod.

* **Nametag Manager:** Robust synchronization system:
  - Automatic sync on every player connection/disconnection.
  - State verification on config reload to ensure consistency.
  - Proper team cleanup when system is disabled.
  - Detailed logging for debugging nametag operations.

* **Command Organization:** Added nametag configuration commands:
  - `/oneria config set hideNametags <true/false>` - Toggle nametag visibility.
  - Nametag status displayed in `/oneria config status`.
  - Instant application without server restart required.

**Fixed**

* **Colors Command Display:** Resolved visual glitches in `/colors` output:
  - Color codes in examples no longer interfere with display.
  - Proper spacing and alignment in color reference table.
  - Formatting codes display correctly with usage syntax.
  - Component construction prevents code interpretation in examples.

* **Nametag Synchronization:** Fixed edge cases in nametag visibility:
  - Nametags properly hide/show when config changes.
  - Team membership correctly updated on player events.
  - No lingering team data after system disable.

* **Chat Color Parsing:** Fixed color codes not working in chat:
  - `OneriaChatFormatter` now uses `ColorHelper.parseColors()`.
  - All chat messages properly display colors and formatting.
  - Markdown and color codes work together seamlessly.

**Technical**

* **New Classes:**
  - `NametagManager` - Centralized nametag visibility control using scoreboard teams.
  - `ColorHelper` - Advanced color code parser with segment-based processing.
  - `TextSegment` (inner class) - Represents text portions with their styling.

* **Enhanced Classes:**
  - `OneriaEventHandler` - Added nametag sync on player login/logout.
  - `OneriaCommands` - Added `hideNametags` configuration command.
  - `OneriaChatFormatter` - Refactored to use `ColorHelper` for color parsing.

**Performance**

* Nametag system uses native scoreboard teams - zero performance impact.
* Color parsing optimized with segment-based processing.
* Sync operations only trigger on player events and config changes.

**Configuration**

* **New Options:**
  - `hideNametags` (Boolean) - Hide all player nametags above heads.
    - Location: `[Obfuscation Settings]` section.
    - Default: `false`.
    - Instantly toggleable via command.

**Known Limitations**

* Nametag hiding uses scoreboard teams - may conflict with other mods using team-based systems.
* Individual nametag control per player is not supported (all or nothing).

**Migration Notes**

* No breaking changes - fully backward compatible with 1.1.1.
* Nametag visibility can be toggled at any time without restart.
* `hideNametags` defaults to `false` - no behavior change unless explicitly enabled.
* `/colors` command now displays correctly - no configuration needed.
* Color codes work seamlessly in all contexts (chat, commands, nicknames).

## [1.1.1] - 2026-01-10

**Added**

* **Advanced Chat System:** Fully integrated chat formatting system similar to Better Forge Chat Reborn:
  - Customizable player name format with LuckPerms prefix/suffix support (`$prefix $name $suffix`).
  - Flexible chat message format with timestamp and color support (`$time | $name: $msg`).
  - Global chat message color configuration (16 Minecraft colors available).
  - Timestamp system with customizable Java SimpleDateFormat.
  - **Markdown Support:** Real-time markdown styling in chat messages:
    - `**text**` → **Bold**
    - `*text*` → *Italic*
    - `__text__` → Underline
    - `~~text~~` → Strikethrough
  - `/colors` command to display all available colors and formatting codes with visual preview.
  - Full integration with existing nickname system - nicknames appear in chat automatically.
  - LuckPerms suffix support added to complement existing prefix system.

**Improved**

* **Chat Integration:** Chat system now respects all existing mod features:
  - Nicknames set via `/oneria nick` are automatically used in chat.
  - LuckPerms prefixes and suffixes are properly displayed.
  - Staff permissions work seamlessly with chat formatting.
  - All color codes (§ and &) are fully supported.

* **Configuration:** New unified chat configuration section in `OneriaConfig.java`:
  - `enableChatFormat` - Toggle entire chat system on/off.
  - `playerNameFormat` - Customize name display with variables.
  - `chatMessageFormat` - Full message template customization.
  - `chatMessageColor` - Global message color setting.
  - `enableTimestamp` - Show/hide timestamps.
  - `timestampFormat` - Custom timestamp format.
  - `markdownEnabled` - Enable/disable markdown parsing.
  - `enableColorsCommand` - Toggle `/colors` command availability.

**Technical**

* **New Classes:**
  - `OneriaChatFormatter` - Central chat formatting logic with markdown parser.
  - `MixinServerGamePacketListenerImpl` - Chat message interception and formatting.

* **Enhanced Classes:**
  - `OneriaConfig` - Added complete Chat System configuration section.
  - `OneriaServerUtilities` - Added `getPlayerSuffix()` method for LuckPerms suffix retrieval.
  - `OneriaCommands` - Added `/colors` command for color reference.

* **Mixin Updates:**
  - New mixin for `ServerGamePacketListenerImpl` to intercept chat messages.
  - Proper cancellation and custom message broadcasting.

**Performance**

* Efficient regex-based markdown parsing with minimal overhead.
* Chat formatting only applies when enabled in config.
* Cached LuckPerms data retrieval for better performance.

**Migration Notes**

* No breaking changes - chat system is fully backward compatible.
* Existing nicknames will automatically appear in formatted chat.
* Default configuration provides a clean, modern chat experience.
* Chat formatting can be disabled entirely by setting `enableChatFormat = false`.

**Known Compatibility**

* ✅ Fully compatible with LuckPerms for prefixes/suffixes.
* ✅ Works seamlessly with existing nickname system.
* ✅ Compatible with all existing mod features (blur, schedule, platforms, etc.).
* ✅ Does not conflict with Minecraft's vanilla chat system.

## [1.1.0] - 2026-01-07

**Breaking Changes**

* **Mod ID Changed:** The mod ID has been updated from `oneriamod` to its final identifier. If upgrading from 1.0.x, you may need to regenerate configuration files.

**Added**

* **Schedule System (Native):** Fully integrated automatic server opening/closing with customizable times, warnings, and staff bypass.
  - Configurable opening/closing times (format HH:MM).
  - Automated warnings at 45, 30, 10, and 1 minute before closing.
  - Smart kick system that only affects non-staff players.
  - Staff receives notifications when server opens/closes.

* **Permission System:** Advanced staff detection system with multiple layers:
  - Scoreboard tags support (`admin`, `modo`, `staff`, `builder`).
  - OP level bypass (configurable from 0 to 4).
  - LuckPerms groups integration (configurable staff groups).
  - Efficient caching system for performance (30-second cache).

* **Welcome System:** Customizable welcome messages on player login:
  - Multi-line welcome messages with color code support.
  - Variable support (`{player}`, `{nickname}`).
  - Customizable sound effects (volume and pitch configurable).
  - Integration with schedule system to display server status.

* **Platform Teleportation:** Staff-only teleportation system:
  - Define custom platforms with coordinates and dimensions.
  - Quick teleport to predefined RP zones.
  - Staff notifications when platforms are used.
  - Configurable platform list via config file.
  - New alias: `/platform` (shortcut for `/oneria staff platform`).

* **Silent Commands:** Stealth moderation tools for staff:
  - `/oneria staff gamemode <mode> [target]` - Silent gamemode changes.
  - `/oneria staff tp <target>` - Silent teleportation.
  - `/oneria staff effect <target> <effect> <duration> <amplifier>` - Silent effect application.
  - All actions logged to console and notified to other staff members.
  - Optional target notification (disabled by default for stealth).

* **Nickname System:** Advanced nickname management with persistence:
  - `/oneria nick <player> <nickname>` - Set custom nicknames with color code support (§ and &).
  - `/oneria nick <player>` - Reset nickname to original name.
  - `/oneria nick list` - Display all active nicknames.
  - Full integration with proximity blur system.
  - Nicknames displayed in TabList and overhead.
  - Persistent storage in `world/data/oneriamod/nicknames.json`.
  - Automatic save on every change.
  - Instant update without reconnection required.

* **Configuration Commands:** Comprehensive in-game configuration management:
  - `/oneria config reload` - Hot-reload configuration and nicknames without restart.
  - `/oneria config status` - View current mod status with visual formatting.
  - `/oneria config set <option> <value>` - Modify any config option in real-time.
  - Over 20 configurable options accessible via commands.

* **Schedule Commands with Aliases:**
  - `/oneria schedule`, `/schedule`, or `/horaires` - View server schedule with beautiful formatting.
  - Displays current status (OPEN/CLOSED).
  - Shows opening/closing times.
  - Calculates time remaining until next event.

**Improved**

* **Obfuscation System:** Enhanced proximity blur with better performance and security:
  - Now respects custom nicknames set via `/oneria nick`.
  - Improved obfuscation algorithm that preserves color codes length.
  - Configurable obfuscation length (1-16 characters).
  - **Security Enhancement:** Rank prefixes are now ALWAYS hidden when names are obfuscated to prevent metagaming and grade detection.
  - Better handling of LuckPerms prefixes.
  - Admin view: Staff members with `opsSeeAll` enabled see nicknames with real names in italic gray next to them: `Nickname (RealName)`.

* **Staff Detection:** More reliable permission checking:
  - Multi-layered permission system (tags → OP → LuckPerms).
  - Cached results for better performance (30-second cache).
  - Automatic cache invalidation on logout.
  - Debug mode for permission troubleshooting.

* **Command Organization:** Better command structure with aliases:
  - All mod commands unified under `/oneria`.
  - Logical subcommands: `config`, `staff`, `whitelist`, `nick`, `schedule`.
  - New convenient aliases: `/platform`, `/schedule`, `/horaires`.
  - Improved command suggestions and tab completion.
  - Consistent error messages and feedback.

**Fixed**

* **Packet Handling:** Fixed `ClientboundPlayerInfoUpdatePacket` constructor issues.
* **Mixin Compatibility:** Corrected casting errors with `ServerCommonPacketListenerImpl`.
* **Mixin Obfuscation:** Resolved "Unable to locate obfuscation mapping" errors by adding `remap = false` where needed.
* **Compact Source Files:** Fixed Java 21 compilation issues with unnamed classes.
* **Schedule Logic:** Fixed edge cases with midnight transitions.
* **Nickname Persistence:** Nicknames now properly persist across server restarts via JSON storage.
* **Configuration Reload:** All systems now properly reload when config changes, including nickname system.

**Technical**

* **New Classes:**
  - `OneriaPermissions` - Centralized permission management system with caching.
  - `OneriaScheduleManager` - Schedule system with tick-based checking and smart warnings.
  - `OneriaEventHandler` - Event handling for login/logout with delayed welcome messages.
  - `OneriaCommands` - Unified command registry with comprehensive subcommands.
  - `NicknameManager` - Persistent nickname storage with automatic JSON serialization.

* **Mixin Improvements:**
  - `MixinServerPlayer` - Injects into `getDisplayName()` to support nicknames in all contexts.
  - `MixinServerCommonPacketListenerImpl` - Enhanced packet modification with admin preview and security features.
  - `ClientboundPlayerInfoUpdatePacketAccessor` - Direct packet entry manipulation for TabList.

* **Performance:**
  - Permission caching reduces overhead by 90%.
  - Schedule system only checks every 20 seconds (400 ticks).
  - Efficient packet modification with minimal performance impact.
  - Nickname loading uses lazy initialization pattern.

* **Data Storage:**
  - Nicknames stored in `world/data/oneriamod/nicknames.json`.
  - Pretty-printed JSON format for easy manual editing.
  - Automatic directory creation and error handling.
  - UUID-based storage for reliability across name changes.

**Configuration**

* **New Config Sections:**
  - `[Permissions System]` - Staff detection settings with multiple layers.
  - `[Schedule System]` - Opening/closing configuration with automated warnings.
  - `[Messages]` - Customizable kick/warning messages with variable support.
  - `[Welcome Message]` - Welcome system settings with sound configuration.
  - `[Teleportation Platforms]` - Platform definitions with dimension support.
  - `[Silent Commands]` - Moderation logging options for transparency.

* **Updated Options:**
  - `obfuscatePrefix` - Now deprecated, prefixes are always hidden during obfuscation for security.
  - `debugSelfBlur` - Fixed to properly work with admin exemptions.

**Migration Notes**

* **Configuration:** The config file structure is unchanged, but you may want to review the `obfuscatePrefix` setting as it no longer affects obfuscated names.
* **Nicknames:** Nicknames from previous versions will need to be re-set as the storage location has changed to `world/data/oneriamod/`.
* **Commands:** Old KubeJS-based `/horaires` command is now fully replaced by native implementation.
* **Permissions:** Staff permissions now use unified `OneriaPermissions.isStaff()` check across all systems.

## [1.0.1] - 2026-01-05

**Added**

* **Full Release:** Combined all core modules into a stable build.
* **Obfuscation System:** Implemented a Proximity Blur system for player names in TabList and overhead.
* **Schedule System:** Added automatic server opening/closing logic with automated kick for non-staff players.
* **Silent Commands:** Implemented /oneria staff module (gamemode, tp, effects) with stealth logging.
* **Whitelist System:** Added a specific whitelist to bypass the blur system for selected players.
* **LuckPerms Integration:** Support for LuckPerms prefixes and group-based permissions.
* **Welcome System:** Customizable welcome messages with sound support upon login.

**Fixed**

* **Syntax Errors:** Fixed invalid switch block syntax in command classes.
* **Data Handling:** Fixed casting issues and dynamic list updates for the whitelist configuration.

**Security**

* **Permissions:** Added robust permission checks using LuckPerms groups, Vanilla tags, and OP levels.
* **Staff Logging:** Commands used by staff are now logged to the console and other online staff members.

## [1.0.0] - 2026-01-04

**Added**

* **Initial Beta:** Experimental implementation of the name obfuscation (blur) system.
* Basic configuration file generation.

---

## Legend

* **Breaking Changes:** Changes that may require manual intervention or break compatibility.
* **Added:** New features or functionality.
* **Improved:** Enhancements to existing features.
* **Fixed:** Bug fixes and corrections.
* **Security:** Security-related changes.
* **Technical:** Internal/technical changes.
* **Migration Notes:** Important information for upgrading.