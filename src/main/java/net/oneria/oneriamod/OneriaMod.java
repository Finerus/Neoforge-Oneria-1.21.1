package net.oneria.oneriamod;

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

import java.util.EnumSet;

@Mod("oneriamod")
public class OneriaMod {
    public static final String MODID = "oneriamod";
    private int tickCounter = 0;

    public OneriaMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, OneriaConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(OneriaCommands.class); // Enregistrement des commandes
    }

    // Récupération sécurisée du préfixe LuckPerms
    public static String getPlayerPrefix(ServerPlayer player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : "";
            }
        } catch (Exception e) {
            // LuckPerms non chargé ou erreur
        }
        return "";
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!OneriaConfig.ENABLE_BLUR.get()) return;

        // On ne met pas à jour à chaque tick pour économiser la bande passante (ici toutes les 10 ticks = 0.5s)
        if (tickCounter++ % 10 == 0) {
            var server = event.getServer();
            if (server == null) return;

            // On force l'envoi d'un paquet UPDATE_DISPLAY_NAME pour tous les joueurs
            // Notre Mixin va intercepter ce paquet et modifier le contenu à la volée
            ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                    server.getPlayerList().getPlayers()
            );
            server.getPlayerList().broadcastAll(packet);
        }
    }
}