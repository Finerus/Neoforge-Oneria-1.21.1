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
        // 1. If mod is disabled, return original packet immediately
        if (!OneriaConfig.ENABLE_BLUR.get()) return packet;

        // 2. Cast "this" (the mixin) to the real object (ServerCommonPacketListenerImpl)
        Object self = this;

        // Check that the real instance is indeed a ServerGamePacketListenerImpl
        if (!(self instanceof ServerGamePacketListenerImpl)) {
            return packet;
        }

        ServerGamePacketListenerImpl gameListener = (ServerGamePacketListenerImpl) self;
        ServerPlayer receiver = gameListener.player;
        if (receiver == null) return packet;

        if (packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket) {
            // 3. Whitelist Check
            boolean isWhitelisted = OneriaConfig.WHITELIST.get().contains(receiver.getGameProfile().getName());

            // 4. OP Check (if OPS_SEE_ALL is enabled)
            boolean isOpExempt = OneriaConfig.OPS_SEE_ALL.get() && receiver.hasPermissions(2);

            // If player is whitelisted OR OP with exemption, they see everything
            if (isWhitelisted || isOpExempt) return packet;

            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = infoPacket.actions();

            // 5. Replacement Logic
            if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) ||
                    actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {

                List<ClientboundPlayerInfoUpdatePacket.Entry> originalEntries = ((ClientboundPlayerInfoUpdatePacketAccessor) infoPacket).getEntries();
                List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries = new ArrayList<>();
                double maxDistSq = Math.pow(OneriaConfig.PROXIMITY_DISTANCE.get(), 2);

                for (ClientboundPlayerInfoUpdatePacket.Entry entry : originalEntries) {
                    ServerPlayer targetPlayer = receiver.server.getPlayerList().getPlayer(entry.profileId());
                    Component displayName;

                    if (targetPlayer != null) {
                        // Retrieve prefix and name
                        String prefix = OneriaMod.getPlayerPrefix(targetPlayer);
                        String name = targetPlayer.getGameProfile().getName();

                        // Check if we need to obfuscate
                        boolean isSelf = targetPlayer.getUUID().equals(receiver.getUUID());
                        boolean shouldBlur = false;

                        if (isSelf) {
                            // Debug mode: blur self if DEBUG_SELF_BLUR is enabled
                            shouldBlur = OneriaConfig.DEBUG_SELF_BLUR.get();
                        } else {
                            // For other players: check distance
                            double distSq = receiver.distanceToSqr(targetPlayer);
                            shouldBlur = distSq > maxDistSq;
                        }

                        if (shouldBlur) {
                            // Get configured length for obfuscated name
                            int nameLength = OneriaConfig.OBFUSCATED_NAME_LENGTH.get();
                            String obfuscatedName = "X".repeat(nameLength);

                            // Check if we should also obfuscate prefix
                            if (OneriaConfig.OBFUSCATE_PREFIX.get() && !prefix.isEmpty()) {
                                // Obfuscate prefix too (keeping approximate length)
                                String obfuscatedPrefix = "§k" + "X".repeat(Math.max(1, prefix.length() / 2));
                                displayName = Component.literal(obfuscatedPrefix + " §k" + obfuscatedName);
                            } else {
                                // Do not obfuscate prefix, only name
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

                // Injection via empty constructor + Setter Accessor
                ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
                ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(newEntries);
                return newPacket;
            }
        }

        // Mandatory default return
        return packet;
    }
}