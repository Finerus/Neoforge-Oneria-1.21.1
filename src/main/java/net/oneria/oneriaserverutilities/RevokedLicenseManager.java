package net.oneria.oneriaserverutilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Gestionnaire pour tracker les permis révoqués et les supprimer de l'inventaire
 */
public class RevokedLicenseManager {

    // Map: UUID du joueur -> Set des professions révoquées en attente de suppression
    private static final Map<UUID, Set<String>> pendingRemovals = new HashMap<>();

    /**
     * Marque une licence comme révoquée pour un joueur
     * La licence sera supprimée de son inventaire dès qu'il essaiera de l'utiliser
     */
    public static void markForRemoval(UUID playerUUID, String professionId) {
        pendingRemovals.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(professionId);
        OneriaServerUtilities.LOGGER.info("[RevokedLicense] Marked {} for removal from player {}",
                professionId, playerUUID);
    }

    /**
     * Vérifie si une profession est marquée pour suppression
     */
    public static boolean isMarkedForRemoval(UUID playerUUID, String professionId) {
        Set<String> professions = pendingRemovals.get(playerUUID);
        return professions != null && professions.contains(professionId);
    }

    /**
     * Retire la marque de suppression après avoir supprimé la licence
     */
    public static void unmarkRemoval(UUID playerUUID, String professionId) {
        Set<String> professions = pendingRemovals.get(playerUUID);
        if (professions != null) {
            professions.remove(professionId);
            if (professions.isEmpty()) {
                pendingRemovals.remove(playerUUID);
            }
        }
    }

    /**
     * Supprime immédiatement toutes les licences révoquées de l'inventaire du joueur
     */
    public static void removeAllRevokedLicenses(ServerPlayer player) {
        Set<String> revokedProfessions = pendingRemovals.get(player.getUUID());
        if (revokedProfessions == null || revokedProfessions.isEmpty()) {
            return;
        }

        int removed = 0;

        // Parcourir tout l'inventaire
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.is(OneriaItems.LICENSE.get())) {
                // Extraire le nom de la profession depuis le nom de l'item
                String displayName = stack.getHoverName().getString();

                // Le nom est au format "§lPermis de <Profession>"
                for (String profession : revokedProfessions) {
                    ProfessionRestrictionManager.ProfessionData profData =
                            ProfessionRestrictionManager.getProfessionData(profession);

                    if (profData != null) {
                        String expectedName = profData.colorCode + "§lPermis de " + profData.displayName;

                        if (displayName.equals(expectedName)) {
                            // Supprimer l'item
                            player.getInventory().removeItem(i, stack.getCount());
                            removed++;

                            player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "§cVotre " + profData.getFormattedName() + "§c§l Permis§c a été révoqué et retiré."));

                            OneriaServerUtilities.LOGGER.info("[RevokedLicense] Removed {} license from player {}",
                                    profession, player.getName().getString());
                        }
                    }
                }
            }
        }

        if (removed > 0) {
            // Nettoyer les marques
            for (String profession : new HashSet<>(revokedProfessions)) {
                unmarkRemoval(player.getUUID(), profession);
            }
        }
    }

    /**
     * Nettoie les marques de suppression pour un joueur (appelé à la déconnexion)
     */
    public static void cleanup(UUID playerUUID) {
        pendingRemovals.remove(playerUUID);
    }
}