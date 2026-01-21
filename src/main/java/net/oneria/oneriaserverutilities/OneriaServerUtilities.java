package net.oneria.oneriaserverutilities;

import com.mojang.logging.LogUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.EnumSet;

@Mod("oneriaserverutilities")
public class OneriaServerUtilities {
    public static final String MODID = "oneriaserverutilities";
    public static final Logger LOGGER = LogUtils.getLogger();
    private int tickCounter = 0;

    public OneriaServerUtilities(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, OneriaConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);

        // Initialize Schedule System - avec un délai
        modEventBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getType() == ModConfig.Type.SERVER) {
                OneriaScheduleManager.reload();
                LOGGER.info("Schedule system initialized after config load");
            }
        });
    }

    // Securely retrieve LuckPerms prefix
    public static String getPlayerPrefix(ServerPlayer player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : "";
            }
        } catch (IllegalStateException e) {
            // LuckPerms not loaded - this is normal
            return "";
        } catch (Exception e) {
            LOGGER.debug("LuckPerms not available: {}", e.getMessage());
        }
        return "";
    }

    // Securely retrieve LuckPerms suffix
    public static String getPlayerSuffix(ServerPlayer player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String suffix = user.getCachedData().getMetaData().getSuffix();
                return suffix != null ? suffix : "";
            }
        } catch (IllegalStateException e) {
            // LuckPerms not loaded - this is normal
            return "";
        } catch (Exception e) {
            LOGGER.debug("LuckPerms is not available: {}", e.getMessage());
        }
        return "";
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Do not update every tick to save bandwidth (here every 10 ticks = 0.5s)
        if (tickCounter++ % 10 == 0) {
            var server = event.getServer();
            if (server == null) return;

            // Blur system - seulement si activé
            if (OneriaConfig.ENABLE_BLUR.get()) {
                // Force sending an UPDATE_DISPLAY_NAME packet for all players
                // Our Mixin will intercept this packet and modify the content on the fly
                ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        server.getPlayerList().getPlayers()
                );
                server.getPlayerList().broadcastAll(packet);
            }
        }

        // Schedule System Tick (indépendant du blur)
        OneriaScheduleManager.tick(event.getServer());

        // World Border Warning System Tick (indépendant du blur)
        WorldBorderManager.tick(event.getServer());
    }
}