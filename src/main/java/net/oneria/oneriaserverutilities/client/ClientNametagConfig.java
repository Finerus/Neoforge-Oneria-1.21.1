package net.oneria.oneriaserverutilities.client;

public class ClientNametagConfig {
    private static boolean hideNametags = false;
    private static boolean hasReceivedConfig = false;

    public static void setHideNametags(boolean hide) {
        hideNametags = hide;
        hasReceivedConfig = true;
    }

    public static boolean shouldHideNametags() {
        return hideNametags;
    }

    public static boolean hasReceivedServerConfig() {
        return hasReceivedConfig;
    }

    public static void reset() {
        hideNametags = false;
        hasReceivedConfig = false;
    }
}