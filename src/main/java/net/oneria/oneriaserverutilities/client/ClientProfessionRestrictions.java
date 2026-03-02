package net.oneria.oneriaserverutilities.client;

import net.oneria.oneriaserverutilities.OneriaPatternUtils;
import net.oneria.oneriaserverutilities.OneriaServerUtilities;

import java.util.HashSet;
import java.util.Set;

/**
 * Stocke côté client les items/armures bloqués reçus du serveur
 * Utilisé par ClientProfessionRestrictionHandler pour vérifier les restrictions
 */
public class ClientProfessionRestrictions {

    private static final Set<String> blockedCrafts = new HashSet<>();
    private static final Set<String> blockedEquipment = new HashSet<>();

    /**
     * Met à jour les restrictions depuis le serveur
     * Appelé quand on reçoit SyncProfessionRestrictionsPacket
     */
    public static void updateRestrictions(Set<String> crafts, Set<String> equipment) {
        blockedCrafts.clear();
        blockedCrafts.addAll(crafts);

        blockedEquipment.clear();
        blockedEquipment.addAll(equipment);

        OneriaServerUtilities.LOGGER.debug("[ClientRestrictions] Updated: {} crafts, {} equipment",
                crafts.size(), equipment.size());
    }

    /**
     * Vérifie si un craft est bloqué
     */
    public static boolean isCraftBlocked(String itemId) {
        return blockedCrafts.stream().anyMatch(pattern -> OneriaPatternUtils.matchesPattern(itemId, pattern));
    }

    /**
     * Vérifie si un équipement est bloqué
     */
    public static boolean isEquipmentBlocked(String itemId) {
        return blockedEquipment.stream().anyMatch(pattern -> OneriaPatternUtils.matchesPattern(itemId, pattern));
    }
}