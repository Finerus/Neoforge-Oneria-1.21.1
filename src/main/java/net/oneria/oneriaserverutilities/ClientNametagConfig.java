package net.oneria.oneriaserverutilities;

public class ClientNametagConfig {
    private static boolean hideNametags = false;
    private static boolean hasReceivedServerConfig = false;

    public static void setHideNametags(boolean hide) {
        hideNametags = hide;
        hasReceivedServerConfig = true;
    }

    public static void reset() {
        hideNametags = false;
        hasReceivedServerConfig = false;
    }

    public static boolean shouldHideNametags() {
        return hasReceivedServerConfig && hideNametags;
    }
}