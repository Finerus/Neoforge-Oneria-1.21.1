package net.oneria.oneriaserverutilities.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Stockage côté CLIENT des restrictions synchronisées depuis le serveur
 */
public class ClientProfessionRestrictions {

    private static Set<String> blockedCrafts = new HashSet<>();
    private static Set<String> blockedEquipment = new HashSet<>();

    /**
     * Met à jour les restrictions depuis le packet serveur
     */
    public static void updateRestrictions(Set<String> crafts, Set<String> equipment) {
        blockedCrafts = new HashSet<>(crafts);
        blockedEquipment = new HashSet<>(equipment);
    }

    /**
     * Vérifie si un item est bloqué pour le craft
     */
    public static boolean isCraftBlocked(String itemId) {
        return blockedCrafts.contains(itemId);
    }

    /**
     * Vérifie si un équipement est bloqué
     */
    public static boolean isEquipmentBlocked(String itemId) {
        return blockedEquipment.contains(itemId);
    }

    /**
     * Reset lors de la déconnexion
     */
    public static void reset() {
        blockedCrafts.clear();
        blockedEquipment.clear();
    }
}