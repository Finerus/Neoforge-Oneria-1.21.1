package net.oneria.oneriaserverutilities;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID, value = Dist.CLIENT)
public class ClientNametagRenderer {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (ClientNametagConfig.shouldHideNametags() && event.getEntity() instanceof Player) {
            event.setContent(net.minecraft.network.chat.Component.empty());
        }
    }
}