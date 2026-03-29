package net.rp.rpessentials.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.network.RequestOpenGuiPacket;

/**
 * Écoute les ticks client pour détecter les appuis de touches GUI.
 */
@EventBusSubscriber(modid = RpEssentials.MODID, value = Dist.CLIENT)
public class RpClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) return;

        // ── Touche GUI Profession ─────────────────────────────────────────────
        while (RpKeyBindings.OPEN_PROFESSION_GUI != null
                && RpKeyBindings.OPEN_PROFESSION_GUI.consumeClick()) {
            PacketDistributor.sendToServer(
                    new RequestOpenGuiPacket(RequestOpenGuiPacket.GuiType.PROFESSION));
            RpEssentials.LOGGER.debug("[RPEssentials] Sent PROFESSION GUI request to server");
        }

        // ── Touche GUI Profil Joueur ──────────────────────────────────────────
        while (RpKeyBindings.OPEN_PLAYER_PROFILE_GUI != null
                && RpKeyBindings.OPEN_PLAYER_PROFILE_GUI.consumeClick()) {
            PacketDistributor.sendToServer(
                    new RequestOpenGuiPacket(RequestOpenGuiPacket.GuiType.PLAYER_PROFILE));
            RpEssentials.LOGGER.debug("[RPEssentials] Sent PLAYER_PROFILE GUI request to server");
        }

        while (RpKeyBindings.OPEN_DICE_GUI != null
                && RpKeyBindings.OPEN_DICE_GUI.consumeClick()) {
            try {
                if (net.rp.rpessentials.config.RpConfig.ENABLE_DICE_SYSTEM.get()) {
                    mc.setScreen(new net.rp.rpessentials.client.gui.DiceSelectionScreen());
                }
            } catch (IllegalStateException ignored) {}
        }

        // ── Touche GUI Config Manager ─────────────────────────────────────────
        while (RpKeyBindings.OPEN_CONFIG_MANAGER_GUI != null
                && RpKeyBindings.OPEN_CONFIG_MANAGER_GUI.consumeClick()) {
            PacketDistributor.sendToServer(
                    new RequestOpenGuiPacket(RequestOpenGuiPacket.GuiType.CONFIG_MANAGER));
            RpEssentials.LOGGER.debug("[RPEssentials] Sent CONFIG_MANAGER GUI request to server");
        }
    }
}
