package net.oneria.oneriaserverutilities;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class OneriaEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        OneriaServerUtilities.LOGGER.info("Player {} logged in", player.getName().getString());

        boolean hideNametags = OneriaConfig.HIDE_NAMETAGS.get();
        PacketDistributor.sendToPlayer(player, new HideNametagsPacket(hideNametags));
        OneriaServerUtilities.LOGGER.info("Sent nametag config to {}: hide={}", player.getName().getString(), hideNametags);

        if (NicknameManager.hasNickname(player.getUUID())) {
            String nickname = NicknameManager.getNickname(player.getUUID());
            String nametagDisplay;

            if (OneriaConfig.SHOW_NAMETAG_PREFIX_SUFFIX.get()) {
                String prefix = OneriaServerUtilities.getPlayerPrefix(player);
                String suffix = OneriaServerUtilities.getPlayerSuffix(player);
                nametagDisplay = prefix + nickname + suffix;
            } else {
                nametagDisplay = nickname;
            }

            player.setCustomName(Component.literal(nametagDisplay.replace("&", "§")));
            player.setCustomNameVisible(true);
        }

        // Execute after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                player.getServer().execute(() -> {
                    checkScheduleOnJoin(player);
                    sendWelcomeMessage(player);
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

        OneriaPermissions.invalidateCache(player.getUUID());

        WorldBorderManager.clearCache(player.getUUID());
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