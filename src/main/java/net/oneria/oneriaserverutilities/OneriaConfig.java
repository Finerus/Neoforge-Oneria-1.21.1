package net.oneria.oneriaserverutilities;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class OneriaConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // === OBFUSCATION CONFIGURATION ===
    public static final ModConfigSpec.IntValue PROXIMITY_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_BLUR;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_VISIBLE_LIST;
    public static final ModConfigSpec.BooleanValue BLUR_SPECTATORS;
    public static final ModConfigSpec.BooleanValue WHITELIST_EXEMPT_PROFESSIONS;
    public static final ModConfigSpec.IntValue OBFUSCATED_NAME_LENGTH;
    public static final ModConfigSpec.BooleanValue OBFUSCATE_PREFIX;
    public static final ModConfigSpec.BooleanValue ENABLE_SNEAK_STEALTH;
    public static final ModConfigSpec.IntValue SNEAK_PROXIMITY_DISTANCE;
    public static final ModConfigSpec.BooleanValue OPS_SEE_ALL;
    public static final ModConfigSpec.BooleanValue DEBUG_SELF_BLUR;
    public static final ModConfigSpec.BooleanValue HIDE_NAMETAGS;
    public static final ModConfigSpec.BooleanValue SHOW_NAMETAG_PREFIX_SUFFIX;

    // === PERMISSIONS SYSTEM ===
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STAFF_TAGS;
    public static final ModConfigSpec.IntValue OP_LEVEL_BYPASS;
    public static final ModConfigSpec.BooleanValue USE_LUCKPERMS_GROUPS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LUCKPERMS_STAFF_GROUPS;

    // === WORLD BORDER & ZONES ===
    public static final ModConfigSpec.BooleanValue ENABLE_WORLD_BORDER_WARNING;
    public static final ModConfigSpec.IntValue WORLD_BORDER_DISTANCE;
    public static final ModConfigSpec.ConfigValue<String> WORLD_BORDER_MESSAGE;
    public static final ModConfigSpec.IntValue WORLD_BORDER_CHECK_INTERVAL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NAMED_ZONES;
    public static final ModConfigSpec.ConfigValue<String> ZONE_MESSAGE_MODE;

    static {
        // ===============================================================================
        // CATEGORY: OBFUSCATION (TabList & Nametags)
        // ===============================================================================
        BUILDER.push("Obfuscation Settings");

        PROXIMITY_DISTANCE = BUILDER
                .comment("CONFIGURATION: Proximity Distance",
                        "The distance (in blocks) required to see another player's name clearly.",
                        "If the player is further away, the name will be replaced by '???'.",
                        "Default value: 8 blocks.")
                .defineInRange("proximityDistance", 8, 1, 128);

        ENABLE_BLUR = BUILDER
                .comment("CONFIGURATION: Blur Effect",
                        "Enables or disables the name blurring system.",
                        "Setting this to 'false' completely disables the client-side visual effect.")
                .define("enableBlur", true);

        OBFUSCATED_NAME_LENGTH = BUILDER
                .comment("CONFIGURATION: Hidden Name Length",
                        "Number of characters used to replace the name (e.g., 5 results in '?????').")
                .defineInRange("obfuscatedNameLength", 5, 1, 16);

        OBFUSCATE_PREFIX = BUILDER
                .comment("CONFIGURATION: Hide Ranks/Prefixes",
                        "If 'true', prefixes (ranks, titles) will also be hidden in the TabList.",
                        "Recommended for strict RP.")
                .define("obfuscatePrefix", true);

        ENABLE_SNEAK_STEALTH = BUILDER
                .comment("CONFIGURATION: Sneak Stealth Mode",
                        "If 'true', players who are sneaking become harder to detect.",
                        "Their name will only be visible at a much closer distance.")
                .define("enableSneakStealth", true);

        SNEAK_PROXIMITY_DISTANCE = BUILDER
                .comment("CONFIGURATION: Sneak Detection Distance",
                        "The distance (in blocks) at which sneaking players can be detected.",
                        "Only applies when enableSneakStealth is true.",
                        "Default value: 2 blocks.")
                .defineInRange("sneakProximityDistance", 2, 1, 32);

        OPS_SEE_ALL = BUILDER
                .comment("CONFIGURATION: Admin View",
                        "If 'true', operators and staff always see all names clearly.")
                .define("opsSeeAll", true);

        DEBUG_SELF_BLUR = BUILDER
                .comment("DEBUG: Self-Blur",
                        "If 'true', applies the blur to yourself for testing purposes.",
                        "Never leave enabled in production.")
                .define("debugSelfBlur", false);

        HIDE_NAMETAGS = BUILDER
                .comment("CONFIGURATION: Hide Nametags",
                        "If 'true', hides all player nametags above their heads.",
                        "Uses scoreboard teams to hide names server-side.")
                .define("hideNametags", false);

        SHOW_NAMETAG_PREFIX_SUFFIX = BUILDER
                .comment("CONFIGURATION: Show Prefix/Suffix on Nametags",
                        "If 'true', displays LuckPerms prefix/suffix above player heads.")
                .define("showNametagPrefixSuffix", true);

        WHITELIST = BUILDER
                .comment("WHITELIST: Immune Players",
                        "List of usernames that always see everything clearly, even without OP.",
                        "These players are never obfuscated for others either.")
                .defineList("whitelist", List.of(), obj -> obj instanceof String);

        BLACKLIST = BUILDER
                .comment("BLACKLIST: Always Hidden Players",
                        "List of usernames that are always obfuscated, regardless of proximity.")
                .defineList("blacklist", List.of(), obj -> obj instanceof String);

        ALWAYS_VISIBLE_LIST = BUILDER
                .comment("ALWAYS VISIBLE: Always Shown in TabList",
                        "List of usernames that are always shown clearly in the TabList.")
                .defineList("alwaysVisibleList", List.of(), obj -> obj instanceof String);

        BLUR_SPECTATORS = BUILDER
                .comment("CONFIGURATION: Blur Spectators",
                        "If 'true', spectators are also subject to name blurring.")
                .define("blurSpectators", false);

        WHITELIST_EXEMPT_PROFESSIONS = BUILDER
                .comment("CONFIGURATION: Whitelist Exempts Profession Restrictions",
                        "If 'true', players in the whitelist are also exempt from profession restrictions.")
                .define("whitelistExemptProfessions", false);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: PERMISSIONS
        // ===============================================================================
        BUILDER.push("Permissions System");

        STAFF_TAGS = BUILDER
                .comment("List of LuckPerms tags/groups considered as 'staff'.",
                        "Used to determine who receives staff notifications.")
                .defineList("staffTags", Arrays.asList("admin", "moderateur", "modo", "staff", "builder"), obj -> obj instanceof String);

        OP_LEVEL_BYPASS = BUILDER
                .comment("Minimum OP level required to bypass all restrictions.",
                        "Set to 0 to disable OP bypass entirely.")
                .defineInRange("opLevelBypass", 2, 0, 4);

        USE_LUCKPERMS_GROUPS = BUILDER
                .comment("If 'true', uses LuckPerms groups to determine staff status.",
                        "If 'false', uses OP level only.")
                .define("useLuckPermsGroups", true);

        LUCKPERMS_STAFF_GROUPS = BUILDER
                .comment("LuckPerms groups considered as staff.",
                        "Only used if useLuckPermsGroups is true.")
                .defineList("luckPermsStaffGroups", Arrays.asList("admin", "moderateur", "staff"), obj -> obj instanceof String);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: WORLD BORDER & ZONES
        // ===============================================================================
        BUILDER.push("World Border Warning");

        ENABLE_WORLD_BORDER_WARNING = BUILDER
                .comment("Enable warning when players reach world border distance.")
                .define("enableWorldBorderWarning", true);

        WORLD_BORDER_DISTANCE = BUILDER
                .comment("Distance from spawn (in blocks) before warning is triggered.")
                .defineInRange("worldBorderDistance", 2000, 100, 100000);

        WORLD_BORDER_MESSAGE = BUILDER
                .comment("Message displayed when player reaches border.",
                        "Variables: {distance}, {player}")
                .define("worldBorderMessage", "§c§l⚠ WARNING §r§7You've reached the limit of the world! (§c{distance} blocks§7)");

        WORLD_BORDER_CHECK_INTERVAL = BUILDER
                .comment("Check interval in ticks (20 ticks = 1 second).",
                        "Higher values = less frequent checks = better performance.")
                .defineInRange("worldBorderCheckInterval", 40, 20, 200);

        NAMED_ZONES = BUILDER
                .comment("Named zones that trigger a message when entered/exited.",
                        "Format: name;centerX;centerZ;radius;messageEnter;messageExit",
                        "Example: Village;100;200;150;§aBienvenue au Village!;§7Vous quittez le Village.")
                .defineList("namedZones", Arrays.asList(), obj -> obj instanceof String);

        ZONE_MESSAGE_MODE = BUILDER
                .comment("Display mode for zone and world border messages.",
                        "IMMERSIVE = ImmersiveMessageAPI overlay (requires client mod).",
                        "CHAT = Standard chat message.",
                        "ACTION_BAR = Action bar (vanilla, no client mod needed).")
                .define("zoneMessageMode", "ACTION_BAR");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
