package net.oneria.oneriamod;

import net.neoforged.neoforge.common.ModConfigSpec;

// Cette classe définit et stocke les spécifications de configuration.
// Les valeurs seront lues depuis le fichier 'config/oneria-common.toml'.
public class Config {

    public static final ModConfigSpec SPEC;

    // -- Section Tab List Obfuscation --
    public static final ModConfigSpec.BooleanValue ENABLE_TAB_OBFUSCATION;
    public static final ModConfigSpec.BooleanValue OBFUSCATED_NAME_BOLD;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_TAB_PREFIX;

    static {
        // Initialisation du constructeur de spécification de configuration
        ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        BUILDER.comment("Configuration des fonctionnalités d'ONERIA").push("tab_obfuscation");

        ENABLE_TAB_OBFUSCATION = BUILDER
                .comment("Active ou désactive l'obfuscation (§k) des noms des joueurs dans la Tab List (Appuyez sur Tab).")
                .define("enableTabObfuscation", true); // Valeur par défaut: true

        OBFUSCATED_NAME_BOLD = BUILDER
                .comment("Active ou désactive le style gras sur le nom obfusqué.")
                .define("obfuscatedNameBold", true); // Valeur par défaut: true

        CUSTOM_TAB_PREFIX = BUILDER
                .comment("Ajoute un préfixe personnalisé devant le nom du joueur dans la Tab List. Laissez vide pour aucun préfixe. Supporte les codes couleur Minecraft (§c, §l, etc.).")
                .define("customTabPrefix", "[ONERIA] "); // Valeur par défaut: "[ONERIA] "

        BUILDER.pop(); // Fin de la section tab_obfuscation

        SPEC = BUILDER.build();
    }
}