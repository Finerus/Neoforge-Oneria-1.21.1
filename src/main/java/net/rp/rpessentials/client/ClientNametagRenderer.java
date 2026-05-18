package net.rp.rpessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.rp.rpessentials.ColorHelper;
import net.rp.rpessentials.RpEssentials;

import java.util.UUID;

@EventBusSubscriber(modid = RpEssentials.MODID, value = Dist.CLIENT)
public class ClientNametagRenderer {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof AbstractClientPlayer target)) return;

        if (ClientNametagConfig.shouldHideNametags()) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        UUID uuid = target.getUUID();
        String realName = target.getGameProfile().getName();

        ClientNametagCache.NametagData data = ClientNametagCache.get(uuid);
        if (data == null) return;

        String prefix  = data.prefix() != null ? data.prefix() : "";
        String display = data.displayName() != null && !data.displayName().isEmpty()
                ? data.displayName()
                : realName;

        event.setContent(ColorHelper.parseColors(prefix + display));
    }
}