package net.rp.rpessentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.rp.rpessentials.ColorHelper;
import net.rp.rpessentials.RpEssentialsPermissions;
import net.rp.rpessentials.config.MessagesConfig;
import net.rp.rpessentials.identity.RpEssentialsChatFormatter;
import net.rp.rpessentials.config.ChatConfig;
import net.rp.rpessentials.moderation.MuteManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepte le chat pour appliquer le formatage personnalisé.
 *
 * 4.1.6 : le format du spy log de proximité utilise resolveRpPlaceholders
 * pour supporter {player}/{nick}/{real}/{nick_real}.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "broadcastChatMessage",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onBroadcastChatMessage(PlayerChatMessage chatMessage, CallbackInfo ci) {
        if (!ChatConfig.ENABLE_CHAT_FORMAT.get()) return;

        String message = chatMessage.signedContent();
        if (message.startsWith("/")) return;

        ci.cancel();

        // Vérification mute
        if (MuteManager.isMuted(player.getUUID())) {
            MuteManager.MuteEntry entry = MuteManager.getEntry(player.getUUID());
            if (entry != null) {
                player.sendSystemMessage(ColorHelper.parseColors(
                        MessagesConfig.get(MessagesConfig.MUTE_BLOCKED_MESSAGE,
                                "expiry", entry.isPermanent() ? "Permanent" : entry.getFormattedExpiry())));
            }
            return;
        }

        // Chat de proximité
        boolean proximityEnabled;
        try { proximityEnabled = ChatConfig.ENABLE_PROXIMITY_CHAT.get(); }
        catch (IllegalStateException e) { proximityEnabled = false; }

        boolean isGlobal      = !proximityEnabled;
        String  actualMessage = message;

        if (proximityEnabled) {
            try {
                String bypass = ChatConfig.PROXIMITY_CHAT_BYPASS_PREFIX.get();
                if (message.startsWith(bypass)) {
                    actualMessage = message.substring(bypass.length());
                    isGlobal      = true;
                }
            } catch (IllegalStateException ignored) { isGlobal = true; }
        }

        Component formatted = RpEssentialsChatFormatter.formatChatMessage(player, actualMessage, isGlobal);

        if (isGlobal) {
            player.getServer().getPlayerList().broadcastSystemMessage(formatted, false);
        } else {
            int distance;
            try { distance = ChatConfig.PROXIMITY_CHAT_DISTANCE.get(); }
            catch (IllegalStateException e) { distance = 32; }
            double distSq = (double) distance * distance;

            for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                if (p.getUUID().equals(player.getUUID())
                        || (p.level() == player.level() && p.distanceToSqr(player) <= distSq)) {
                    p.sendSystemMessage(formatted);
                }
            }

            // Spy staff hors portée — résolution complète des placeholders
            String rawSpy = MessagesConfig.get(MessagesConfig.PROXIMITY_CHAT_SPY_FORMAT,
                    "msg",      actualMessage,
                    "distance", String.valueOf(distance));
            // Support {player}/{nick}/{real}/{nick_real} dans le format spy
            rawSpy = RpEssentialsChatFormatter.resolveRpPlaceholders(rawSpy, player);
            Component spyComp = ColorHelper.parseColors(rawSpy);

            for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                if (RpEssentialsPermissions.isStaff(p)
                        && !p.getUUID().equals(player.getUUID())
                        && (p.level() != player.level() || p.distanceToSqr(player) > distSq)) {
                    p.sendSystemMessage(spyComp);
                }
            }
        }
    }
}