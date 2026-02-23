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
    public static int sendMessage(ServerPlayer sender, ServerPlayer target, String message) {
        UUID senderUuid = sender.getUUID();
        UUID targetUuid = target.getUUID();

        lastMessaged.put(targetUuid, senderUuid);
        lastMessaged.put(senderUuid, targetUuid);

        String senderNick = NicknameManager.getNickname(senderUuid);
        String targetNick = NicknameManager.getNickname(targetUuid);
        String senderDisplay = (senderNick != null) ? senderNick : sender.getName().getString();
        String targetDisplay = (targetNick != null) ? targetNick : target.getName().getString();

        MutableComponent senderName = Component.literal(targetDisplay)
                .withStyle(style -> style
                        .withBold(true)
                        .withColor(0x999999)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Cliquer pour répondre")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/msg " + target.getName().getString() + " ")));

        MutableComponent targetName = Component.literal(senderDisplay)
                .withStyle(style -> style
                        .withBold(true)
                        .withColor(0x999999)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Cliquer pour répondre")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/msg " + sender.getName().getString() + " ")));

        MutableComponent toSender = Component.literal("§7[MP] Vous écrivez à ")
                .append(senderName)
                .append(Component.literal("§7 : " + message));

        MutableComponent toTarget = Component.literal("§7[MP] ")
                .append(targetName)
                .append(Component.literal("§7 vous écrit : " + message));

        sender.sendSystemMessage(toSender);
        target.sendSystemMessage(toTarget);

        try {
            if (OneriaConfig.LOG_PRIVATE_MESSAGES.get()) {
                OneriaServerUtilities.LOGGER.info("[MP] {} -> {}: {}",
                        sender.getName().getString(),
                        target.getName().getString(),
                        message);
            }
        } catch (Exception ignored) {}

        return 1;
    }

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
        return sendMessage(sender, target, message);
    }

    public static void clearCache(UUID playerId) {
        lastMessaged.remove(playerId);
    }
}