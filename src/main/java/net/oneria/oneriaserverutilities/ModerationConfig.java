package net.oneria.oneriaserverutilities;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class ModerationConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // === MODERATION (Silent Commands) ===
    public static final ModConfigSpec.BooleanValue ENABLE_SILENT_COMMANDS;
    public static final ModConfigSpec.BooleanValue LOG_TO_STAFF;
    public static final ModConfigSpec.BooleanValue LOG_TO_CONSOLE;
    public static final ModConfigSpec.BooleanValue NOTIFY_TARGET;

    // === PLATFORMS ===
    public static final ModConfigSpec.BooleanValue ENABLE_PLATFORMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PLATFORMS;

    static {
        // ===============================================================================
        // CATEGORY: SILENT COMMANDS
        // ===============================================================================
        BUILDER.push("Silent Commands");

        ENABLE_SILENT_COMMANDS = BUILDER
                .comment("Enable /oneria staff gm/tp/effect commands.")
                .define("enableSilentCommands", true);

        LOG_TO_STAFF = BUILDER
                .comment("Notify other staff members when a silent command is used.")
                .define("logToStaff", true);

        LOG_TO_CONSOLE = BUILDER
                .comment("Log silent commands to the server console.")
                .define("logToConsole", true);

        NOTIFY_TARGET = BUILDER
                .comment("If 'true', the target receives a message (useful for debug, otherwise leave false).")
                .define("notifyTarget", false);

        BUILDER.pop();

        // ===============================================================================
        // CATEGORY: TELEPORTATION PLATFORMS
        // ===============================================================================
        BUILDER.push("Teleportation Platforms");

        ENABLE_PLATFORMS = BUILDER
                .comment("Enable the /oneria staff platform command.")
                .define("enablePlatforms", true);

        PLATFORMS = BUILDER
                .comment("List of TP platforms.",
                        "Format: id;DisplayName;dimension;x;y;z",
                        "Example: spawn;The Spawn;minecraft:overworld;0;100;0")
                .defineList("platforms", Arrays.asList("platform1;Platform 1;oneria:quartier;7217;18;-1321"), obj -> obj instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
