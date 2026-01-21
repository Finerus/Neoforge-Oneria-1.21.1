package net.oneria.oneriaserverutilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldBorderManager {

    // Track si le joueur a déjà été averti (reset quand il revient dans la zone safe)
    private static final Map<UUID, Boolean> hasBeenWarned = new HashMap<>();
    private static boolean systemInitialized = false;

    /**
     * Check all players for world border proximity
     * Called from server tick
     */
    public static void tick(MinecraftServer server) {
        // PROTECTION: Vérifier que la config est chargée
        try {
            if (OneriaConfig.ENABLE_WORLD_BORDER_WARNING == null) {
                return;
            }

            if (!OneriaConfig.ENABLE_WORLD_BORDER_WARNING.get()) {
                return;
            }

            // Log une seule fois l'initialisation
            if (!systemInitialized) {
                systemInitialized = true;
                OneriaServerUtilities.LOGGER.info("[WorldBorder] System initialized - Distance: {} blocks",
                        OneriaConfig.WORLD_BORDER_DISTANCE.get());
            }
        } catch (Exception e) {
            // Config pas encore chargée, on skip silencieusement
            return;
        }

        // Check only at configured interval
        try {
            if (server.getTickCount() % OneriaConfig.WORLD_BORDER_CHECK_INTERVAL.get() != 0) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        double maxDist = OneriaConfig.WORLD_BORDER_DISTANCE.get();
        double maxDistSq = maxDist * maxDist;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                checkPlayerDistance(player, maxDistSq, maxDist);
            } catch (Exception e) {
                OneriaServerUtilities.LOGGER.error("[WorldBorder] Error checking player {}", player.getName().getString(), e);
            }
        }
    }

    /**
     * Check if a player has exceeded the border distance
     */
    private static void checkPlayerDistance(ServerPlayer player, double maxDistSq, double maxDist) {
        BlockPos spawn = player.serverLevel().getSharedSpawnPos();

        // Calculer la distance depuis le spawn (en 2D, sans Y)
        double dx = player.getX() - spawn.getX();
        double dz = player.getZ() - spawn.getZ();
        double distSq = dx * dx + dz * dz;
        double actualDist = Math.sqrt(distSq);

        UUID playerId = player.getUUID();
        boolean isOutsideBorder = distSq > maxDistSq;
        Boolean alreadyWarned = hasBeenWarned.getOrDefault(playerId, false);

        if (isOutsideBorder && !alreadyWarned) {
            // Le joueur vient de dépasser la limite pour la première fois
            OneriaServerUtilities.LOGGER.info("[WorldBorder] Player {} exceeded border at {}/{} blocks",
                    player.getName().getString(), String.format("%.1f", actualDist), String.format("%.0f", maxDist));
            sendBorderWarning(player, actualDist);
            hasBeenWarned.put(playerId, true);
        } else if (!isOutsideBorder && alreadyWarned) {
            // Le joueur est revenu dans la zone safe, on reset le warning
            OneriaServerUtilities.LOGGER.info("[WorldBorder] Player {} returned to safe zone",
                    player.getName().getString());
            hasBeenWarned.put(playerId, false);
        }
    }

    /**
     * Send border warning message to player (ONE TIME ONLY)
     */
    private static void sendBorderWarning(ServerPlayer player, double distance) {
        try {
            String message = OneriaConfig.WORLD_BORDER_MESSAGE.get()
                    .replace("{distance}", String.format("%.0f", distance))
                    .replace("{player}", player.getName().getString());

            Component formatted = ColorHelper.parseColors(message);
            player.sendSystemMessage(formatted);

            // Play warning sound
            player.playNotifySound(
                    SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.MASTER,
                    1.0f,
                    0.5f
            );

            OneriaServerUtilities.LOGGER.info("[WorldBorder] Warning sent to {}", player.getName().getString());
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.error("[WorldBorder] Error sending warning to player", e);
        }
    }

    /**
     * Clear warning cache for a player (on logout)
     */
    public static void clearCache(UUID playerId) {
        hasBeenWarned.remove(playerId);
    }

    /**
     * Clear all warning cache
     */
    public static void clearAllCache() {
        hasBeenWarned.clear();
        systemInitialized = false;
    }
}