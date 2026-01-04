package net.oneria.oneriamod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.oneria.oneriamod.OneriaConfig;
import net.oneria.oneriamod.OneriaMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MixinServerCommonPacketListenerImpl {

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            argsOnly = true,
            remap = false
    )
    private Packet<?> modifyPacket(Packet<?> packet) {
        // 1. Si le mod est désactivé, on renvoie le paquet original immédiatement
        if (!OneriaConfig.ENABLE_BLUR.get()) return packet;

        // 2. On cast l'objet "this" (le mixin) vers l'objet réel (ServerCommonPacketListenerImpl)
        Object self = this;

        // Vérifier que l'instance réelle est bien un ServerGamePacketListenerImpl
        if (!(self instanceof ServerGamePacketListenerImpl)) {
            return packet;
        }

        ServerGamePacketListenerImpl gameListener = (ServerGamePacketListenerImpl) self;
        ServerPlayer receiver = gameListener.player;
        if (receiver == null) return packet;

        if (packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket) {
            // 3. Vérification Whitelist
            boolean isWhitelisted = OneriaConfig.WHITELIST.get().contains(receiver.getGameProfile().getName());

            // 4. Vérification OP (si OPS_SEE_ALL est activé)
            boolean isOpExempt = OneriaConfig.OPS_SEE_ALL.get() && receiver.hasPermissions(2);

            // Si le joueur est whitelisté OU OP avec exemption, il voit tout
            if (isWhitelisted || isOpExempt) return packet;

            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = infoPacket.actions();

            // 5. Logique de remplacement
            if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) ||
                    actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {

                List<ClientboundPlayerInfoUpdatePacket.Entry> originalEntries = ((ClientboundPlayerInfoUpdatePacketAccessor) infoPacket).getEntries();
                List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries = new ArrayList<>();
                double maxDistSq = Math.pow(OneriaConfig.PROXIMITY_DISTANCE.get(), 2);

                for (ClientboundPlayerInfoUpdatePacket.Entry entry : originalEntries) {
                    ServerPlayer targetPlayer = receiver.server.getPlayerList().getPlayer(entry.profileId());
                    Component displayName;

                    if (targetPlayer != null) {
                        // Récupérer le préfixe et le nom
                        String prefix = OneriaMod.getPlayerPrefix(targetPlayer);
                        String name = targetPlayer.getGameProfile().getName();

                        // Vérifier si on doit obfusquer
                        boolean isSelf = targetPlayer.getUUID().equals(receiver.getUUID());
                        boolean shouldBlur = false;

                        if (isSelf) {
                            // Mode debug : s'obfusquer soi-même si DEBUG_SELF_BLUR est activé
                            shouldBlur = OneriaConfig.DEBUG_SELF_BLUR.get();
                        } else {
                            // Pour les autres joueurs : vérifier la distance
                            double distSq = receiver.distanceToSqr(targetPlayer);
                            shouldBlur = distSq > maxDistSq;
                        }

                        if (shouldBlur) {
                            // Récupérer la longueur configurée pour le pseudo obfusqué
                            int nameLength = OneriaConfig.OBFUSCATED_NAME_LENGTH.get();
                            String obfuscatedName = "X".repeat(nameLength);

                            // Vérifier si on doit aussi obfusquer le préfixe
                            if (OneriaConfig.OBFUSCATE_PREFIX.get() && !prefix.isEmpty()) {
                                // Obfusquer le préfixe aussi (on garde sa longueur approximative)
                                String obfuscatedPrefix = "§k" + "X".repeat(Math.max(1, prefix.length() / 2));
                                displayName = Component.literal(obfuscatedPrefix + " §k" + obfuscatedName);
                            } else {
                                // Ne pas obfusquer le préfixe, seulement le pseudo
                                displayName = Component.literal(prefix + "§k" + obfuscatedName);
                            }
                        } else {
                            displayName = Component.literal(prefix + name);
                        }
                    } else {
                        displayName = entry.displayName();
                    }

                    newEntries.add(new ClientboundPlayerInfoUpdatePacket.Entry(
                            entry.profileId(), entry.profile(), entry.listed(),
                            entry.latency(), entry.gameMode(), displayName, entry.chatSession()
                    ));
                }

                // Injection via constructeur vide + Setter Accessor
                ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
                ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(newEntries);
                return newPacket;
            }
        }

        // Retour par défaut OBLIGATOIRE
        return packet;
    }
}