package net.oneria.oneriaserverutilities.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.oneria.oneriaserverutilities.NicknameManager;
import net.oneria.oneriaserverutilities.OneriaConfig;
import net.oneria.oneriaserverutilities.OneriaServerUtilities;
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
    private Packet modifyPacket(Packet packet) {
        // 1. Si le mod est désactivé, on renvoie le paquet original immédiatement
        if (!OneriaConfig.ENABLE_BLUR.get()) {
            return packet;
        }

        // 2. On cast l'objet "this" (le mixin) vers l'objet réel (ServerCommonPacketListenerImpl)
        Object self = this;

        // Vérifier que l'instance réelle est bien un ServerGamePacketListenerImpl
        if (!(self instanceof ServerGamePacketListenerImpl)) {
            return packet;
        }

        ServerGamePacketListenerImpl gameListener = (ServerGamePacketListenerImpl) self;
        ServerPlayer receiver = gameListener.player;
        if (receiver == null) {
            return packet;
        }

        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket)) {
            return packet;
        }

        ClientboundPlayerInfoUpdatePacket infoPacket = (ClientboundPlayerInfoUpdatePacket) packet;

        // 3. Vérification Whitelist
        boolean isWhitelisted = OneriaConfig.WHITELIST.get().contains(receiver.getGameProfile().getName());

        // 4. Vérification OP (si OPS_SEE_ALL est activé)
        boolean isOpExempt = OneriaConfig.OPS_SEE_ALL.get() && receiver.hasPermissions(2);

        // 5. Vérification debugSelfBlur - si activé, on ne doit PAS exempter le joueur
        boolean debugMode = OneriaConfig.DEBUG_SELF_BLUR.get();

        // Si le joueur est whitelisté OU OP avec exemption, ET que le mode debug n'est PAS activé
        // Les admins voient tout mais avec les nicknames affichés différemment
        boolean isAdmin = !debugMode && (isWhitelisted || isOpExempt);

        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = infoPacket.actions();

        // 6. Logique de remplacement
        boolean shouldProcess = actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) ||
                actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

        if (!shouldProcess) {
            return packet;
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> originalEntries = ((ClientboundPlayerInfoUpdatePacketAccessor) infoPacket).getEntries();
        List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries = new ArrayList<>();
        double maxDistSq = Math.pow(OneriaConfig.PROXIMITY_DISTANCE.get(), 2);

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : originalEntries) {
            ServerPlayer targetPlayer = receiver.server.getPlayerList().getPlayer(entry.profileId());
            Component displayName;

            if (targetPlayer != null) {
                String prefix = OneriaServerUtilities.getPlayerPrefix(targetPlayer);
                String realName = targetPlayer.getGameProfile().getName();
                boolean hasNickname = NicknameManager.hasNickname(targetPlayer.getUUID());
                String nickname = null;

                if (hasNickname) {
                    nickname = NicknameManager.getNickname(targetPlayer.getUUID());
                }

                // CAS 1: Admin qui voit tout
                if (isAdmin) {
                    if (hasNickname && nickname != null) {
                        // Admin voit: Prefix + Nickname + §7§o(RealName)
                        String fullDisplay = prefix + nickname + " §7§o(" + realName + ")";
                        displayName = Component.literal(fullDisplay);
                    } else {
                        // Pas de nickname, affichage normal
                        displayName = Component.literal(prefix + realName);
                    }
                }
                // CAS 2: Joueur normal - vérifier la distance
                else {
                    // En mode debug, on floute même soi-même
                    double distSq = receiver.distanceToSqr(targetPlayer);
                    boolean shouldBlur = debugMode || (distSq > maxDistSq);

                    String displayedName;
                    if (hasNickname && nickname != null) {
                        displayedName = nickname;
                    } else {
                        displayedName = realName;
                    }

                    if (shouldBlur) {
                        // Flouter le nom - AUCUN préfixe pour ne pas révéler les grades
                        String cleanName = displayedName.replaceAll("§.", "");
                        int obfLength = Math.min(cleanName.length(), OneriaConfig.OBFUSCATED_NAME_LENGTH.get());
                        String obfuscatedPart = "§k" + "?".repeat(obfLength);
                        displayName = Component.literal(obfuscatedPart);
                    } else {
                        // Afficher le nickname (ou nom réel si pas de nickname) avec le préfixe
                        displayName = Component.literal(prefix + displayedName);
                    }
                }
            } else {
                displayName = entry.displayName();
            }

            ClientboundPlayerInfoUpdatePacket.Entry newEntry = new ClientboundPlayerInfoUpdatePacket.Entry(
                    entry.profileId(),
                    entry.profile(),
                    entry.listed(),
                    entry.latency(),
                    entry.gameMode(),
                    displayName,
                    entry.chatSession()
            );

            newEntries.add(newEntry);
        }

        // Injection via constructeur vide + Setter Accessor
        ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(actions, List.of());
        ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(newEntries);
        return newPacket;
    }
}