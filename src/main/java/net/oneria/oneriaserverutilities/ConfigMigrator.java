package net.oneria.oneriaserverutilities;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Migre automatiquement les anciens fichiers de config vers oneria/.
 *
 * Deux migrations indépendantes :
 *   1. oneriaserverutilities-server.toml → oneria/{core,chat,schedule,moderation}.toml
 *   2. oneria-professions.toml           → oneria/oneria-professions.toml
 *
 * Chaque migration a sa propre condition de déclenchement.
 * Doit être appelé AVANT registerConfig().
 */
public class ConfigMigrator {

    private static final String OLD_MAIN_FILE        = "oneriaserverutilities-server.toml";
    private static final String OLD_PROFESSIONS_FILE = "oneria-professions.toml";
    private static final String BACKUP_SUFFIX        = ".migrated.bak";

    // Fichiers templates embarqués dans le JAR (src/main/resources/config_templates/)
    private static final String[] MAIN_TEMPLATE_FILES = {
            "oneria-core.toml",
            "oneria-chat.toml",
            "oneria-schedule.toml",
            "oneria-moderation.toml"
    };

    // Clés renommées entre l'ancien fichier principal et les nouveaux : ancien → nouveau
    private static final Map<String, String> KEY_RENAMES = new HashMap<>();
    static {
        KEY_RENAMES.put("serverClosedMessage",    "msgServerClosed");
        KEY_RENAMES.put("serverOpenedMessage",    "msgServerOpened");
        KEY_RENAMES.put("warningMessage",         "msgWarning");
        KEY_RENAMES.put("closingImminentMessage", "msgClosingImminent");
    }

    // =========================================================================
    // POINT D'ENTRÉE
    // =========================================================================

    public static void migrateIfNeeded() {
        File configDir = findConfigDir();
        File oneriaDir = new File(configDir, "oneria");

        migrateMainConfig(configDir, oneriaDir);
        migrateProfessionsConfig(configDir, oneriaDir);
    }

    // =========================================================================
    // MIGRATION PRINCIPALE
    // =========================================================================

    private static void migrateMainConfig(File configDir, File oneriaDir) {
        File oldFile = new File(configDir, OLD_MAIN_FILE);
        if (!oldFile.exists()) return;

        // Skip si au moins un des nouveaux fichiers existe déjà
        boolean alreadyMigrated = Arrays.stream(MAIN_TEMPLATE_FILES)
                .anyMatch(f -> new File(oneriaDir, f).exists());
        if (alreadyMigrated) return;

        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");
        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Legacy main config detected, migrating...");
        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");

        try {
            Map<String, String> values = parseConfig(oldFile);
            if (values.isEmpty()) {
                OneriaServerUtilities.LOGGER.warn("[ConfigMigrator] Could not parse legacy config, aborting.");
                return;
            }

            // Appliquer les renames
            for (Map.Entry<String, String> rename : KEY_RENAMES.entrySet()) {
                if (values.containsKey(rename.getKey()) && !values.containsKey(rename.getValue())) {
                    values.put(rename.getValue(), values.get(rename.getKey()));
                }
            }

            if (!oneriaDir.exists()) oneriaDir.mkdirs();

            int total = 0;
            for (String templateName : MAIN_TEMPLATE_FILES) {
                int count = writeFromTemplate(templateName, values, new File(oneriaDir, templateName));
                total += count;
                OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Written {} values to {}", count, templateName);
            }

            backup(oldFile, configDir, OLD_MAIN_FILE);

            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Main migration complete! {} values migrated.", total);
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");

        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[ConfigMigrator] Main migration failed!", e);
        }
    }

    // =========================================================================
    // MIGRATION PROFESSIONS
    // =========================================================================

    private static void migrateProfessionsConfig(File configDir, File oneriaDir) {
        File oldFile = new File(configDir, OLD_PROFESSIONS_FILE);
        File newFile = new File(oneriaDir, "oneria-professions.toml");

        if (!oldFile.exists()) return;
        if (newFile.exists()) return;

        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");
        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Legacy professions config detected, migrating...");
        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");

        try {
            Map<String, String> values = parseConfig(oldFile);

            if (!oneriaDir.exists()) oneriaDir.mkdirs();

            int count = writeFromTemplate("oneria-professions.toml", values, newFile);
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Written {} values to oneria-professions.toml", count);

            backup(oldFile, configDir, OLD_PROFESSIONS_FILE);

            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Professions migration complete!");
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] ==========================================");

        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[ConfigMigrator] Professions migration failed!", e);
        }
    }

    // =========================================================================
    // PARSING
    // =========================================================================

    private static Map<String, String> parseConfig(File file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        Pattern kvPattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.+)$");

        for (String line : Files.readAllLines(file.toPath())) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) continue;
            Matcher m = kvPattern.matcher(trimmed);
            if (m.matches()) result.put(m.group(1), m.group(2).trim());
        }

        OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Parsed {} key-value pairs from {}", result.size(), file.getName());
        return result;
    }

    // =========================================================================
    // ÉCRITURE DEPUIS TEMPLATE
    // =========================================================================

    private static int writeFromTemplate(String templateName, Map<String, String> values, File outputFile)
            throws IOException {

        String resourcePath = "/config_templates/" + templateName;
        InputStream stream = ConfigMigrator.class.getResourceAsStream(resourcePath);
        if (stream == null) throw new IOException("Template not found in JAR: " + resourcePath);

        Pattern kvPattern = Pattern.compile("^(\\s*)(\\w+)(\\s*=\\s*)(.+)$");
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = kvPattern.matcher(line);
                if (m.matches() && values.containsKey(m.group(2))) {
                    line = m.group(1) + m.group(2) + m.group(3) + values.get(m.group(2));
                    count++;
                }
                writer.write(line);
                writer.newLine();
            }
        }

        return count;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private static void backup(File file, File dir, String name) {
        File backup = new File(dir, name + BACKUP_SUFFIX);
        if (file.renameTo(backup)) {
            OneriaServerUtilities.LOGGER.info("[ConfigMigrator] Backed up as: {}", backup.getName());
        } else {
            OneriaServerUtilities.LOGGER.warn("[ConfigMigrator] Could not backup {} (file still present).", name);
        }
    }

    private static File findConfigDir() {
        File standard = new File("config");
        if (standard.exists()) return standard;
        return new File(".");
    }
}