package net.oneria.oneriaserverutilities;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneriaMessagingManager {

    private static final Map<UUID, UUID> lastMessaged = new HashMap<>();

    /**
     * Envoie un message privé entre deux joueurs avec format Oneria
     */
    public static boolean sendMessage(ServerPlayer sender, ServerPlayer recipient, String message) {
        String senderDisplay = NicknameManager.getDisplayName(sender);
        String recipientDisplay = NicknameManager.getDisplayName(recipient);

        // Message pour le destinataire : "[SenderNick] vous écrit : message"
        MutableComponent senderClickable = Component.literal("§6" + senderDisplay + "§r")
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§7Cliquer pour répondre")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/msg " + sender.getName().getString() + " ")));

        MutableComponent toRecipient = Component.literal("§8[MP] ")
                .append(senderClickable)
                .append(Component.literal("§7 vous écrit : §f" + message));

        // Message pour l'expéditeur : "Vous écrivez à [RecipientNick] : message"
        MutableComponent recipientClickable = Component.literal("§6" + recipientDisplay + "§r")
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§7Cliquer pour répondre")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/msg " + recipient.getName().getString() + " ")));

        MutableComponent toSender = Component.literal("§8[MP] §7Vous écrivez à ")
                .append(recipientClickable)
                .append(Component.literal("§7 : §f" + message));

        recipient.sendSystemMessage(toRecipient);
        sender.sendSystemMessage(toSender);

        lastMessaged.put(sender.getUUID(), recipient.getUUID());
        lastMessaged.put(recipient.getUUID(), sender.getUUID());

        OneriaServerUtilities.LOGGER.info("[MSG] {} -> {}: {}",
                sender.getName().getString(), recipient.getName().getString(), message);
        return true;
    }

    /**
     * Répond au dernier interlocuteur via /r
     */
    public static int reply(ServerPlayer sender, String message, MinecraftServer server) {
        UUID lastId = lastMessaged.get(sender.getUUID());
        if (lastId == null) {
            sender.sendSystemMessage(Component.literal("§c[MP] Vous n'avez personne à qui répondre."));
            return 0;
        }
        ServerPlayer target = server.getPlayerList().getPlayer(lastId);
        if (target == null) {
            sender.sendSystemMessage(Component.literal("§c[MP] Ce joueur n'est plus connecté."));
            return 0;
        }
        return sendMessage(sender, target, message) ? 1 : 0;
    }

    public static void clearCache(UUID playerId) {
        lastMessaged.remove(playerId);
    }
}