package net.rp.rpessentials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = RpEssentials.MODID)
public class RpEssentialsEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        LastConnectionManager.recordLogin(player);

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ProfessionSyncHelper.syncToPlayer(player);

        try {
            if (ScheduleConfig.ENABLE_WELCOME != null && ScheduleConfig.ENABLE_WELCOME.get()) {
                for (String line : ScheduleConfig.WELCOME_LINES.get()) {
                    String formatted = line.replace("%player%", player.getName().getString());
                    player.sendSystemMessage(ColorHelper.parseColors(
                            ColorHelper.translateAlternateColorCodes(formatted)));
                }
            }
        } catch (IllegalStateException e) {
            // config pas encore chargée
        }

        try {
            String soundId = ScheduleConfig.WELCOME_SOUND.get();
            if (soundId != null && !soundId.isBlank()) {
                ResourceLocation rl = ResourceLocation.tryParse(soundId);
                if (rl != null) {
                    SoundEvent sound = SoundEvent.createVariableRangeEvent(rl);
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.Holder.direct(sound),
                            SoundSource.MASTER,
                            player.getX(), player.getY(), player.getZ(),
                            1.0f, 1.0f,
                            player.getRandom().nextLong()
                    ));
                }
            }
        } catch (IllegalStateException e) {
            // config pas encore chargée
        }

        // Envoi legacy hideNametags (conservé pour rétrocompatibilité)
        try {
            boolean hideNametags = RpEssentialsConfig.HIDE_NAMETAGS.get();
            PacketDistributor.sendToPlayer(player, new HideNametagsPacket(hideNametags));
        } catch (IllegalStateException e) {
            // config pas encore chargée
        }

        // Sync nametag avancé : envoi après 500 ms pour laisser le client finir de charger
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            server.execute(() -> {
                // Envoie la config + la liste de tous les autres joueurs au nouveau joueur
                NametagSyncHelper.sendTo(player, server);
                // Met à jour tous les autres joueurs pour qu'ils connaissent le nouveau
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (!other.getUUID().equals(player.getUUID())) {
                        NametagSyncHelper.sendTo(other, server);
                    }
                }
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        LastConnectionManager.recordLogout(player);
        RpEssentialsMessagingManager.clearCache(player.getUUID());

        // Met à jour tous les joueurs restants pour retirer le joueur parti de leurs données
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    NametagSyncHelper.sendTo(other, server);
                }
            });
        }
    }
}