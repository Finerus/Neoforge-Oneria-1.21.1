package net.oneria.oneriamod;

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
    public static final ModConfigSpec.IntValue OBFUSCATED_NAME_LENGTH;
    public static final ModConfigSpec.BooleanValue OBFUSCATE_PREFIX;
    public static final ModConfigSpec.BooleanValue OPS_SEE_ALL;
    public static final ModConfigSpec.BooleanValue DEBUG_SELF_BLUR;

    // === PERMISSIONS SYSTEM ===
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STAFF_TAGS;
    public static final ModConfigSpec.IntValue OP_LEVEL_BYPASS;
    public static final ModConfigSpec.BooleanValue USE_LUCKPERMS_GROUPS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LUCKPERMS_STAFF_GROUPS;

    // === SCHEDULE SYSTEM ===
    public static final ModConfigSpec.BooleanValue ENABLE_SCHEDULE;
    public static final ModConfigSpec.ConfigValue<String> OPENING_TIME;
    public static final ModConfigSpec.ConfigValue<String> CLOSING_TIME;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WARNING_TIMES;
    public static final ModConfigSpec.BooleanValue KICK_NON_STAFF;

    // === MESSAGES ===
    public static final ModConfigSpec.ConfigValue<String> MSG_SERVER_CLOSED;
    public static final ModConfigSpec.ConfigValue<String> MSG_SERVER_OPENED;
    public static final ModConfigSpec.ConfigValue<String> MSG_WARNING;
    public static final ModConfigSpec.ConfigValue<String> MSG_CLOSING_IMMINENT;

    // === WELCOME ===
    public static final ModConfigSpec.BooleanValue ENABLE_WELCOME;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WELCOME_LINES;
    public static final ModConfigSpec.ConfigValue<String> WELCOME_SOUND;
    public static final ModConfigSpec.DoubleValue WELCOME_SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue WELCOME_SOUND_PITCH;

    // === PLATFORMS ===
    public static final ModConfigSpec.BooleanValue ENABLE_PLATFORMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PLATFORMS;

    // === MODERATION (Silent Commands) ===
    public static final ModConfigSpec.BooleanValue ENABLE_SILENT_COMMANDS;
    public static final ModConfigSpec.BooleanValue LOG_TO_STAFF;
    public static final ModConfigSpec.BooleanValue LOG_TO_CONSOLE;
    public static final ModConfigSpec.BooleanValue NOTIFY_TARGET;

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

        OPS_SEE_ALL = BUILDER
                .comment("CONFIGURATION: Admin View",
                        "If 'true', operators and staff always see all names clearly.")
                .define("opsSeeAll", true);

        DEBUG_SELF_BLUR = BUILDER
                .comment("DEBUG: Self-Blur",
                        "If 'true', applies the blur to yourself for testing purposes.",
                        "Never leave enabled in production.")
                .define("debugSelfBlur", false);

        WHITELIST = BUILDER
                .comment("WHITELIST: Immune Players",
                        "List of usernames that always see everything clearly, even without OP.",
                        "Format: A list of strings.")
                .defineList("whitelist", Arrays.asList("AdminPlayer", "Moderator"), obj -> obj instanceof String);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: PERMISSIONS
        // ===============================================================================
        BUILDER.push("Permissions System");

        STAFF_TAGS = BUILDER
                .comment("List of tags (scoreboard tags) considered as Staff.",
                        "Example: /tag add PlayerName admin")
                .defineList("staffTags", Arrays.asList("admin", "modo", "staff", "builder"), obj -> obj instanceof String);

        OP_LEVEL_BYPASS = BUILDER
                .comment("Minimum OP level to be considered Staff (0 = disabled, 4 = server admin).")
                .defineInRange("opLevelBypass", 2, 0, 4);

        USE_LUCKPERMS_GROUPS = BUILDER
                .comment("Enable LuckPerms integration to detect staff via their groups.")
                .define("useLuckPermsGroups", true);

        LUCKPERMS_STAFF_GROUPS = BUILDER
                .comment("List of LuckPerms groups considered as Staff.")
                .defineList("luckPermsStaffGroups", Arrays.asList("admin", "moderator", "mod"), obj -> obj instanceof String);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: SCHEDULE SYSTEM
        // ===============================================================================
        BUILDER.push("Schedule System");

        ENABLE_SCHEDULE = BUILDER.comment("Enable automatic opening/closing system.").define("enableSchedule", true);
        OPENING_TIME = BUILDER.comment("Opening time (Format HH:MM).").define("openingTime", "19:00");
        CLOSING_TIME = BUILDER.comment("Closing time (Format HH:MM).").define("closingTime", "23:59");
        WARNING_TIMES = BUILDER.comment("Minutes before closing to send a warning.").defineList("warningTimes", Arrays.asList(45, 30, 10, 1), obj -> obj instanceof Integer);
        KICK_NON_STAFF = BUILDER.comment("If 'true', kicks non-staff players at closing time.").define("kickNonStaff", true);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: MESSAGES
        // ===============================================================================
        BUILDER.push("Messages");

        MSG_SERVER_CLOSED = BUILDER.comment("Message displayed upon kick (Supports § color codes).").define("serverClosedMessage", "§c§l[SERVER CLOSED]");
        MSG_SERVER_OPENED = BUILDER.comment("Message sent to staff upon opening.").define("serverOpenedMessage", "§a§l[SERVER OPENED]");
        MSG_WARNING = BUILDER.comment("Warning message ({minutes} is automatically replaced).").define("warningMessage", "§e⚠ Warning! Closing in {minutes} min.");
        MSG_CLOSING_IMMINENT = BUILDER.comment("Final message 1 min before closing.").define("closingImminentMessage", "§c⚠ CLOSING IMMINENT!");

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: WELCOME
        // ===============================================================================
        BUILDER.push("Welcome Message");

        ENABLE_WELCOME = BUILDER.comment("Display welcome message on connection.").define("enableWelcome", true);
        WELCOME_LINES = BUILDER.comment("Message lines. Variables: {player}, {nickname}.").defineList("welcomeLines", Arrays.asList("Welcome {nickname}"), obj -> obj instanceof String);
        WELCOME_SOUND = BUILDER.comment("Sound played on connection (leave empty for none).").define("welcomeSound", "minecraft:entity.player.levelup");
        WELCOME_SOUND_VOLUME = BUILDER.defineInRange("welcomeSoundVolume", 0.5, 0.0, 1.0);
        WELCOME_SOUND_PITCH = BUILDER.defineInRange("welcomeSoundPitch", 1.0, 0.5, 2.0);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: PLATFORMS (Staff TP)
        // ===============================================================================
        BUILDER.push("Teleportation Platforms");

        ENABLE_PLATFORMS = BUILDER.comment("Enable the /oneria staff platform command.").define("enablePlatforms", true);
        PLATFORMS = BUILDER
                .comment("List of TP platforms.",
                        "Format: id;DisplayName;dimension;x;y;z",
                        "Example: spawn;The Spawn;minecraft:overworld;0;100;0")
                .defineList("platforms", Arrays.asList("platform1;Platform 1;oneria:quartier;7217;18;-1321"), obj -> obj instanceof String);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: MODERATION (Silent Commands)
        // ===============================================================================
        BUILDER.push("Silent Commands");

        ENABLE_SILENT_COMMANDS = BUILDER.comment("Enable /oneria staff gm/tp/effect commands.").define("enableSilentCommands", true);
        LOG_TO_STAFF = BUILDER.comment("Notify other staff members when a silent command is used.").define("logToStaff", true);
        LOG_TO_CONSOLE = BUILDER.comment("Log silent commands to the server console.").define("logToConsole", true);
        NOTIFY_TARGET = BUILDER.comment("If 'true', the target receives a message (useful for debug, otherwise leave false).").define("notifyTarget", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}