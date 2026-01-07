package net.oneria.oneriaserverutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire de nicknames avec sauvegarde automatique
 * Le fichier est sauvegardé dans le dossier world/data/oneriamod/
 */
public class NicknameManager {
    private static final Map<UUID, String> nicknames = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File nicknameFile = null;

    /**
     * Définit le fichier de sauvegarde et charge les nicknames
     * Appelé automatiquement au premier accès
     */
    private static void ensureInitialized() {
        if (nicknameFile != null) return;

        try {
            // Utiliser le dossier de travail (là où se trouve le serveur)
            File worldFolder = new File("world");
            if (!worldFolder.exists()) {
                // Si on est pas dans le bon dossier, utiliser le dossier courant
                worldFolder = new File(".");
            }

            // Créer le dossier world/data/oneriamod/
            File dataFolder = new File(worldFolder, "data/oneriamod");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            nicknameFile = new File(dataFolder, "nicknames.json");

            // Charger les nicknames si le fichier existe
            if (nicknameFile.exists()) {
                loadFromFile();
            }

            OneriaServerUtilities.LOGGER.info("[NicknameManager] Initialized - File: {}", nicknameFile.getAbsolutePath());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[NicknameManager] Failed to initialize", e);
        }
    }

    /**
     * Charge les nicknames depuis le fichier JSON
     */
    private static void loadFromFile() {
        if (nicknameFile == null || !nicknameFile.exists()) return;

        try (FileReader reader = new FileReader(nicknameFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> data = GSON.fromJson(reader, type);

            if (data != null) {
                nicknames.clear();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        nicknames.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        OneriaServerUtilities.LOGGER.warn("[NicknameManager] Invalid UUID: {}", entry.getKey());
                    }
                }
                OneriaServerUtilities.LOGGER.info("[NicknameManager] Loaded {} nicknames", nicknames.size());
            }
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[NicknameManager] Failed to load nicknames", e);
        }
    }

    /**
     * Sauvegarde les nicknames dans le fichier JSON
     */
    private static void saveToFile() {
        ensureInitialized();
        if (nicknameFile == null) return;

        try {
            // S'assurer que le dossier parent existe
            File parent = nicknameFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Convertir les UUID en String pour JSON
            Map<String, String> data = new HashMap<>();
            for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }

            // Écrire dans le fichier
            try (FileWriter writer = new FileWriter(nicknameFile)) {
                GSON.toJson(data, writer);
            }

            OneriaServerUtilities.LOGGER.debug("[NicknameManager] Saved {} nicknames", nicknames.size());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[NicknameManager] Failed to save nicknames", e);
        }
    }

    /**
     * Définit un nickname pour un joueur
     */
    public static void setNickname(UUID playerUUID, String nickname) {
        ensureInitialized();

        if (nickname == null || nickname.isEmpty()) {
            nicknames.remove(playerUUID);
        } else {
            nicknames.put(playerUUID, nickname);
        }

        saveToFile();
    }

    /**
     * Récupère le nickname d'un joueur
     */
    public static String getNickname(UUID playerUUID) {
        ensureInitialized();
        return nicknames.get(playerUUID);
    }

    /**
     * Récupère le nom d'affichage (nickname ou nom réel)
     */
    public static String getDisplayName(ServerPlayer player) {
        String nickname = getNickname(player.getUUID());
        return nickname != null ? nickname : player.getGameProfile().getName();
    }

    /**
     * Supprime le nickname d'un joueur
     */
    public static void removeNickname(UUID playerUUID) {
        ensureInitialized();
        nicknames.remove(playerUUID);
        saveToFile();
    }

    /**
     * Vérifie si un joueur a un nickname
     */
    public static boolean hasNickname(UUID playerUUID) {
        ensureInitialized();
        return nicknames.containsKey(playerUUID);
    }

    /**
     * Supprime tous les nicknames
     */
    public static void clearAll() {
        ensureInitialized();
        nicknames.clear();
        saveToFile();
    }

    /**
     * Retourne le nombre de nicknames enregistrés
     */
    public static int count() {
        ensureInitialized();
        return nicknames.size();
    }

    /**
     * Recharge les nicknames depuis le fichier
     */
    public static void reload() {
        nicknameFile = null; // Réinitialiser
        nicknames.clear();
        ensureInitialized();
    }
}