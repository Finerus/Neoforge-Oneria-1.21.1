package net.rp.rpessentials;

import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Migration automatique de tous les anciens fichiers vers rpessentials/.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  CHAÎNE DE MIGRATION (exécutées dans l'ordre, conditions indépendantes) │
 * │                                                                         │
 * │  Phase 1 — RpEssentials-server.toml (v1/v2)                   │
 * │            → config/oneria/oneria-{core,chat,schedule,moderation}.toml │
 * │                                                                         │
 * │  Phase 2 — config/oneria-professions.toml (v2, racine)                 │
 * │            → config/oneria/oneria-professions.toml                     │
 * │                                                                         │
 * │  Phase 3 — config/oneria/oneria-*.toml (v3.x, ancien modid)            │
 * │            → config/rpessentials/rpessentials-*.toml                   │
 * │                                                                         │
 * │  Phase 4 — world/data/oneriamod/*.json (v3.x, ancien modid)            │
 * │            → world/data/rpessentials/*.json                            │
 * │            ⚠ Appelée séparément avec le worldPath (ServerStarting)     │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Appeler migrateIfNeeded() AVANT registerConfig().
 * Appeler migrateDataIfNeeded(worldDataPath) dans ServerStartingEvent.
 */
public class ConfigMigrator {

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    private static final String BACKUP_SUFFIX = ".migrated.bak";

    // Phase 1
    private static final String OLD_MAIN_FILE = "RpEssentials-server.toml";
    private static final String[] PHASE1_TEMPLATE_FILES = {
            "oneria-core.toml",
            "oneria-chat.toml",
            "oneria-schedule.toml",
            "oneria-moderation.toml"
    };
    private static final Map<String, String> PHASE1_KEY_RENAMES = new HashMap<>();
    static {
        PHASE1_KEY_RENAMES.put("serverClosedMessage",    "msgServerClosed");
        PHASE1_KEY_RENAMES.put("serverOpenedMessage",    "msgServerOpened");
        PHASE1_KEY_RENAMES.put("warningMessage",         "msgWarning");
        PHASE1_KEY_RENAMES.put("closingImminentMessage", "msgClosingImminent");
    }

    // Phase 2
    private static final String OLD_PROFESSIONS_FILE = "oneria-professions.toml";

    // Phase 3 : mapping oneria-*.toml → rpessentials-*.toml
    private static final Map<String, String> PHASE3_FILE_RENAMES = new LinkedHashMap<>();
    static {
        PHASE3_FILE_RENAMES.put("oneria-core.toml",        "rpessentials-core.toml");
        PHASE3_FILE_RENAMES.put("oneria-chat.toml",        "rpessentials-chat.toml");
        PHASE3_FILE_RENAMES.put("oneria-schedule.toml",    "rpessentials-schedule.toml");
        PHASE3_FILE_RENAMES.put("oneria-moderation.toml",  "rpessentials-moderation.toml");
        PHASE3_FILE_RENAMES.put("oneria-professions.toml", "rpessentials-professions.toml");
        PHASE3_FILE_RENAMES.put("oneria-messages.toml",    "rpessentials-messages.toml");
    }

    // Phase 4 : dossier data (noms de fichiers identiques, seul le dossier change)
    private static final String OLD_DATA_DIR  = "oneriamod";
    private static final String NEW_DATA_DIR  = "rpessentials";

    // =========================================================================
    // POINT D'ENTRÉE — configs (avant registerConfig)
    // =========================================================================

    public static void migrateIfNeeded() {
        File configDir = findConfigDir();
        File oneriaDir    = new File(configDir, "oneria");
        File rpessentialsDir = new File(configDir, "rpessentials");

        migratePhase1(configDir, oneriaDir);
        migratePhase2(configDir, oneriaDir);
        migratePhase3(oneriaDir, rpessentialsDir);
    }

    // =========================================================================
    // POINT D'ENTRÉE — données JSON (ServerStartingEvent)
    // =========================================================================

    /**
     * À appeler dans ServerStartingEvent avec :
     *   Path worldData = event.getServer().getWorldPath(LevelResource.ROOT)
     *                         .resolve("data");
     *   ConfigMigrator.migrateDataIfNeeded(worldData);
     */
    public static void migrateDataIfNeeded(Path worldDataPath) {
        Path oldDir = worldDataPath.resolve(OLD_DATA_DIR);
        Path newDir = worldDataPath.resolve(NEW_DATA_DIR);

        if (!Files.exists(oldDir)) return;
        if (Files.exists(newDir))  return; // déjà migré

        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");
        RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 4] Data migration: oneriamod/ → rpessentials/");
        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");

        try {
            Files.createDirectories(newDir);
            int count = 0;

            try (Stream<Path> files = Files.list(oldDir)) {
                for (Path src : (Iterable<Path>) files::iterator) {
                    if (!Files.isRegularFile(src)) continue;
                    Path dst = newDir.resolve(src.getFileName());
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 4] Copied: {}",
                            src.getFileName());
                    count++;
                }
            }

            // Backup du vieux dossier
            Path backup = worldDataPath.resolve(OLD_DATA_DIR + BACKUP_SUFFIX);
            try {
                Files.move(oldDir, backup, StandardCopyOption.REPLACE_EXISTING);
                RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 4] Backed up oneriamod/ → {}", backup.getFileName());
            } catch (IOException e) {
                RpEssentials.LOGGER.warn("[ConfigMigrator] [Phase 4] Could not backup oneriamod/ — original kept.");
            }

            RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 4] Done — {} file(s) migrated.", count);

        } catch (Exception e) {
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 4] Data migration failed!", e);
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 4] Manual action: copy world/data/oneriamod/ → world/data/rpessentials/");
        }
    }

    // =========================================================================
    // PHASE 1 — RpEssentials-server.toml → oneria/{core,chat,schedule,moderation}.toml
    // =========================================================================

    private static void migratePhase1(File configDir, File oneriaDir) {
        File oldFile = new File(configDir, OLD_MAIN_FILE);
        if (!oldFile.exists()) return;

        boolean alreadyMigrated = Arrays.stream(PHASE1_TEMPLATE_FILES)
                .anyMatch(f -> new File(oneriaDir, f).exists());
        if (alreadyMigrated) return;

        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");
        RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 1] Legacy v1/v2 config detected, migrating...");
        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");

        try {
            Map<String, String> values = parseConfig(oldFile);
            // Appliquer les renommages de clés
            Map<String, String> renamed = new LinkedHashMap<>(values);
            PHASE1_KEY_RENAMES.forEach((oldKey, newKey) -> {
                if (renamed.containsKey(oldKey)) {
                    renamed.put(newKey, renamed.remove(oldKey));
                    RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 1] Renamed key: {} → {}", oldKey, newKey);
                }
            });

            if (renamed.isEmpty()) {
                RpEssentials.LOGGER.warn("[ConfigMigrator] [Phase 1] Could not parse legacy config, aborting.");
                return;
            }

            oneriaDir.mkdirs();
            int totalMigrated = 0;
            for (String templateName : PHASE1_TEMPLATE_FILES) {
                File outFile = new File(oneriaDir, templateName);
                int migrated = writeFromTemplate(templateName, renamed, outFile);
                RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 1] {} → {} key(s) migrated", templateName, migrated);
                totalMigrated += migrated;
            }

            backup(oldFile, configDir, OLD_MAIN_FILE);
            RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 1] Done — {} key(s) total.", totalMigrated);

        } catch (Exception e) {
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 1] Migration failed!", e);
        }
    }

    // =========================================================================
    // PHASE 2 — oneria-professions.toml (racine) → oneria/oneria-professions.toml
    // =========================================================================

    private static void migratePhase2(File configDir, File oneriaDir) {
        File oldFile = new File(configDir, OLD_PROFESSIONS_FILE);
        if (!oldFile.exists()) return;

        File newFile = new File(oneriaDir, OLD_PROFESSIONS_FILE);
        if (newFile.exists()) return;

        RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 2] Moving oneria-professions.toml → oneria/");

        try {
            oneriaDir.mkdirs();
            Files.copy(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            backup(oldFile, configDir, OLD_PROFESSIONS_FILE);
            RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 2] Done.");
        } catch (Exception e) {
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 2] Migration failed!", e);
        }
    }

    // =========================================================================
    // PHASE 3 — config/oneria/oneria-*.toml → config/rpessentials/rpessentials-*.toml
    // =========================================================================

    private static void migratePhase3(File oneriaDir, File rpessentialsDir) {
        // Rien à faire si le dossier rpessentials/ existe déjà
        if (rpessentialsDir.exists()) return;
        // Rien à faire si le dossier oneria/ n'existe pas
        if (!oneriaDir.exists()) return;

        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");
        RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 3] v3.x config detected: oneria/ → rpessentials/");
        RpEssentials.LOGGER.info("[ConfigMigrator] ==========================================");

        try {
            rpessentialsDir.mkdirs();
            int count = 0;

            for (Map.Entry<String, String> entry : PHASE3_FILE_RENAMES.entrySet()) {
                File src = new File(oneriaDir, entry.getKey());
                File dst = new File(rpessentialsDir, entry.getValue());
                if (!src.exists()) {
                    RpEssentials.LOGGER.debug("[ConfigMigrator] [Phase 3] Not found (skipped): {}", entry.getKey());
                    continue;
                }
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 3] {} → {}", entry.getKey(), entry.getValue());
                count++;
            }

            // Backup du vieux dossier oneria/
            File backupDir = new File(oneriaDir.getParentFile(), "oneria" + BACKUP_SUFFIX);
            if (oneriaDir.renameTo(backupDir)) {
                RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 3] Backed up config/oneria/ → config/oneria{}", BACKUP_SUFFIX);
            } else {
                RpEssentials.LOGGER.warn("[ConfigMigrator] [Phase 3] Could not rename oneria/ — backup skipped, originals kept.");
            }

            RpEssentials.LOGGER.info("[ConfigMigrator] [Phase 3] Done — {} file(s) migrated.", count);

        } catch (Exception e) {
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 3] Migration failed!", e);
            RpEssentials.LOGGER.error("[ConfigMigrator] [Phase 3] Manual action: copy config/oneria/ → config/rpessentials/ and rename oneria-*.toml → rpessentials-*.toml");
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

        RpEssentials.LOGGER.info("[ConfigMigrator] Parsed {} key-value pairs from {}", result.size(), file.getName());
        return result;
    }

    // =========================================================================
    // ÉCRITURE DEPUIS TEMPLATE (Phase 1 uniquement)
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
            RpEssentials.LOGGER.info("[ConfigMigrator] Backed up as: {}", backup.getName());
        } else {
            RpEssentials.LOGGER.warn("[ConfigMigrator] Could not backup {} (file still present).", name);
        }
    }

    private static File findConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }
}