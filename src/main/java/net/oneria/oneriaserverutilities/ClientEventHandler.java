package net.oneria.oneriaserverutilities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.oneria.oneriaserverutilities.client.ClientNametagConfig;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNametagConfig.reset();
        OneriaServerUtilities.LOGGER.info("Nametag config reset on disconnect");
    }
}