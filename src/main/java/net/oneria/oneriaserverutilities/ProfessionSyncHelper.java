package net.oneria.oneriaserverutilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilitaire pour synchroniser les restrictions de métiers avec le client
 */
public class ProfessionSyncHelper {

    /**
     * Envoie les restrictions au joueur lors de sa connexion
     * Appelé dans OneriaEventHandler.onPlayerLogin()
     */
    public static void syncToPlayer(ServerPlayer player) {
        // Récupérer les licences du joueur
        List<String> playerLicenses = LicenseManager.getLicenses(player.getUUID());

        // Calculer les items bloqués pour CE joueur
        Set<String> blockedCrafts = calculateBlockedCrafts(playerLicenses);
        Set<String> blockedEquipment = calculateBlockedEquipment(playerLicenses);

        // Envoyer le packet au client
        SyncProfessionRestrictionsPacket packet = new SyncProfessionRestrictionsPacket(blockedCrafts, blockedEquipment);
        PacketDistributor.sendToPlayer(player, packet);

        OneriaServerUtilities.LOGGER.info("[ProfessionSync] Sent restrictions to {} - {} crafts, {} equipment blocked",
                player.getName().getString(), blockedCrafts.size(), blockedEquipment.size());
    }

    /**
     * Calcule les items de craft bloqués pour un joueur
     */
    private static Set<String> calculateBlockedCrafts(List<String> playerLicenses) {
        Set<String> blocked = new HashSet<>();

        // Parcourir tous les items bloqués globalement
        for (String itemPattern : ProfessionConfig.GLOBAL_BLOCKED_CRAFTS.get()) {
            // Vérifier si le joueur a une licence qui autorise cet item
            if (!hasPermissionForPattern(playerLicenses, itemPattern, ProfessionConfig.PROFESSION_ALLOWED_CRAFTS.get())) {
                blocked.add(itemPattern);
            }
        }

        return blocked;
    }

    /**
     * Calcule les équipements bloqués pour un joueur
     */
    private static Set<String> calculateBlockedEquipment(List<String> playerLicenses) {
        Set<String> blocked = new HashSet<>();

        // Parcourir tous les équipements bloqués globalement
        for (String itemPattern : ProfessionConfig.GLOBAL_BLOCKED_EQUIPMENT.get()) {
            // Vérifier si le joueur a une licence qui autorise cet équipement
            if (!hasPermissionForPattern(playerLicenses, itemPattern, ProfessionConfig.PROFESSION_ALLOWED_EQUIPMENT.get())) {
                blocked.add(itemPattern);
            }
        }

        return blocked;
    }

    /**
     * Vérifie si le joueur a la permission pour un pattern via ses licences
     */
    private static boolean hasPermissionForPattern(List<String> licenses, String pattern, List<? extends String> allowedList) {
        for (String license : licenses) {
            for (String allowEntry : allowedList) {
                if (!allowEntry.contains(";")) continue;

                String[] parts = allowEntry.split(";", 2);
                String professionId = parts[0].toLowerCase().trim();
                String allowedItems = parts[1];

                // Vérifier si c'est la bonne licence
                if (!professionId.equals(license.toLowerCase())) continue;

                // Vérifier si le pattern est dans la liste autorisée
                for (String allowedItem : allowedItems.split(",")) {
                    allowedItem = allowedItem.trim();
                    if (allowedItem.equals(pattern) || pattern.startsWith(allowedItem.replace("*", ""))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}