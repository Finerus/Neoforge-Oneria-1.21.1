package net.rp.rpessentials;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.rp.rpessentials.client.ClientNametagConfig;

@EventBusSubscriber(modid = RpEssentials.MODID)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNametagConfig.reset();
        RpEssentials.LOGGER.info("Nametag config reset on disconnect");
    }
}