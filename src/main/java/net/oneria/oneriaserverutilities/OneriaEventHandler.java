package net.oneria.oneriaserverutilities;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class OneriaEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        OneriaServerUtilities.LOGGER.info("Player {} logged in", player.getName().getString());

        // Custom join message - AVEC PROTECTION
        try {
            if (OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE != null &&
                    OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE.get()) {

                String joinMsg = OneriaConfig.JOIN_MESSAGE.get();
                if (!joinMsg.equalsIgnoreCase("none")) {
                    String nickname = NicknameManager.getDisplayName(player);
                    String formatted = joinMsg
                            .replace("{player}", player.getName().getString())
                            .replace("{nickname}", nickname);

                    Component message = ColorHelper.parseColors(formatted);
                    player.getServer().getPlayerList().broadcastSystemMessage(message, false);
                }
            }
        } catch (Exception e) {
            // Config pas encore chargée, on skip silencieusement
            OneriaServerUtilities.LOGGER.debug("Config not loaded yet for join message");
        }

        // Execute after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                player.getServer().execute(() -> {
                    checkScheduleOnJoin(player);
                    sendWelcomeMessage(player);

                    // Synchroniser les nametags pour ce joueur
                    NametagManager.onPlayerJoin(player);

                    // Re-synchroniser pour tout le monde au cas où
                    NametagManager.syncNametagVisibility(player.getServer());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        OneriaServerUtilities.LOGGER.info("Player {} logged out", player.getName().getString());

        // Custom leave message - AVEC PROTECTION
        try {
            if (OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE != null &&
                    OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE.get()) {

                String leaveMsg = OneriaConfig.LEAVE_MESSAGE.get();
                if (!leaveMsg.equalsIgnoreCase("none")) {
                    String nickname = NicknameManager.getDisplayName(player);
                    String formatted = leaveMsg
                            .replace("{player}", player.getName().getString())
                            .replace("{nickname}", nickname);

                    Component message = ColorHelper.parseColors(formatted);
                    player.getServer().getPlayerList().broadcastSystemMessage(message, false);
                }
            }
        } catch (Exception e) {
            // Config pas encore chargée, on skip silencieusement
            OneriaServerUtilities.LOGGER.debug("Config not loaded yet for leave message");
        }

        // Nettoyer le cache des permissions
        OneriaPermissions.invalidateCache(player.getUUID());

        // Nettoyer le cache des warnings de border
        WorldBorderManager.clearCache(player.getUUID());

        // Retirer le joueur de la team des nametags
        NametagManager.onPlayerLogout(player);

        // Re-synchroniser pour les joueurs restants
        if (player.getServer() != null) {
            NametagManager.syncNametagVisibility(player.getServer());
        }
    }

    private static void checkScheduleOnJoin(ServerPlayer player) {
        Component kickMessage = OneriaScheduleManager.canPlayerJoin(player);

        if (kickMessage != null) {
            player.connection.disconnect(kickMessage);
            OneriaServerUtilities.LOGGER.info("Kicked {} (server closed, non-staff)", player.getName().getString());
        } else if (OneriaConfig.ENABLE_SCHEDULE.get()) {
            if (OneriaPermissions.isStaff(player)) {
                if (!OneriaScheduleManager.isServerOpen()) {
                    player.sendSystemMessage(Component.literal("§6[STAFF] Connection authorized (server closed)."));
                }
            }

            String timeInfo = OneriaScheduleManager.getTimeUntilNextEvent();
            player.sendSystemMessage(Component.literal("§7" + timeInfo));
        }
    }

    private static void sendWelcomeMessage(ServerPlayer player) {
        if (!OneriaConfig.ENABLE_WELCOME.get()) return;

        String playerName = player.getName().getString();
        String displayName = player.getDisplayName().getString();

        for (String line : OneriaConfig.WELCOME_LINES.get()) {
            String formattedLine = line
                    .replace("{player}", playerName)
                    .replace("{nickname}", displayName);

            player.sendSystemMessage(Component.literal(formattedLine));
        }

        String soundName = OneriaConfig.WELCOME_SOUND.get();
        if (!soundName.isEmpty()) {
            try {
                ResourceLocation soundLocation = ResourceLocation.parse(soundName);
                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);

                player.playNotifySound(
                        soundEvent,
                        SoundSource.MASTER,
                        OneriaConfig.WELCOME_SOUND_VOLUME.get().floatValue(),
                        OneriaConfig.WELCOME_SOUND_PITCH.get().floatValue()
                );
            } catch (Exception e) {
                OneriaServerUtilities.LOGGER.warn("Failed to play welcome sound: {}", soundName);
            }
        }
    }
}