package net.rp.rpessentials.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rp.rpessentials.RpEssentials;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet SERVEUR → CLIENT
 *
 * Envoie la liste des joueurs connectés avec toutes leurs données RP :
 * licences, warns actifs, statut mute, playtime, notes staff, etc.
 *
 * Version 4.1.6 : PlayerData enrichi (rétrocompatible via StreamCodec versionné).
 */
public record OpenPlayerProfileGuiPacket(
        List<PlayerData> players,
        List<String> availableProfessionIds,
        List<String> availableRoles
) implements CustomPacketPayload {

    // =========================================================================
    // DATA
    // =========================================================================

    /**
     * Snapshot des données RP d'un joueur.
     *
     * Champs nouveaux en 4.1.6 :
     *   activeWarnCount, isMuted, muteExpiry, playtimeMs,
     *   sessionMs, noteCount, isOnline
     */
    public record PlayerData(
            UUID   uuid,
            String mcName,
            String currentNick,           // "" si pas de nickname
            String currentRole,           // "" si aucun rôle détecté
            List<String> currentLicenses, // liste des licences actives
            // ── Nouveau en 4.1.6 ─────────────────────────────────────────────
            int    activeWarnCount,       // nombre de warns actifs (non expirés)
            boolean isMuted,              // joueur muté ?
            String muteExpiry,            // "" si non muté, sinon durée restante formatée
            long   playtimeMs,            // playtime cumulatif total (persisté + session)
            long   sessionMs,             // durée de la session courante uniquement
            int    noteCount,             // nombre de notes staff
            boolean isOnline              // connecté au moment du snapshot
    ) {
        // Constructeur de compatibilité pour les cas sans nouveaux champs
        public static PlayerData simple(UUID uuid, String mcName, String nick, String role, List<String> licenses) {
            return new PlayerData(uuid, mcName, nick, role, licenses, 0, false, "", 0L, 0L, 0, true);
        }
    }

    // =========================================================================
    // PACKET INFRA
    // =========================================================================

    public static final Type<OpenPlayerProfileGuiPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "open_player_profile_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenPlayerProfileGuiPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenPlayerProfileGuiPacket decode(FriendlyByteBuf buf) {
                    int playerCount = buf.readVarInt();
                    List<PlayerData> players = new ArrayList<>(playerCount);
                    for (int i = 0; i < playerCount; i++) {
                        UUID   uuid         = buf.readUUID();
                        String mcName       = buf.readUtf();
                        String nick         = buf.readUtf();
                        String role         = buf.readUtf();
                        int    licCount     = buf.readVarInt();
                        List<String> lics   = new ArrayList<>(licCount);
                        for (int j = 0; j < licCount; j++) lics.add(buf.readUtf());
                        // Champs 4.1.6
                        int     warns       = buf.readVarInt();
                        boolean muted       = buf.readBoolean();
                        String  muteExpiry  = buf.readUtf();
                        long    playtime    = buf.readLong();
                        long    session     = buf.readLong();
                        int     notes       = buf.readVarInt();
                        boolean online      = buf.readBoolean();
                        players.add(new PlayerData(uuid, mcName, nick, role, lics,
                                warns, muted, muteExpiry, playtime, session, notes, online));
                    }
                    int profCount = buf.readVarInt();
                    List<String> profIds = new ArrayList<>(profCount);
                    for (int i = 0; i < profCount; i++) profIds.add(buf.readUtf());
                    int roleCount = buf.readVarInt();
                    List<String> roles = new ArrayList<>(roleCount);
                    for (int i = 0; i < roleCount; i++) roles.add(buf.readUtf());
                    return new OpenPlayerProfileGuiPacket(players, profIds, roles);
                }

                @Override
                public void encode(FriendlyByteBuf buf, OpenPlayerProfileGuiPacket packet) {
                    buf.writeVarInt(packet.players().size());
                    for (PlayerData p : packet.players()) {
                        buf.writeUUID(p.uuid());
                        buf.writeUtf(p.mcName());
                        buf.writeUtf(p.currentNick());
                        buf.writeUtf(p.currentRole());
                        buf.writeVarInt(p.currentLicenses().size());
                        for (String lic : p.currentLicenses()) buf.writeUtf(lic);
                        // Champs 4.1.6
                        buf.writeVarInt(p.activeWarnCount());
                        buf.writeBoolean(p.isMuted());
                        buf.writeUtf(p.muteExpiry());
                        buf.writeLong(p.playtimeMs());
                        buf.writeLong(p.sessionMs());
                        buf.writeVarInt(p.noteCount());
                        buf.writeBoolean(p.isOnline());
                    }
                    buf.writeVarInt(packet.availableProfessionIds().size());
                    for (String id : packet.availableProfessionIds()) buf.writeUtf(id);
                    buf.writeVarInt(packet.availableRoles().size());
                    for (String r : packet.availableRoles()) buf.writeUtf(r);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // =========================================================================
    // HANDLER — côté CLIENT
    // =========================================================================

    public static void handleOnClient(OpenPlayerProfileGuiPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            ClientGuiOpener.openPlayerProfileGui(
                    packet.players(),
                    packet.availableProfessionIds(),
                    packet.availableRoles()
            );
        });
    }
}