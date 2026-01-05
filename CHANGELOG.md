# Changelog - Oneria Mod
All notable changes to this project will be documented in this file.

## [1.0.1] - 2026-05-01

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