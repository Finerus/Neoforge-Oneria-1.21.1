package net.oneria.oneriaserverutilities.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.oneria.oneriaserverutilities.OneriaChatFormatter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin pour intercepter et formater les messages de chat
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {

    @Shadow
    public ServerPlayer player;

    /**
     * Intercepte l'envoi des messages de chat pour les formater
     */
    @Inject(
            method = "broadcastChatMessage",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onBroadcastChatMessage(PlayerChatMessage message, CallbackInfo ci) {
        // Formater le message avec notre système
        Component formattedMessage = OneriaChatFormatter.formatChatMessage(
                this.player,
                message.signedContent()
        );

        // Envoyer le message formaté à tous les joueurs
        this.player.getServer().getPlayerList().broadcastSystemMessage(
                formattedMessage,
                false
        );

        // Annuler l'envoi du message original
        ci.cancel();
    }
}