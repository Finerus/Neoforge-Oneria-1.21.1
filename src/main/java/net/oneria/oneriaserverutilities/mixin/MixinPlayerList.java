package net.oneria.oneriaserverutilities.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.oneria.oneriaserverutilities.ColorHelper;
import net.oneria.oneriaserverutilities.NicknameManager;
import net.oneria.oneriaserverutilities.OneriaConfig;
import net.oneria.oneriaserverutilities.OneriaServerUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class MixinPlayerList {

    @Unique
    private boolean oneria$isSendingCustomMessage = false;

    /**
     * Intercepte TOUS les messages système pour filtrer join/leave vanilla
     */
    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    public void onBroadcastSystemMessage(Component component, boolean bl, CallbackInfo ci) {
        // Si on envoie notre propre message, laisser passer
        if (oneria$isSendingCustomMessage) {
            return;
        }

        String messageText = component.getString();

        try {
            if (OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE != null &&
                    OneriaConfig.ENABLE_CUSTOM_JOIN_LEAVE.get()) {

                // Détecter message de connexion
                if (messageText.contains("joined the game") ||
                        messageText.contains("a rejoint la partie")) {

                    ci.cancel(); // Cancel le message vanilla

                    String joinMsg = OneriaConfig.JOIN_MESSAGE.get();
                    if (!joinMsg.equalsIgnoreCase("none")) {
                        // Extraire le nom du joueur du message vanilla
                        String playerName = extractPlayerName(messageText, true);
                        sendCustomJoinMessage(playerName);
                    }

                    OneriaServerUtilities.LOGGER.debug("[Join] Cancelled vanilla message and sent custom");
                }
                // Détecter message de déconnexion
                else if (messageText.contains("left the game") ||
                        messageText.contains("a quitté la partie")) {

                    ci.cancel(); // Cancel le message vanilla

                    String leaveMsg = OneriaConfig.LEAVE_MESSAGE.get();
                    if (!leaveMsg.equalsIgnoreCase("none")) {
                        // Extraire le nom du joueur du message vanilla
                        String playerName = extractPlayerName(messageText, false);
                        sendCustomLeaveMessage(playerName);
                    }

                    OneriaServerUtilities.LOGGER.debug("[Leave] Cancelled vanilla message and sent custom");
                }
            }
        } catch (Exception e) {
            OneriaServerUtilities.LOGGER.debug("Config not loaded for join/leave messages");
        }
    }

    /**
     * Extrait le nom du joueur depuis le message vanilla
     */
    @Unique
    private String extractPlayerName(String message, boolean isJoin) {
        // Format: "PlayerName joined the game" ou "PlayerName left the game"
        if (isJoin) {
            if (message.contains("joined the game")) {
                return message.replace(" joined the game", "").trim();
            } else if (message.contains("a rejoint la partie")) {
                return message.replace(" a rejoint la partie", "").trim();
            }
        } else {
            if (message.contains("left the game")) {
                return message.replace(" left the game", "").trim();
            } else if (message.contains("a quitté la partie")) {
                return message.replace(" a quitté la partie", "").trim();
            }
        }
        return "Unknown";
    }

    /**
     * Envoie le message de connexion custom
     */
    @Unique
    private void sendCustomJoinMessage(String playerName) {
        try {
            String joinMsg = OneriaConfig.JOIN_MESSAGE.get();

            // Trouver le joueur pour obtenir son nickname
            ServerPlayer player = findPlayerByName(playerName);
            String nickname = player != null ? NicknameManager.getDisplayName(player) : playerName;

            String formatted = joinMsg
                    .replace("{player}", playerName)
                    .replace("{nickname}", nickname);

            Component message = ColorHelper.parseColors(formatted);

            // Envoyer le message custom
            oneria$isSendingCustomMessage = true;
            PlayerList playerList = (PlayerList)(Object)this;
            playerList.broadcastSystemMessage(message, false);
            oneria$isSendingCustomMessage = false;

            OneriaServerUtilities.LOGGER.debug("[Join] Sent custom message for {}", playerName);
        } catch (Exception e) {
            oneria$isSendingCustomMessage = false;
            OneriaServerUtilities.LOGGER.error("[Join] Error sending custom message", e);
        }
    }

    /**
     * Envoie le message de déconnexion custom
     */
    @Unique
    private void sendCustomLeaveMessage(String playerName) {
        try {
            String leaveMsg = OneriaConfig.LEAVE_MESSAGE.get();

            // Trouver le joueur pour obtenir son nickname
            ServerPlayer player = findPlayerByName(playerName);
            String nickname = player != null ? NicknameManager.getDisplayName(player) : playerName;

            String formatted = leaveMsg
                    .replace("{player}", playerName)
                    .replace("{nickname}", nickname);

            Component message = ColorHelper.parseColors(formatted);

            // Envoyer le message custom
            oneria$isSendingCustomMessage = true;
            PlayerList playerList = (PlayerList)(Object)this;
            playerList.broadcastSystemMessage(message, false);
            oneria$isSendingCustomMessage = false;

            OneriaServerUtilities.LOGGER.debug("[Leave] Sent custom message for {}", playerName);
        } catch (Exception e) {
            oneria$isSendingCustomMessage = false;
            OneriaServerUtilities.LOGGER.error("[Leave] Error sending custom message", e);
        }
    }

    /**
     * Trouve un joueur par son nom
     */
    @Unique
    private ServerPlayer findPlayerByName(String name) {
        try {
            PlayerList playerList = (PlayerList)(Object)this;
            for (ServerPlayer player : playerList.getPlayers()) {
                if (player.getName().getString().equals(name)) {
                    return player;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}