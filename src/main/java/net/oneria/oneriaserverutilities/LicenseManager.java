package net.oneria.oneriaserverutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class LicenseManager {
    private static final Map<UUID, List<String>> playerLicenses = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File licenseFile = null;

    private static void ensureInitialized() {
        if (licenseFile != null) return;

        try {
            File worldFolder = new File("world");
            if (!worldFolder.exists()) {
                worldFolder = new File(".");
            }

            File dataFolder = new File(worldFolder, "data/oneriamod");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            licenseFile = new File(dataFolder, "licenses.json");

            if (licenseFile.exists()) {
                loadFromFile();
            }

            OneriaServerUtilities.LOGGER.info("[LicenseManager] Initialized - File: {}", licenseFile.getAbsolutePath());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[LicenseManager] Failed to initialize", e);
        }
    }

    private static void loadFromFile() {
        if (licenseFile == null || !licenseFile.exists()) return;

        try (FileReader reader = new FileReader(licenseFile)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> data = GSON.fromJson(reader, type);

            if (data != null) {
                playerLicenses.clear();
                for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        playerLicenses.put(uuid, new ArrayList<>(entry.getValue()));
                    } catch (IllegalArgumentException e) {
                        OneriaServerUtilities.LOGGER.warn("[LicenseManager] Invalid UUID: {}", entry.getKey());
                    }
                }
                OneriaServerUtilities.LOGGER.info("[LicenseManager] Loaded {} player licenses", playerLicenses.size());
            }
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[LicenseManager] Failed to load licenses", e);
        }
    }

    private static void saveToFile() {
        ensureInitialized();
        if (licenseFile == null) return;

        try {
            File parent = licenseFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Map<String, List<String>> data = new HashMap<>();
            for (Map.Entry<UUID, List<String>> entry : playerLicenses.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(licenseFile)) {
                GSON.toJson(data, writer);
            }

            OneriaServerUtilities.LOGGER.debug("[LicenseManager] Saved licenses for {} players", playerLicenses.size());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[LicenseManager] Failed to save licenses", e);
        }
    }

    public static void addLicense(UUID playerUUID, String profession) {
        ensureInitialized();
        playerLicenses.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(profession);
        saveToFile();
    }

    public static void removeLicense(UUID playerUUID, String profession) {
        ensureInitialized();
        List<String> licenses = playerLicenses.get(playerUUID);
        if (licenses != null) {
            licenses.remove(profession);
            if (licenses.isEmpty()) {
                playerLicenses.remove(playerUUID);
            }
            saveToFile();
        }
    }

    public static List<String> getLicenses(UUID playerUUID) {
        ensureInitialized();
        return new ArrayList<>(playerLicenses.getOrDefault(playerUUID, new ArrayList<>()));
    }

    public static boolean hasLicense(UUID playerUUID, String profession) {
        ensureInitialized();
        List<String> licenses = playerLicenses.get(playerUUID);
        return licenses != null && licenses.contains(profession);
    }

    public static void reload() {
        licenseFile = null;
        playerLicenses.clear();
        ensureInitialized();
    }

    /**
     * Retourne toutes les licences de tous les joueurs
     */
    public static Map<UUID, List<String>> getAllLicenses() {
        ensureInitialized();
        Map<UUID, List<String>> result = new HashMap<>();
        for (Map.Entry<UUID, List<String>> entry : playerLicenses.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
}