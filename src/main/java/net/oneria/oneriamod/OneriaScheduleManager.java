package net.oneria.oneriamod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OneriaScheduleManager {

    private static LocalTime openingTime;
    private static LocalTime closingTime;
    private static final Set<Integer> sentWarnings = new HashSet<>();
    private static boolean hasClosedToday = false;
    private static boolean hasOpenedToday = false;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initializes or reloads schedule from config
     */
    public static void reload() {
        try {
            openingTime = LocalTime.parse(OneriaConfig.OPENING_TIME.get(), TIME_FORMATTER);
            closingTime = LocalTime.parse(OneriaConfig.CLOSING_TIME.get(), TIME_FORMATTER);
            sentWarnings.clear();
            hasClosedToday = false;
            hasOpenedToday = false;
        } catch (Exception e) {
            OneriaMod.LOGGER.error("Error parsing schedule times: " + e.getMessage());
            // Default values on error
            openingTime = LocalTime.of(19, 0);
            closingTime = LocalTime.of(23, 59);
        }
    }

    /**
     * Checks if the server is currently open
     */
    public static boolean isServerOpen() {
        if (!OneriaConfig.ENABLE_SCHEDULE.get()) return true;

        LocalTime now = LocalTime.now();

        // If closing is after midnight (e.g., 02:00)
        if (closingTime.isBefore(openingTime)) {
            return now.isAfter(openingTime) || now.isBefore(closingTime);
        }

        // Normal schedule
        return !now.isBefore(openingTime) && now.isBefore(closingTime);
    }

    /**
     * Called every tick to manage schedule
     */
    public static void tick(MinecraftServer server) {
        if (!OneriaConfig.ENABLE_SCHEDULE.get()) return;

        // Check only every 20 seconds (400 ticks)
        if (server.getTickCount() % 400 != 0) return;

        LocalTime now = LocalTime.now();

        // Reset flags at midnight
        if (now.getHour() == 0 && now.getMinute() == 0) {
            hasClosedToday = false;
            hasOpenedToday = false;
            sentWarnings.clear();
        }

        // Opening message (once per day)
        if (!hasOpenedToday && now.getHour() == openingTime.getHour() && now.getMinute() == openingTime.getMinute()) {
            hasOpenedToday = true;
            sendOpeningMessage(server);
        }

        // Warnings before closing
        if (isServerOpen()) {
            checkWarnings(server, now);
        }

        // Automatic closing
        if (!hasClosedToday && now.getHour() == closingTime.getHour() && now.getMinute() == closingTime.getMinute()) {
            hasClosedToday = true;
            closeServer(server);
        }
    }

    private static void checkWarnings(MinecraftServer server, LocalTime now) {
        for (int minutes : OneriaConfig.WARNING_TIMES.get()) {
            LocalTime warningTime = closingTime.minusMinutes(minutes);

            if (now.getHour() == warningTime.getHour() &&
                    now.getMinute() == warningTime.getMinute() &&
                    !sentWarnings.contains(minutes)) {

                sentWarnings.add(minutes);

                String message;
                if (minutes == 1) {
                    message = OneriaConfig.MSG_CLOSING_IMMINENT.get();
                } else {
                    message = OneriaConfig.MSG_WARNING.get().replace("{minutes}", String.valueOf(minutes));
                }

                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(message),
                        false
                );
            }
        }
    }

    private static void sendOpeningMessage(MinecraftServer server) {
        String message = OneriaConfig.MSG_SERVER_OPENED.get()
                .replace("{opening}", OneriaConfig.OPENING_TIME.get())
                .replace("{closing}", OneriaConfig.CLOSING_TIME.get());

        // Send only to connected staff
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (OneriaPermissions.isStaff(player)) {
                player.sendSystemMessage(Component.literal(message));
            }
        }
    }

    private static void closeServer(MinecraftServer server) {
        if (!OneriaConfig.KICK_NON_STAFF.get()) return;

        String kickMessage = OneriaConfig.MSG_SERVER_CLOSED.get()
                .replace("{opening}", OneriaConfig.OPENING_TIME.get())
                .replace("{closing}", OneriaConfig.CLOSING_TIME.get());

        List<ServerPlayer> playersToKick = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!OneriaPermissions.isStaff(player)) {
                playersToKick.add(player);
            } else {
                player.sendSystemMessage(Component.literal("§6[STAFF] Server closed - You may remain connected."));
            }
        }

        // Kick non-staff players
        for (ServerPlayer player : playersToKick) {
            player.connection.disconnect(Component.literal(kickMessage));
            OneriaMod.LOGGER.info("Kicked {} (server closed, non-staff)", player.getName().getString());
        }

        sentWarnings.clear();
    }

    /**
     * Checks if a player can join
     * @return null if OK, otherwise the kick message
     */
    public static Component canPlayerJoin(ServerPlayer player) {
        if (!OneriaConfig.ENABLE_SCHEDULE.get()) return null;
        if (OneriaPermissions.isStaff(player)) return null;
        if (isServerOpen()) return null;

        String message = OneriaConfig.MSG_SERVER_CLOSED.get()
                .replace("{opening}", OneriaConfig.OPENING_TIME.get())
                .replace("{closing}", OneriaConfig.CLOSING_TIME.get());

        return Component.literal(message);
    }

    /**
     * Gets time remaining until next open/close
     */
    public static String getTimeUntilNextEvent() {
        LocalTime now = LocalTime.now();

        if (isServerOpen()) {
            long minutesUntilClose = java.time.Duration.between(now, closingTime).toMinutes();
            if (minutesUntilClose < 0) minutesUntilClose += 24 * 60;

            long hours = minutesUntilClose / 60;
            long minutes = minutesUntilClose % 60;
            return String.format("Closing in %dh%02d", hours, minutes);
        } else {
            long minutesUntilOpen = java.time.Duration.between(now, openingTime).toMinutes();
            if (minutesUntilOpen < 0) minutesUntilOpen += 24 * 60;

            long hours = minutesUntilOpen / 60;
            long minutes = minutesUntilOpen % 60;
            return String.format("Opening in %dh%02d", hours, minutes);
        }
    }
}