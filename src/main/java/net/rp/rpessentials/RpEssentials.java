package net.rp.rpessentials;

import com.mojang.logging.LogUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

// ⚠ CHANGED: "RpEssentials" → "rpessentials"
@Mod("rpessentials")
public class RpEssentials {

    // ⚠ CHANGED: valeur du MODID
    public static final String MODID = "rpessentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    private int tickCounter = 0;

    public RpEssentials(IEventBus modEventBus, ModContainer modContainer) {
        // Migrer tous les anciens fichiers de config (phases 1→2→3) AVANT registerConfig()
        ConfigMigrator.migrateIfNeeded();

        // ⚠ CHANGED: tous les chemins "oneria/oneria-*.toml" → "rpessentials/rpessentials-*.toml"
        modContainer.registerConfig(ModConfig.Type.SERVER, RpEssentialsConfig.SPEC,       "rpessentials/rpessentials-core.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ChatConfig.SPEC,         "rpessentials/rpessentials-chat.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ScheduleConfig.SPEC,     "rpessentials/rpessentials-schedule.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ModerationConfig.SPEC,   "rpessentials/rpessentials-moderation.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ProfessionConfig.SPEC,   "rpessentials/rpessentials-professions.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, MessagesConfig.SPEC,     "rpessentials/rpessentials-messages.toml");

        RpEssentialsItems.ITEMS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getType() == ModConfig.Type.SERVER) {
                RpEssentialsScheduleManager.reload();
                ProfessionRestrictionManager.reloadCache();
                LOGGER.info("[RPEssentials] Schedule system and profession restrictions initialized after config load");
            }
        });
    }

    public static String getPlayerPrefix(ServerPlayer player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : "";
            }
        } catch (IllegalStateException | NoClassDefFoundError e) {
            // LuckPerms non disponible
        }
        return "";
    }

    public static String getPlayerSuffix(ServerPlayer player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String suffix = user.getCachedData().getMetaData().getSuffix();
                return suffix != null ? suffix : "";
            }
        } catch (IllegalStateException | NoClassDefFoundError e) {
            // LuckPerms non disponible
        }
        return "";
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        // Tick planning (toutes les 20 ticks = 1 seconde)
        if (tickCounter % 20 == 0) {
            RpEssentialsScheduleManager.tick(event.getServer());
        }

        // Sweep midnight (toutes les 1200 ticks = 60 secondes)
        if (tickCounter % 1200 == 0) {
            RpEssentialsScheduleManager.tickMidnightSweep(event.getServer());
            tickCounter = 0;
        }
    }
}