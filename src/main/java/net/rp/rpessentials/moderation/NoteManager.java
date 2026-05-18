package net.rp.rpessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.RpEssentialsDataPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NoteManager {

    // =========================================================================
    // INNER CLASS
    // =========================================================================

    public static class NoteEntry {
        public int    id;
        public String text;
        public String authorName;
        public String authorUUID;
        public String timestamp;

        public NoteEntry() {}

        public NoteEntry(int id, String text, String authorName, String authorUUID) {
            this.id         = id;
            this.text       = text;
            this.authorName = authorName;
            this.authorUUID = authorUUID;
            this.timestamp  = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    // =========================================================================
    // STATE
    // =========================================================================

    // UUID cible → liste de notes
    private static final Map<UUID, List<NoteEntry>> notes = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File notesDir = null;
    private static final Map<UUID, String> playerNames = new HashMap<>();

    // =========================================================================
    // INIT
    // =========================================================================

    private static synchronized void ensureInitialized() {
        if (notesDir != null) return;
        try {
            notesDir = new File(RpEssentialsDataPaths.getDataFolder(), "notes");
            if (!notesDir.exists()) notesDir.mkdirs();
            loadAll();
        } catch (Exception e) {
            RpEssentials.LOGGER.error("[NoteManager] Failed to initialize", e);
        }
    }

    // =========================================================================
    // LOAD / SAVE
    // =========================================================================

    private static void loadAll() {
        if (notesDir == null) return;
        File[] files = notesDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                String fname   = file.getName().replace(".json", "");
                String uuidStr = fname.contains(" - ")
                        ? fname.substring(fname.lastIndexOf(" - ") + 3)
                        : fname;
                UUID uuid = UUID.fromString(uuidStr);
                if (fname.contains(" - "))
                    playerNames.put(uuid, fname.substring(0, fname.lastIndexOf(" - ")));
                Type type = new TypeToken<List<NoteEntry>>(){}.getType();
                List<NoteEntry> list = GSON.fromJson(reader, type);
                if (list != null) {
                    list.sort(Comparator.comparingInt(e -> e.id));
                    notes.put(uuid, new ArrayList<>(list));
                }
            } catch (Exception e) {
                RpEssentials.LOGGER.warn("[NoteManager] Could not load file: {}", file.getName());
            }
        }
        RpEssentials.LOGGER.info("[NoteManager] Loaded notes for {} players", notes.size());
    }

    private static void saveForPlayer(UUID uuid) {
        if (notesDir == null) return;
        List<NoteEntry> list = notes.getOrDefault(uuid, List.of());
        String name     = playerNames.get(uuid);
        String baseName = (name != null ? name + " - " : "") + uuid;
        File targetFile = new File(notesDir, baseName + ".json");

        CompletableFuture.runAsync(() -> {
            try {
                // Migration : supprimer l'ancien fichier UUID-only si on a le nom
                if (name != null) {
                    File oldFile = new File(notesDir, uuid + ".json");
                    if (oldFile.exists() && !oldFile.getCanonicalPath().equals(targetFile.getCanonicalPath()))
                        oldFile.delete();
                }
                if (list.isEmpty()) { targetFile.delete(); return; }
                try (FileWriter writer = new FileWriter(targetFile)) {
                    GSON.toJson(list, writer);
                }
            } catch (Exception e) {
                RpEssentials.LOGGER.error("[NoteManager] Failed to save notes for {}", uuid, e);
            }
        });
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================
    public static int addNote(UUID targetUUID, String targetName, String authorName, String authorUUID, String text) {
        ensureInitialized();
        playerNames.put(targetUUID, targetName);
        List<NoteEntry> list = notes.computeIfAbsent(targetUUID, k -> new ArrayList<>());

        try {
            int max = net.rp.rpessentials.config.ModerationConfig.NOTE_MAX_PER_PLAYER.get();
            if (max > 0 && list.size() >= max) {
                RpEssentials.LOGGER.warn("[NoteManager] Note limit ({}) reached for {}", max, targetName);
                return -1;
            }
        } catch (IllegalStateException ignored) {}

        Set<Integer> usedIds = new HashSet<>();
        for (NoteEntry e : list) usedIds.add(e.id);
        int id = 1;
        while (usedIds.contains(id)) id++;
        list.add(new NoteEntry(id, text, authorName, authorUUID));
        list.sort(Comparator.comparingInt(e -> e.id));
        saveForPlayer(targetUUID);
        return id;
    }

    public static boolean removeNote(UUID targetUUID, int noteId) {
        ensureInitialized();
        List<NoteEntry> list = notes.get(targetUUID);
        if (list == null) return false;
        boolean removed = list.removeIf(e -> e.id == noteId);
        if (removed) saveForPlayer(targetUUID);
        return removed;
    }

    public static void clearNotes(UUID targetUUID) {
        ensureInitialized();
        notes.remove(targetUUID);
        saveForPlayer(targetUUID);
    }

    public static List<NoteEntry> getNotes(UUID targetUUID) {
        ensureInitialized();
        return new ArrayList<>(notes.getOrDefault(targetUUID, List.of()));
    }

    public static boolean hasNotes(UUID targetUUID) {
        ensureInitialized();
        List<NoteEntry> list = notes.get(targetUUID);
        return list != null && !list.isEmpty();
    }
}