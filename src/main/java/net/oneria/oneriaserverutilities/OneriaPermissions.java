package net.oneria.oneriaserverutilities;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneriaPermissions {

    // Cache to avoid checking every time
    private static final Map<UUID, CacheEntry> staffCache = new HashMap<>();
    private static final long CACHE_DURATION = 30000; // 30 seconds

    private static class CacheEntry {
        boolean isStaff;
        long timestamp;

        CacheEntry(boolean isStaff) {
            this.isStaff = isStaff;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION;
        }
    }

    /**
     * Checks if a player is considered staff
     * Verification hierarchy:
     * 1. Vanilla Tags (admin, modo, staff, builder, etc.)
     * 2. OP Level
     * 3. LuckPerms Groups (if enabled)
     */
    public static boolean isStaff(ServerPlayer player) {
        if (player == null) return false;

        // Check cache
        CacheEntry cached = staffCache.get(player.getUUID());
        if (cached != null && cached.isValid()) {
            return cached.isStaff;
        }

        boolean result = checkStaffStatus(player);

        // Update cache
        staffCache.put(player.getUUID(), new CacheEntry(result));

        return result;
    }

    private static boolean checkStaffStatus(ServerPlayer player) {
        // 1. Check Vanilla Tags
        for (String tag : OneriaConfig.STAFF_TAGS.get()) {
            if (player.getTags().contains(tag)) {
                return true;
            }
        }

        // 2. Check OP Level
        int opLevel = OneriaConfig.OP_LEVEL_BYPASS.get();
        if (opLevel > 0 && player.hasPermissions(opLevel)) {
            return true;
        }

        // 3. Check LuckPerms (if enabled AND available)
        if (OneriaConfig.USE_LUCKPERMS_GROUPS.get()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUUID());
                if (user != null) {
                    String primaryGroup = user.getPrimaryGroup();
                    if (OneriaConfig.LUCKPERMS_STAFF_GROUPS.get().contains(primaryGroup)) {
                        return true;
                    }

                    // Check inherited groups as well
                    for (String group : OneriaConfig.LUCKPERMS_STAFF_GROUPS.get()) {
                        if (user.getInheritedGroups(user.getQueryOptions()).stream()
                                .anyMatch(g -> g.getName().equals(group))) {
                            return true;
                        }
                    }
                }
            } catch (IllegalStateException e) {
                // LuckPerms not loaded - silently continue
            } catch (Exception e) {
                // LuckPerms error - log once and continue
                if (e.getMessage() != null && !e.getMessage().contains("not loaded")) {
                    OneriaServerUtilities.LOGGER.debug("LuckPerms check failed: {}", e.getMessage());
                }
            }
        }

        return false;
    }

    /**
     * Invalidates cache for a player (call on logout)
     */
    public static void invalidateCache(UUID playerUUID) {
        staffCache.remove(playerUUID);
    }

    /**
     * Clears entire cache (useful for config reload)
     */
    public static void clearCache() {
        staffCache.clear();
    }
}