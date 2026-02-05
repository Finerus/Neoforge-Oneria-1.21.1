package net.oneria.oneriaserverutilities;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration dédiée aux métiers et restrictions
 * Fichier: serverconfig/oneria-professions.toml
 */
public class ProfessionConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ===============================================================================
    // PROFESSION DEFINITIONS
    // ===============================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSIONS;

    // ===============================================================================
    // GLOBAL RESTRICTIONS (Ce qui est INTERDIT par défaut pour TOUS)
    // ===============================================================================

    // Crafts interdits globalement
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GLOBAL_BLOCKED_CRAFTS;

    // Blocs incassables globalement
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GLOBAL_UNBREAKABLE_BLOCKS;

    // Items inutilisables globalement
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GLOBAL_BLOCKED_ITEMS;

    // Items non équipables globalement (armures, outils)
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GLOBAL_BLOCKED_EQUIPMENT;

    // ===============================================================================
    // PROFESSION OVERRIDES (Ce qui est AUTORISÉ par métier spécifique)
    // ===============================================================================

    // Format: profession;item1,item2,item3
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_ALLOWED_CRAFTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_ALLOWED_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_ALLOWED_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_ALLOWED_EQUIPMENT;

    // ===============================================================================
    // MESSAGES
    // ===============================================================================
    public static final ModConfigSpec.ConfigValue<String> MSG_CRAFT_BLOCKED;
    public static final ModConfigSpec.ConfigValue<String> MSG_BLOCK_BREAK_BLOCKED;
    public static final ModConfigSpec.ConfigValue<String> MSG_ITEM_USE_BLOCKED;
    public static final ModConfigSpec.ConfigValue<String> MSG_EQUIPMENT_BLOCKED;

    static {
        BUILDER.comment(
                "═══════════════════════════════════════════════════════════════",
                "  ONERIA - PROFESSION & RESTRICTION SYSTEM",
                "═══════════════════════════════════════════════════════════════",
                "",
                "This configuration file manages:",
                "  - Profession definitions (name, color, display)",
                "  - Global restrictions (crafts, blocks, items, equipment)",
                "  - Profession-specific permissions (overrides)",
                "",
                "Format for profession definitions:",
                "  id;DisplayName;ColorCode",
                "  Example: chasseur;Chasseur;§a (green hunter)",
                "",
                "Format for restrictions:",
                "  - Use Minecraft resource locations (namespace:path)",
                "  - Example: minecraft:diamond_pickaxe",
                "  - Supports wildcards: minecraft:*_sword (all swords)",
                "",
                "Format for profession overrides:",
                "  profession_id;item1,item2,item3",
                "  Example: mineur;minecraft:iron_pickaxe,minecraft:diamond_pickaxe",
                "",
                "═══════════════════════════════════════════════════════════════"
        ).push("Profession System");

        // -----------------------------------------------------------------------
        // PROFESSION DEFINITIONS
        // -----------------------------------------------------------------------
        BUILDER.comment(
                "",
                "═══════════════════════════════════════",
                "  PROFESSION DEFINITIONS",
                "═══════════════════════════════════════",
                "",
                "Define all available professions with their display properties.",
                "Format: id;DisplayName;ColorCode",
                "",
                "Available color codes:",
                "  §0 = Black       §1 = Dark Blue    §2 = Dark Green",
                "  §3 = Dark Aqua   §4 = Dark Red     §5 = Dark Purple",
                "  §6 = Gold        §7 = Gray         §8 = Dark Gray",
                "  §9 = Blue        §a = Green        §b = Aqua",
                "  §c = Red         §d = Light Purple §e = Yellow",
                "  §f = White",
                "",
                "Examples:",
                "  chasseur;Chasseur;§a           (Green hunter)",
                "  forgeron;Forgeron;§c           (Red blacksmith)",
                "  alchimiste;Alchimiste;§5       (Purple alchemist)"
        );

        PROFESSIONS = BUILDER
                .defineList("professions",
                        Arrays.asList(
                                "chasseur;Chasseur;§a",
                                "pecheur;Pêcheur;§b",
                                "mineur;Mineur;§8",
                                "bucheron;Bûcheron;§6",
                                "forgeron;Forgeron;§c",
                                "alchimiste;Alchimiste;§5",
                                "marchand;Marchand;§e",
                                "garde;Garde;§9"
                        ),
                        obj -> obj instanceof String && ((String) obj).split(";").length == 3
                );

        BUILDER.pop();

        // -----------------------------------------------------------------------
        // GLOBAL RESTRICTIONS
        // -----------------------------------------------------------------------
        BUILDER.comment(
                "",
                "═══════════════════════════════════════",
                "  GLOBAL RESTRICTIONS",
                "═══════════════════════════════════════",
                "",
                "Define what is BLOCKED for ALL players by default.",
                "Players with specific professions can override these via",
                "the profession override lists below.",
                "",
                "Use Minecraft resource locations (namespace:item_name)",
                "Supports wildcards with * at the end of namespace or path."
        ).push("Global Restrictions");

        GLOBAL_BLOCKED_CRAFTS = BUILDER
                .comment(
                        "",
                        "Crafts that are blocked for everyone (unless overridden by profession).",
                        "",
                        "Examples:",
                        "  minecraft:diamond_sword      - Block diamond sword crafting",
                        "  minecraft:*_pickaxe          - Block all pickaxes",
                        "  minecraft:iron_*             - Block all iron items"
                )
                .defineList("globalBlockedCrafts",
                        Arrays.asList(
                                "minecraft:diamond_sword",
                                "minecraft:diamond_pickaxe",
                                "minecraft:diamond_axe",
                                "minecraft:netherite_sword",
                                "minecraft:netherite_pickaxe"
                        ),
                        obj -> obj instanceof String
                );

        GLOBAL_UNBREAKABLE_BLOCKS = BUILDER
                .comment(
                        "",
                        "Blocks that cannot be broken by anyone (unless overridden by profession).",
                        "",
                        "Examples:",
                        "  minecraft:diamond_ore        - Cannot mine diamond ore",
                        "  minecraft:ancient_debris     - Cannot mine ancient debris",
                        "  minecraft:*_ore              - Cannot mine any ore"
                )
                .defineList("globalUnbreakableBlocks",
                        Arrays.asList(
                                "minecraft:diamond_ore",
                                "minecraft:deepslate_diamond_ore",
                                "minecraft:iron_ore",
                                "minecraft:deepslate_iron_ore",
                                "minecraft:gold_ore",
                                "minecraft:deepslate_gold_ore"
                        ),
                        obj -> obj instanceof String
                );

        GLOBAL_BLOCKED_ITEMS = BUILDER
                .comment(
                        "",
                        "Items that cannot be used/interacted with (right-click blocked).",
                        "",
                        "Examples:",
                        "  minecraft:flint_and_steel    - Cannot use flint and steel",
                        "  minecraft:*_bucket           - Cannot use any bucket"
                )
                .defineList("globalBlockedItems",
                        Arrays.asList(
                                "minecraft:flint_and_steel",
                                "minecraft:fire_charge"
                        ),
                        obj -> obj instanceof String
                );

        GLOBAL_BLOCKED_EQUIPMENT = BUILDER
                .comment(
                        "",
                        "Equipment (armor, tools, weapons) that cannot be equipped/held.",
                        "",
                        "Examples:",
                        "  minecraft:diamond_sword      - Cannot equip diamond sword",
                        "  minecraft:diamond_helmet     - Cannot wear diamond helmet",
                        "  minecraft:diamond_*          - Cannot use any diamond equipment"
                )
                .defineList("globalBlockedEquipment",
                        Arrays.asList(
                                "minecraft:diamond_sword",
                                "minecraft:diamond_helmet",
                                "minecraft:diamond_chestplate",
                                "minecraft:diamond_leggings",
                                "minecraft:diamond_boots"
                        ),
                        obj -> obj instanceof String
                );

        BUILDER.pop();

        // -----------------------------------------------------------------------
        // PROFESSION OVERRIDES
        // -----------------------------------------------------------------------
        BUILDER.comment(
                "",
                "═══════════════════════════════════════",
                "  PROFESSION OVERRIDES",
                "═══════════════════════════════════════",
                "",
                "Define what each profession is ALLOWED to do despite global restrictions.",
                "Format: profession_id;item1,item2,item3",
                "",
                "The profession_id must match the id from the profession definitions above.",
                "",
                "Examples:",
                "  mineur;minecraft:iron_pickaxe,minecraft:diamond_pickaxe",
                "  forgeron;minecraft:diamond_sword,minecraft:anvil",
                "  bucheron;minecraft:iron_axe,minecraft:diamond_axe"
        ).push("Profession Overrides");

        PROFESSION_ALLOWED_CRAFTS = BUILDER
                .comment(
                        "",
                        "Crafts that each profession IS ALLOWED to make.",
                        "Overrides global craft restrictions."
                )
                .defineList("professionAllowedCrafts",
                        Arrays.asList(
                                "mineur;minecraft:iron_pickaxe,minecraft:diamond_pickaxe",
                                "forgeron;minecraft:diamond_sword,minecraft:iron_sword,minecraft:anvil",
                                "bucheron;minecraft:iron_axe,minecraft:diamond_axe"
                        ),
                        obj -> obj instanceof String && ((String) obj).contains(";")
                );

        PROFESSION_ALLOWED_BLOCKS = BUILDER
                .comment(
                        "",
                        "Blocks that each profession IS ALLOWED to break.",
                        "Overrides global unbreakable blocks."
                )
                .defineList("professionAllowedBlocks",
                        Arrays.asList(
                                "mineur;minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:iron_ore,minecraft:gold_ore",
                                "bucheron;minecraft:oak_log,minecraft:birch_log,minecraft:spruce_log"
                        ),
                        obj -> obj instanceof String && ((String) obj).contains(";")
                );

        PROFESSION_ALLOWED_ITEMS = BUILDER
                .comment(
                        "",
                        "Items that each profession IS ALLOWED to use.",
                        "Overrides global blocked items."
                )
                .defineList("professionAllowedItems",
                        Arrays.asList(
                                "alchimiste;minecraft:brewing_stand,minecraft:cauldron",
                                "garde;minecraft:flint_and_steel"
                        ),
                        obj -> obj instanceof String && ((String) obj).contains(";")
                );

        PROFESSION_ALLOWED_EQUIPMENT = BUILDER
                .comment(
                        "",
                        "Equipment that each profession IS ALLOWED to use/wear.",
                        "Overrides global blocked equipment."
                )
                .defineList("professionAllowedEquipment",
                        Arrays.asList(
                                "garde;minecraft:diamond_sword,minecraft:diamond_helmet,minecraft:diamond_chestplate",
                                "mineur;minecraft:diamond_pickaxe,minecraft:iron_pickaxe"
                        ),
                        obj -> obj instanceof String && ((String) obj).contains(";")
                );

        BUILDER.pop();

        // -----------------------------------------------------------------------
        // MESSAGES
        // -----------------------------------------------------------------------
        BUILDER.comment(
                "",
                "═══════════════════════════════════════",
                "  MESSAGES",
                "═══════════════════════════════════════",
                "",
                "Customize messages shown to players when actions are blocked.",
                "Variables available:",
                "  {item}       - The item/block/craft name",
                "  {profession} - Required profession(s)"
        ).push("Messages");

        MSG_CRAFT_BLOCKED = BUILDER
                .comment("Message shown when a craft is blocked")
                .define("craftBlockedMessage",
                        "§c✘ Vous ne pouvez pas crafter cet objet ! §7Métier requis: §e{profession}");

        MSG_BLOCK_BREAK_BLOCKED = BUILDER
                .comment("Message shown when block breaking is blocked")
                .define("blockBreakBlockedMessage",
                        "§c✘ Vous ne pouvez pas casser ce bloc ! §7Métier requis: §e{profession}");

        MSG_ITEM_USE_BLOCKED = BUILDER
                .comment("Message shown when item usage is blocked")
                .define("itemUseBlockedMessage",
                        "§c✘ Vous ne pouvez pas utiliser cet objet ! §7Métier requis: §e{profession}");

        MSG_EQUIPMENT_BLOCKED = BUILDER
                .comment("Message shown when equipment is blocked")
                .define("equipmentBlockedMessage",
                        "§c✘ Vous ne pouvez pas équiper cet objet ! §7Métier requis: §e{profession}");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}