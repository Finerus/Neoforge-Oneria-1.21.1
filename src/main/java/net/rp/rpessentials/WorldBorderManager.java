package net.rp.rpessentials;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;

import java.util.*;

public class WorldBorderManager {

    private static final Map<UUID, Boolean> hasBeenWarned = new HashMap<>();
    private static final Map<UUID, Set<String>> playerZoneState = new HashMap<>();
    private static boolean systemInitialized = false;

    public static void tick(MinecraftServer server) {
        try {
            if (RpEssentialsConfig.ENABLE_WORLD_BORDER_WARNING == null) return;
            if (!RpEssentialsConfig.ENABLE_WORLD_BORDER_WARNING.get()) return;
            if (!systemInitialized) {
                systemInitialized = true;
                RpEssentials.LOGGER.info("[WorldBorder] System initialized");
            }
        } catch (Exception e) { return; }

        try {
            if (server.getTickCount() % RpEssentialsConfig.WORLD_BORDER_CHECK_INTERVAL.get() != 0) return;
        } catch (Exception e) { return; }

        double maxDist = RpEssentialsConfig.WORLD_BORDER_DISTANCE.get();
        double maxDistSq = maxDist * maxDist;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                checkPlayerDistance(player, maxDistSq, maxDist);
                checkNamedZones(player);
            } catch (Exception e) {
                RpEssentials.LOGGER.error("[WorldBorder] Error checking player {}", player.getName().getString(), e);
            }
        }
    }

    private static void checkPlayerDistance(ServerPlayer player, double maxDistSq, double maxDist) {
        BlockPos spawn = player.serverLevel().getSharedSpawnPos();
        double dx = player.getX() - spawn.getX();
        double dz = player.getZ() - spawn.getZ();
        double distSq = dx * dx + dz * dz;
        double actualDist = Math.sqrt(distSq);

        UUID playerId = player.getUUID();
        boolean isOutsideBorder = distSq > maxDistSq;
        Boolean alreadyWarned = hasBeenWarned.getOrDefault(playerId, false);

        if (isOutsideBorder && !alreadyWarned) {
            RpEssentials.LOGGER.info("[WorldBorder] Player {} exceeded border at {}/{} blocks",
                    player.getName().getString(), String.format("%.1f", actualDist), String.format("%.0f", maxDist));
            sendBorderWarning(player, actualDist);
            hasBeenWarned.put(playerId, true);
        } else if (!isOutsideBorder && alreadyWarned) {
            hasBeenWarned.put(playerId, false);
        }
    }

    private static void checkNamedZones(ServerPlayer player) {
        List<? extends String> zones;
        try {
            zones = RpEssentialsConfig.NAMED_ZONES.get();
        } catch (Exception e) { return; }

        UUID playerId = player.getUUID();
        Set<String> currentZones = playerZoneState.computeIfAbsent(playerId, k -> new HashSet<>());

        for (String zoneDef : zones) {
            String[] parts = zoneDef.split(";");
            if (parts.length < 5) continue;
            try {
                String zoneName = parts[0].trim();
                double cx = Double.parseDouble(parts[1].trim());
                double cz = Double.parseDouble(parts[2].trim());
                double radius = Double.parseDouble(parts[3].trim());
                String msgEnter = parts[4].trim();
                String msgExit = parts.length >= 6 ? parts[5].trim() : "";

                double dx = player.getX() - cx;
                double dz = player.getZ() - cz;
                boolean inZone = (dx * dx + dz * dz) <= (radius * radius);
                boolean wasInZone = currentZones.contains(zoneName);

                if (inZone && !wasInZone) {
                    currentZones.add(zoneName);
                    sendZoneMessage(player, msgEnter);
                } else if (!inZone && wasInZone) {
                    currentZones.remove(zoneName);
                    if (!msgExit.isEmpty()) sendZoneMessage(player, msgExit);
                }
            } catch (Exception e) {
                RpEssentials.LOGGER.warn("[WorldBorder] Invalid zone definition: {}", zoneDef);
            }
        }
    }

    private static void sendZoneMessage(ServerPlayer player, String message) {
        try {
            sendImmersiveOrFallback(player, message, 5f);
        } catch (Exception e) {
            RpEssentials.LOGGER.error("[WorldBorder] Error sending zone message", e);
        }
    }

    private static void sendBorderWarning(ServerPlayer player, double distance) {
        try {
            String message = RpEssentialsConfig.WORLD_BORDER_MESSAGE.get()
                    .replace("{distance}", String.format("%.0f", distance))
                    .replace("{player}", player.getName().getString());

            sendImmersiveOrFallback(player, message, 6f);

            player.playNotifySound(
                    SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.MASTER,
                    1.0f,
                    0.5f
            );
        } catch (Exception e) {
            RpEssentials.LOGGER.error("[WorldBorder] Error sending border warning", e);
        }
    }

    private static void sendImmersiveOrFallback(ServerPlayer player, String message, float duration) {
        String mode = "ACTION_BAR";
        try {
            mode = RpEssentialsConfig.ZONE_MESSAGE_MODE.get().toUpperCase();
        } catch (Exception ignored) {}

        Component formatted = ColorHelper.parseColors(message);

        switch (mode) {
            case "IMMERSIVE" -> {
                try {
                    toni.immersivemessages.api.ImmersiveMessage.builder(duration, message)
                            .fadeIn(0.5f)
                            .fadeOut(0.5f)
                            .sendServer(player);
                } catch (Exception e) {
                    // Fallback si le mod client n'est pas présent
                    player.displayClientMessage(formatted, true);
                    RpEssentials.LOGGER.warn("[WorldBorder] ImmersiveMessageAPI unavailable, falling back to action bar");
                }
            }
            case "CHAT" -> player.sendSystemMessage(formatted);
            default -> player.displayClientMessage(formatted, true); // ACTION_BAR
        }
    }

    public static void clearCache(UUID playerId) {
        hasBeenWarned.remove(playerId);
        playerZoneState.remove(playerId);
    }

    public static void clearAllCache() {
        hasBeenWarned.clear();
        playerZoneState.clear();
        systemInitialized = false;
    }
}