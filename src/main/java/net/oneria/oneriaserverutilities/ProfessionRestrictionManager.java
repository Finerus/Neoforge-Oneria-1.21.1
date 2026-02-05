package net.oneria.oneriaserverutilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Gestionnaire des restrictions et permissions par métier
 */
public class ProfessionRestrictionManager {

    // Cache pour optimiser les performances
    private static final Map<UUID, Set<String>> playerProfessionsCache = new HashMap<>();
    private static final Map<String, ProfessionData> professionDataCache = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000; // 30 secondes

    /**
     * Classe interne pour stocker les données d'un métier
     */
    public static class ProfessionData {
        public final String id;
        public final String displayName;
        public final String colorCode;

        public ProfessionData(String id, String displayName, String colorCode) {
            this.id = id;
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public String getFormattedName() {
            return colorCode + displayName;
        }
    }

    /**
     * Recharge le cache des métiers depuis la config
     */
    public static void reloadCache() {
        professionDataCache.clear();
        playerProfessionsCache.clear();

        try {
            for (String professionEntry : ProfessionConfig.PROFESSIONS.get()) {
                String[] parts = professionEntry.split(";");
                if (parts.length == 3) {
                    String id = parts[0].toLowerCase().trim();
                    String displayName = parts[1].trim();
                    String colorCode = parts[2].trim();

                    professionDataCache.put(id, new ProfessionData(id, displayName, colorCode));
                }
            }

            lastCacheUpdate = System.currentTimeMillis();
            OneriaServerUtilities.LOGGER.info("[ProfessionRestrictions] Loaded {} professions", professionDataCache.size());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[ProfessionRestrictions] Error loading profession data", e);
        }
    }

    /**
     * Récupère les données d'un métier
     */
    public static ProfessionData getProfessionData(String professionId) {
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            reloadCache();
        }
        return professionDataCache.get(professionId.toLowerCase());
    }

    /**
     * Récupère tous les métiers disponibles
     */
    public static Collection<ProfessionData> getAllProfessions() {
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            reloadCache();
        }
        return professionDataCache.values();
    }

    /**
     * Vérifie si un joueur peut crafter un item
     */
    public static boolean canCraft(ServerPlayer player, ResourceLocation itemId) {
        // Vérifier si l'item est bloqué globalement
        if (!isGloballyBlocked(itemId, ProfessionConfig.GLOBAL_BLOCKED_CRAFTS.get())) {
            return true; // Pas de restriction globale
        }

        // Vérifier si le joueur a un métier qui autorise ce craft
        List<String> playerProfessions = LicenseManager.getLicenses(player.getUUID());
        return hasPermission(playerProfessions, itemId, ProfessionConfig.PROFESSION_ALLOWED_CRAFTS.get());
    }

    /**
     * Vérifie si un joueur peut casser un bloc
     */
    public static boolean canBreakBlock(ServerPlayer player, ResourceLocation blockId) {
        // Vérifier si le bloc est incassable globalement
        if (!isGloballyBlocked(blockId, ProfessionConfig.GLOBAL_UNBREAKABLE_BLOCKS.get())) {
            return true; // Pas de restriction globale
        }

        // Vérifier si le joueur a un métier qui autorise de casser ce bloc
        List<String> playerProfessions = LicenseManager.getLicenses(player.getUUID());
        return hasPermission(playerProfessions, blockId, ProfessionConfig.PROFESSION_ALLOWED_BLOCKS.get());
    }

    /**
     * Vérifie si un joueur peut utiliser un item
     */
    public static boolean canUseItem(ServerPlayer player, ResourceLocation itemId) {
        // Vérifier si l'item est bloqué globalement
        if (!isGloballyBlocked(itemId, ProfessionConfig.GLOBAL_BLOCKED_ITEMS.get())) {
            return true; // Pas de restriction globale
        }

        // Vérifier si le joueur a un métier qui autorise cet item
        List<String> playerProfessions = LicenseManager.getLicenses(player.getUUID());
        return hasPermission(playerProfessions, itemId, ProfessionConfig.PROFESSION_ALLOWED_ITEMS.get());
    }

    /**
     * Vérifie si un joueur peut équiper un item (armure, outil, arme)
     */
    public static boolean canEquip(ServerPlayer player, ResourceLocation itemId) {
        // Vérifier si l'équipement est bloqué globalement
        if (!isGloballyBlocked(itemId, ProfessionConfig.GLOBAL_BLOCKED_EQUIPMENT.get())) {
            return true; // Pas de restriction globale
        }

        // Vérifier si le joueur a un métier qui autorise cet équipement
        List<String> playerProfessions = LicenseManager.getLicenses(player.getUUID());
        return hasPermission(playerProfessions, itemId, ProfessionConfig.PROFESSION_ALLOWED_EQUIPMENT.get());
    }

    /**
     * Vérifie si une ressource est bloquée globalement (PUBLIC pour tooltips)
     */
    public static boolean isGloballyBlocked(ResourceLocation resourceId, List<? extends String> blockedList) {
        String resourceString = resourceId.toString();

        for (String blocked : blockedList) {
            if (matchesPattern(resourceString, blocked)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si un joueur a la permission via ses métiers
     */
    private static boolean hasPermission(List<String> professions, ResourceLocation resourceId, List<? extends String> allowedList) {
        String resourceString = resourceId.toString();

        for (String profession : professions) {
            for (String allowEntry : allowedList) {
                if (!allowEntry.contains(";")) continue;

                String[] parts = allowEntry.split(";", 2);
                String professionId = parts[0].toLowerCase().trim();
                String allowedItems = parts[1];

                // Vérifier si c'est le bon métier
                if (!professionId.equals(profession.toLowerCase())) continue;

                // Vérifier si l'item est dans la liste autorisée
                for (String allowedItem : allowedItems.split(",")) {
                    allowedItem = allowedItem.trim();
                    if (matchesPattern(resourceString, allowedItem)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Vérifie si une ressource correspond à un pattern (supporte les wildcards *)
     */
    private static boolean matchesPattern(String resourceId, String pattern) {
        pattern = pattern.trim();

        // Correspondance exacte
        if (resourceId.equals(pattern)) {
            return true;
        }

        // Support des wildcards
        if (pattern.contains("*")) {
            String regex = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*");
            return Pattern.matches(regex, resourceId);
        }

        return false;
    }

    /**
     * Récupère les métiers requis pour une action
     */
    public static String getRequiredProfessions(ResourceLocation resourceId, List<? extends String> allowedList) {
        String resourceString = resourceId.toString();
        Set<String> requiredProfessions = new HashSet<>();

        for (String allowEntry : allowedList) {
            if (!allowEntry.contains(";")) continue;

            String[] parts = allowEntry.split(";", 2);
            String professionId = parts[0].toLowerCase().trim();
            String allowedItems = parts[1];

            // Vérifier si l'item est dans la liste autorisée
            for (String allowedItem : allowedItems.split(",")) {
                allowedItem = allowedItem.trim();
                if (matchesPattern(resourceString, allowedItem)) {
                    ProfessionData data = getProfessionData(professionId);
                    if (data != null) {
                        requiredProfessions.add(data.getFormattedName());
                    }
                }
            }
        }

        return requiredProfessions.isEmpty() ? "§cAucun" : String.join("§7, ", requiredProfessions);
    }

    /**
     * Récupère le message formaté pour un craft bloqué
     */
    public static String getCraftBlockedMessage(ResourceLocation itemId) {
        String professions = getRequiredProfessions(itemId, ProfessionConfig.PROFESSION_ALLOWED_CRAFTS.get());
        return ProfessionConfig.MSG_CRAFT_BLOCKED.get()
                .replace("{item}", itemId.toString())
                .replace("{profession}", professions);
    }

    /**
     * Récupère le message formaté pour un bloc incassable
     */
    public static String getBlockBreakBlockedMessage(ResourceLocation blockId) {
        String professions = getRequiredProfessions(blockId, ProfessionConfig.PROFESSION_ALLOWED_BLOCKS.get());
        return ProfessionConfig.MSG_BLOCK_BREAK_BLOCKED.get()
                .replace("{item}", blockId.toString())
                .replace("{profession}", professions);
    }

    /**
     * Récupère le message formaté pour un item bloqué
     */
    public static String getItemUseBlockedMessage(ResourceLocation itemId) {
        String professions = getRequiredProfessions(itemId, ProfessionConfig.PROFESSION_ALLOWED_ITEMS.get());
        return ProfessionConfig.MSG_ITEM_USE_BLOCKED.get()
                .replace("{item}", itemId.toString())
                .replace("{profession}", professions);
    }

    /**
     * Récupère le message formaté pour un équipement bloqué
     */
    public static String getEquipmentBlockedMessage(ResourceLocation itemId) {
        String professions = getRequiredProfessions(itemId, ProfessionConfig.PROFESSION_ALLOWED_EQUIPMENT.get());
        return ProfessionConfig.MSG_EQUIPMENT_BLOCKED.get()
                .replace("{item}", itemId.toString())
                .replace("{profession}", professions);
    }

    /**
     * Invalide le cache pour un joueur
     */
    public static void invalidatePlayerCache(UUID playerUUID) {
        playerProfessionsCache.remove(playerUUID);
    }

    /**
     * Nettoie tout le cache
     */
    public static void clearCache() {
        playerProfessionsCache.clear();
        professionDataCache.clear();
        lastCacheUpdate = 0;
    }
}