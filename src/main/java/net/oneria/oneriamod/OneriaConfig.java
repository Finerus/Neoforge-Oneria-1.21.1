package net.oneria.oneriamod;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Arrays;
import java.util.List;

public class OneriaConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue PROXIMITY_DISTANCE;
    public static final ModConfigSpec.IntValue OBFUSCATED_NAME_LENGTH;
    public static final ModConfigSpec.BooleanValue ENABLE_BLUR;
    public static final ModConfigSpec.BooleanValue OPS_SEE_ALL;
    public static final ModConfigSpec.BooleanValue DEBUG_SELF_BLUR;
    public static final ModConfigSpec.BooleanValue OBFUSCATE_PREFIX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;

    static {
        BUILDER.push("Oneria TabList Settings");

        PROXIMITY_DISTANCE = BUILDER
                .comment("Distance en blocs pour voir le pseudo en clair")
                .defineInRange("proximityDistance", 8, 1, 128);

        OBFUSCATED_NAME_LENGTH = BUILDER
                .comment("Longueur du pseudo obfusqué (nombre de caractères)")
                .defineInRange("obfuscatedNameLength", 6, 1, 16);

        ENABLE_BLUR = BUILDER
                .comment("Activer le flou dynamique")
                .define("enableBlur", true);

        OPS_SEE_ALL = BUILDER
                .comment("Les OPs peuvent voir tous les pseudos en clair (true = oui, false = non)")
                .define("opsSeeAll", true);

        DEBUG_SELF_BLUR = BUILDER
                .comment("Mode debug : S'obfusquer soi-même pour tester (true = oui, false = non)")
                .define("debugSelfBlur", false);

        OBFUSCATE_PREFIX = BUILDER
                .comment("Obfusquer aussi le grade/préfixe (true = oui, false = non)")
                .define("obfuscatePrefix", true);

        WHITELIST = BUILDER
                .comment("Joueurs qui voient toujours tout en clair")
                .defineList("whitelist", Arrays.asList("AdminPlayer", "Moderator"), obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}