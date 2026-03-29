package net.rp.rpessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.RpEssentialsPermissions;
import net.rp.rpessentials.config.ConfigInspector;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet CLIENT → SERVER
 *
 * Sent when the player clicks "Apply" in the Config Manager GUI.
 * Contains the file id and a map of { fullPath → serialized new value }
 * for all entries that were modified.
 *
 * The server validates permissions, applies changes, saves the spec,
 * then sends a confirmation message.
 */
public record SaveConfigEntriesPacket(String fileId, Map<String, String> changes)
        implements CustomPacketPayload {

    public static final Type<SaveConfigEntriesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "save_config_entries"));

    public static final StreamCodec<FriendlyByteBuf, SaveConfigEntriesPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SaveConfigEntriesPacket decode(FriendlyByteBuf buf) {
                    String fileId  = buf.readUtf();
                    int    count   = buf.readVarInt();
                    Map<String, String> changes = new HashMap<>(count);
                    for (int i = 0; i < count; i++) {
                        changes.put(buf.readUtf(), buf.readUtf());
                    }
                    return new SaveConfigEntriesPacket(fileId, changes);
                }

                @Override
                public void encode(FriendlyByteBuf buf, SaveConfigEntriesPacket packet) {
                    buf.writeUtf(packet.fileId());
                    buf.writeVarInt(packet.changes().size());
                    for (Map.Entry<String, String> e : packet.changes().entrySet()) {
                        buf.writeUtf(e.getKey());
                        buf.writeUtf(e.getValue());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // =========================================================================
    // HANDLER — server side
    // =========================================================================

    public static void handleOnServer(SaveConfigEntriesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!RpEssentialsPermissions.isStaff(player)) {
                RpEssentials.LOGGER.warn("[ConfigGUI] Non-staff player {} tried to save config '{}'",
                        player.getName().getString(), packet.fileId());
                return;
            }

            if (packet.changes().isEmpty()) {
                player.sendSystemMessage(Component.literal("§7[Config] No changes to apply."));
                return;
            }

            int applied = ConfigInspector.applyAndSave(packet.fileId(), packet.changes());

            if (applied > 0) {
                RpEssentials.LOGGER.info("[ConfigGUI] {} applied {} change(s) to '{}'",
                        player.getName().getString(), applied, packet.fileId());

                // Send updated entries back so the GUI reflects the saved values
                java.util.List<ConfigInspector.EntryData> entries =
                        ConfigInspector.getEntries(packet.fileId());
                PacketDistributor.sendToPlayer(player,
                        ConfigFileEntriesPacket.from(packet.fileId(), entries));

                player.sendSystemMessage(Component.literal(
                        "§a[Config] §f" + applied + " change(s) applied to §e" + packet.fileId() + "§f."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§c[Config] No changes could be applied (validation failed?)."));
            }
        });
    }
}
