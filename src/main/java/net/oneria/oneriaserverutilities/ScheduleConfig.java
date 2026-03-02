package net.oneria.oneriaserverutilities;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class ScheduleConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // === SCHEDULE SYSTEM ===
    public static final ModConfigSpec.BooleanValue ENABLE_SCHEDULE;
    public static final ModConfigSpec.ConfigValue<String> OPENING_TIME;
    public static final ModConfigSpec.ConfigValue<String> CLOSING_TIME;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WARNING_TIMES;
    public static final ModConfigSpec.BooleanValue KICK_NON_STAFF;

    // === SCHEDULE MESSAGES ===
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

    static {
        // ===============================================================================
        // CATEGORY: SCHEDULE SYSTEM
        // ===============================================================================
        BUILDER.push("Schedule System");

        ENABLE_SCHEDULE = BUILDER
                .comment("Enable the server schedule system.",
                        "When enabled, the server will only allow players during opening hours.")
                .define("enableSchedule", false);

        OPENING_TIME = BUILDER
                .comment("Server opening time in HH:MM format (24h).",
                        "Example: 19:00")
                .define("openingTime", "19:00");

        CLOSING_TIME = BUILDER
                .comment("Server closing time in HH:MM format (24h).",
                        "Example: 23:59")
                .define("closingTime", "23:59");

        WARNING_TIMES = BUILDER
                .comment("List of minutes before closing to warn players.",
                        "Example: [30, 15, 5, 1] will warn at 30min, 15min, 5min and 1min before closing.")
                .defineList("warningTimes", Arrays.asList(30, 15, 5, 1), obj -> obj instanceof Integer);

        KICK_NON_STAFF = BUILDER
                .comment("If 'true', non-staff players are kicked when the server closes.")
                .define("kickNonStaff", true);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: SCHEDULE MESSAGES
        // ===============================================================================
        BUILDER.push("Schedule Messages");

        MSG_SERVER_CLOSED = BUILDER
                .comment("Message sent to players when the server closes.",
                        "Variables: {time}")
                .define("msgServerClosed", "§c§lThe server is now closed. See you tomorrow at {time}!");

        MSG_SERVER_OPENED = BUILDER
                .comment("Message broadcast when the server opens.",
                        "Variables: {time}")
                .define("msgServerOpened", "§a§lThe server is now open! Welcome!");

        MSG_WARNING = BUILDER
                .comment("Warning message sent before closing.",
                        "Variables: {minutes}, {time}")
                .define("msgWarning", "§e§lWarning: §r§eThe server closes in §c{minutes} minutes §e(at {time}).");

        MSG_CLOSING_IMMINENT = BUILDER
                .comment("Message sent when closing is imminent (last warning).",
                        "Variables: {minutes}")
                .define("msgClosingImminent", "§c§lThe server closes in {minutes} minute(s)! Finish what you're doing!");

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: WELCOME MESSAGE
        // ===============================================================================
        BUILDER.push("Welcome Message");

        ENABLE_WELCOME = BUILDER
                .comment("Enable the welcome message shown to players on join.")
                .define("enableWelcome", true);

        WELCOME_LINES = BUILDER
                .comment("Lines of the welcome message. Supports color codes (§ or &).",
                        "Each entry is a separate line.")
                .defineList("welcomeLines", Arrays.asList(
                        "§6§m------------------------------------",
                        "§e§lWelcome to the server!",
                        "§7Enjoy your stay.",
                        "§6§m------------------------------------"
                ), obj -> obj instanceof String);

        WELCOME_SOUND = BUILDER
                .comment("Sound played on join. Use a valid Minecraft sound ID.",
                        "Example: minecraft:ui.toast.challenge_complete",
                        "Set to 'none' to disable.")
                .define("welcomeSound", "minecraft:ui.toast.challenge_complete");

        WELCOME_SOUND_VOLUME = BUILDER
                .comment("Volume of the welcome sound (0.0 to 1.0).")
                .defineInRange("welcomeSoundVolume", 1.0, 0.0, 1.0);

        WELCOME_SOUND_PITCH = BUILDER
                .comment("Pitch of the welcome sound (0.5 to 2.0).")
                .defineInRange("welcomeSoundPitch", 1.0, 0.5, 2.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
