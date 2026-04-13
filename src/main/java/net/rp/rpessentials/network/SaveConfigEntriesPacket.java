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
import net.rp.rpessentials.config.MessagesConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packet CLIENT → SERVEUR : application des modifications de config depuis le GUI.
 *
 * Nouveau en 4.1.6 : broadcast aux staffs online un résumé des changements.
 */
public record SaveConfigEntriesPacket(String fileId, Map<String, String> changes)
        implements CustomPacketPayload {

    public static final Type<SaveConfigEntriesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "save_config_entries"));

    public static final StreamCodec<FriendlyByteBuf, SaveConfigEntriesPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SaveConfigEntriesPacket decode(FriendlyByteBuf buf) {
                    String fileId = buf.readUtf();
                    int    count  = buf.readVarInt();
                    Map<String, String> changes = new HashMap<>(count);
                    for (int i = 0; i < count; i++) changes.put(buf.readUtf(), buf.readUtf());
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
    // HANDLER — côté SERVEUR
    // =========================================================================

    public static void handleOnServer(SaveConfigEntriesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!RpEssentialsPermissions.isStaff(player)) {
                RpEssentials.LOGGER.warn("[ConfigGUI] Non-staff {} tried to save config '{}'",
                        player.getName().getString(), packet.fileId());
                return;
            }

            if (packet.changes().isEmpty()) {
                player.sendSystemMessage(Component.literal("§7[Config] No changes to apply."));
                return;
            }

            // ── Snapshot des anciennes valeurs avant application ──────────────
            Map<String, String> oldValues = snapshotCurrentValues(packet.fileId(), packet.changes().keySet());

            // ── Application ──────────────────────────────────────────────────
            int applied = ConfigInspector.applyAndSave(packet.fileId(), packet.changes());

            if (applied > 0) {
                RpEssentials.LOGGER.info("[ConfigGUI] {} applied {} change(s) to '{}'",
                        player.getName().getString(), applied, packet.fileId());

                // ── Broadcast au staff ────────────────────────────────────────
                broadcastChangesToStaff(player, packet.fileId(), packet.changes(), oldValues);

                // ── Retour des entrées mises à jour ───────────────────────────
                List<ConfigInspector.EntryData> entries = ConfigInspector.getEntries(packet.fileId());
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

    // =========================================================================
    // PRIVATE
    // =========================================================================

    /**
     * Récupère les valeurs actuelles des clés modifiées (avant application).
     */
    private static Map<String, String> snapshotCurrentValues(String fileId, java.util.Set<String> paths) {
        Map<String, String> snapshot = new HashMap<>();
        try {
            List<ConfigInspector.EntryData> entries = ConfigInspector.getEntries(fileId);
            for (ConfigInspector.EntryData e : entries) {
                if (paths.contains(e.fullPath())) {
                    snapshot.put(e.fullPath(), e.currentValue());
                }
            }
        } catch (Exception ignored) {}
        return snapshot;
    }

    /**
     * Envoie à tous les staffs online un résumé lisible des changements.
     *
     * Format : "[CONFIG] Staff a modifié n valeur(s) dans fichier :"
     *          "  clé : ancienne → nouvelle"
     */
    private static void broadcastChangesToStaff(ServerPlayer editor,
                                                String fileId,
                                                Map<String, String> newValues,
                                                Map<String, String> oldValues) {
        String staffName = editor.getName().getString();
        int count = newValues.size();

        StringBuilder sb = new StringBuilder();
        sb.append("§6[CONFIG] §e").append(staffName)
                .append(" §7a modifié §e").append(count).append(" §7valeur(s) dans §f").append(fileId).append("§7:");

        int shown = 0;
        for (Map.Entry<String, String> change : newValues.entrySet()) {
            if (shown >= 5) { // Ne pas spammer si beaucoup de changements
                sb.append("\n  §8... et ").append(count - shown).append(" autre(s)");
                break;
            }
            String key = simplifyKey(change.getKey());
            String oldVal = oldValues.getOrDefault(change.getKey(), "?");
            String newVal = change.getValue();
            // Tronque si trop long
            if (oldVal.length() > 40) oldVal = oldVal.substring(0, 37) + "...";
            if (newVal.length() > 40) newVal = newVal.substring(0, 37) + "...";
            sb.append("\n  §7").append(key).append(" : §c").append(oldVal).append(" §7→ §a").append(newVal);
            shown++;
        }

        Component msg = Component.literal(sb.toString());

        for (ServerPlayer staff : editor.getServer().getPlayerList().getPlayers()) {
            if (RpEssentialsPermissions.isStaff(staff) && !staff.getUUID().equals(editor.getUUID())) {
                staff.sendSystemMessage(msg);
            }
        }
        RpEssentials.LOGGER.info("[CONFIG] {} modified {} value(s) in {}", staffName, count, fileId);
    }

    /** Raccourcit le chemin complet "section.subsection.key" en juste "key" pour la lisibilité. */
    private static String simplifyKey(String fullPath) {
        int dot = fullPath.lastIndexOf('.');
        return dot >= 0 ? fullPath.substring(dot + 1) : fullPath;
    }
}