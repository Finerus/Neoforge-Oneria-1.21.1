package net.rp.rpessentials;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpessentials.identity.NicknameManager;
import net.rp.rpessentials.profession.LicenseManager;

import java.util.UUID;

/**
 * Packet serveur → client.
 * Envoie les données nametag d'UN joueur cible au client récepteur.
 */
public record SyncNametagDataPacket(
        UUID targetUUID,
        String displayName,
        String prefix,
        String suffix,
        String profession,
        boolean isStaff
) implements CustomPacketPayload {

    public static final Type<SyncNametagDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "sync_nametag_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncNametagDataPacket> CODEC =
            StreamCodec.composite(
                    new StreamCodec<>() {
                        @Override public UUID decode(FriendlyByteBuf buf) { return buf.readUUID(); }
                        @Override public void encode(FriendlyByteBuf buf, UUID v) { buf.writeUUID(v); }
                    }, SyncNametagDataPacket::targetUUID,
                    new StreamCodec<>() {
                        @Override public String decode(FriendlyByteBuf buf) { return buf.readUtf(); }
                        @Override public void encode(FriendlyByteBuf buf, String v) { buf.writeUtf(v); }
                    }, SyncNametagDataPacket::displayName,
                    new StreamCodec<>() {
                        @Override public String decode(FriendlyByteBuf buf) { return buf.readUtf(); }
                        @Override public void encode(FriendlyByteBuf buf, String v) { buf.writeUtf(v); }
                    }, SyncNametagDataPacket::prefix,
                    new StreamCodec<>() {
                        @Override public String decode(FriendlyByteBuf buf) { return buf.readUtf(); }
                        @Override public void encode(FriendlyByteBuf buf, String v) { buf.writeUtf(v); }
                    }, SyncNametagDataPacket::suffix,
                    new StreamCodec<>() {
                        @Override public String decode(FriendlyByteBuf buf) { return buf.readUtf(); }
                        @Override public void encode(FriendlyByteBuf buf, String v) { buf.writeUtf(v); }
                    }, SyncNametagDataPacket::profession,
                    new StreamCodec<>() {
                        @Override public Boolean decode(FriendlyByteBuf buf) { return buf.readBoolean(); }
                        @Override public void encode(FriendlyByteBuf buf, Boolean v) { buf.writeBoolean(v); }
                    }, SyncNametagDataPacket::isStaff,
                    SyncNametagDataPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // =========================================================================
    // HANDLER (côté CLIENT)
    // =========================================================================

    public static void handle(SyncNametagDataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            RpEssentials.LOGGER.info("[SyncNametag] CLIENT received: uuid={} displayName='{}'",
                    packet.targetUUID(), packet.displayName());
            net.rp.rpessentials.client.ClientNametagCache.update(packet);
        });
    }

    // =========================================================================
    // FACTORY — construit le packet depuis un ServerPlayer
    // =========================================================================

    public static SyncNametagDataPacket from(ServerPlayer target) {
        UUID uuid = target.getUUID();

        String displayName = NicknameManager.hasNickname(uuid)
                ? NicknameManager.getNickname(uuid)
                : target.getGameProfile().getName();

        String prefix  = RpEssentials.getPlayerPrefix(target);
        String suffix  = RpEssentials.getPlayerSuffix(target);

        java.util.List<String> licenses = LicenseManager.getLicenses(uuid);
        String profession = licenses.isEmpty() ? "" : licenses.get(0);

        boolean isStaff = RpEssentialsPermissions.isStaff(target);

        return new SyncNametagDataPacket(uuid, displayName, prefix, suffix, profession, isStaff);
    }

    // =========================================================================
    // BROADCAST — envoie ce packet à tous les joueurs connectés
    // =========================================================================

    public static void broadcastForPlayer(ServerPlayer target) {
        SyncNametagDataPacket packet = from(target);
        RpEssentials.LOGGER.info("[SyncNametag] SERVER broadcasting for '{}' — displayName='{}'",
                target.getName().getString(), packet.displayName());
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(packet);
    }
}