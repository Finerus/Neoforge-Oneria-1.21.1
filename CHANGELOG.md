# Changelog - Oneria Mod
All notable changes to this project will be documented in this file.

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
* **LuckPerms Integration:** Support for LuckPerms prefixes and group-based staff permissions.
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