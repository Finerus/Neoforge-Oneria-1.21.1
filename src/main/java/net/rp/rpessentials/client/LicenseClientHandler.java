package net.rp.rpessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.client.gui.LicenseScreen;
import net.rp.rpessentials.items.LicenseItem;

@EventBusSubscriber(modid = RpEssentials.MODID, value = Dist.CLIENT)
public class LicenseClientHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getItemStack().getItem() instanceof LicenseItem)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        mc.setScreen(new LicenseScreen(event.getItemStack()));
    }
}