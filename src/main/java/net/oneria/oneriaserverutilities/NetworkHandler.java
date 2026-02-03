package net.oneria.oneriaserverutilities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.oneria.oneriaserverutilities.client.ClientNametagConfig;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                HideNametagsPacket.TYPE,
                HideNametagsPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        NetworkHandler::handleHideNametags,
                        null
                )
        );
    }

    private static void handleHideNametags(HideNametagsPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientNametagConfig.setHideNametags(packet.hideNametags());
            OneriaServerUtilities.LOGGER.info("Received nametag config from server - Hide: {}", packet.hideNametags());
        });
    }
}