package net.rp.rpessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.RpEssentialsPermissions;
import net.rp.rpessentials.moderation.NoteManager;

import java.util.UUID;

/**
 * Packet CLIENT -> SERVEUR
 * Ajouter ou supprimer une note sur un joueur depuis le GUI.
 */
public record PlayerNoteActionPacket(
        UUID   targetUuid,
        boolean isDelete,   // true = supprimer, false = ajouter
        int    noteId,      // utilisé si isDelete = true
        String text         // utilisé si isDelete = false
) implements CustomPacketPayload {

    public static final Type<PlayerNoteActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "player_note_action"));

    public static final StreamCodec<FriendlyByteBuf, PlayerNoteActionPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PlayerNoteActionPacket decode(FriendlyByteBuf buf) {
                    return new PlayerNoteActionPacket(
                            buf.readUUID(),
                            buf.readBoolean(),
                            buf.readVarInt(),
                            buf.readUtf()
                    );
                }
                @Override
                public void encode(FriendlyByteBuf buf, PlayerNoteActionPacket p) {
                    buf.writeUUID(p.targetUuid());
                    buf.writeBoolean(p.isDelete());
                    buf.writeVarInt(p.noteId());
                    buf.writeUtf(p.text());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleOnServer(PlayerNoteActionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer staff)) return;
            if (!RpEssentialsPermissions.isStaff(staff)) return;

            if (packet.isDelete()) {
                NoteManager.removeNote(packet.targetUuid(), packet.noteId());
                RpEssentials.LOGGER.info("[Notes] {} deleted note #{} for {}",
                        staff.getName().getString(), packet.noteId(), packet.targetUuid());
            } else {
                String text = packet.text().trim();
                if (text.isEmpty()) return;
                ServerPlayer targetOnline = staff.getServer().getPlayerList().getPlayer(packet.targetUuid());
                String targetName = targetOnline != null
                        ? targetOnline.getName().getString()
                        : staff.getServer().getProfileCache()
                        .get(packet.targetUuid())
                        .map(p -> p.getName())
                        .orElse(packet.targetUuid().toString());
                int newId = NoteManager.addNote(
                        packet.targetUuid(),
                        targetName,
                        staff.getName().getString(),
                        staff.getUUID().toString(),
                        text
                );
                if (newId < 0) {
                    staff.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c[NOTE] Note limit reached for this player."));
                }
                RpEssentials.LOGGER.info("[Notes] {} added note for {}: {}",
                        staff.getName().getString(), packet.targetUuid(), text);
            }
        });
    }
}