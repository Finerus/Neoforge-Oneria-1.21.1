package net.rp.rpessentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.rp.rpessentials.RpEssentials;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Système nametag avancé côté client.
 *
 * Fonctionnalités :
 *   - Obfuscation par distance  : même logique que le TabList (§k???? au-delà du seuil)
 *   - Occultation par les blocs : raycast caméra → œil du joueur, caché si touche un bloc
 *   - Distance de rendu max     : nametag invisible au-delà de X blocs
 *   - Sneak                     : optionnellement masquer le nametag si accroupi
 *   - Bypass staff              : le staff voit toujours les noms réels
 *   - Format configurable       : "$prefix$name", "$prefix $name", etc.
 *
 * Rétrocompatibilité :
 *   Si {@code advancedEnabled = false}, le comportement legacy {@code hideNametags} est conservé.
 *
 * Aucun Mixin utilisé. Tout passe par {@link RenderNameTagEvent}.
 */
@EventBusSubscriber(modid = RpEssentials.MODID, value = Dist.CLIENT)
public class ClientNametagRenderer {

    // ── Cache d'occlusion par bloc ────────────────────────────────────────────────
    // UUID → [occluded (0/1), expiryMs]
    // Rafraîchi toutes les 150 ms pour éviter un raycast chaque frame (~60/s)
    private static final Map<UUID, long[]> OCCLUSION_CACHE = new HashMap<>();
    private static final long CACHE_TTL_MS = 150L;

    // ═══════════════════════════════════════════════════════════════════════════════
    // EVENT PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {

        // On ne traite que les joueurs
        if (!(event.getEntity() instanceof AbstractClientPlayer target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Jamais son propre nametag
        if (target.getUUID().equals(mc.player.getUUID())) return;

        // ── Mode legacy (système avancé désactivé) ────────────────────────────────
        if (!ClientNametagConfig.isAdvancedEnabled()) {
            if (ClientNametagConfig.hasReceivedConfig() && ClientNametagConfig.shouldHideNametags()) {
                event.setCanRender(TriState.FALSE);
            }
            return;
        }

        // ── Attente de la config serveur ──────────────────────────────────────────
        if (!ClientNametagConfig.hasReceivedConfig()) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        // ── Données du joueur ciblé ───────────────────────────────────────────────
        ClientNametagConfig.PlayerData data = ClientNametagConfig.getPlayerData(target.getUUID());
        if (data == null) {
            // Joueur inconnu (cache non encore reçu), on masque par sécurité
            event.setCanRender(TriState.FALSE);
            return;
        }

        // ── Position caméra ───────────────────────────────────────────────────────
        Vec3 cam    = mc.gameRenderer.getMainCamera().getPosition();
        double distSq = cam.distanceToSqr(target.getEyePosition());

        // ── Bypass staff ──────────────────────────────────────────────────────────
        if (ClientNametagConfig.isViewerStaff() && ClientNametagConfig.isStaffAlwaysSeeReal()) {
            event.setContent(buildComponent(data, false));
            return; // visible, composant remplacé, pas de cancel
        }

        // ── Distance de rendu maximum ─────────────────────────────────────────────
        int renderDist = ClientNametagConfig.getRenderDistance();
        if (renderDist > 0 && distSq > (double) (renderDist * renderDist)) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        // ── Occultation par les blocs (raycast avec cache) ────────────────────────
        if (ClientNametagConfig.isHideBehindBlocks() && isOccluded(mc, target, cam)) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        // ── Sneak ─────────────────────────────────────────────────────────────────
        if (!ClientNametagConfig.isShowWhileSneaking() && target.isCrouching()) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        // ── Obfuscation par distance ──────────────────────────────────────────────
        int obfDist = ClientNametagConfig.getObfuscationDistance();
        boolean obfuscated = ClientNametagConfig.isObfuscationEnabled()
                && distSq > (double) (obfDist * obfDist);

        // ── Rendu avec composant remplacé ─────────────────────────────────────────
        event.setContent(buildComponent(data, obfuscated));
        // Pas de setCanRender(FALSE) → le nametag est affiché normalement
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // RAYCAST AVEC CACHE
    // ═══════════════════════════════════════════════════════════════════════════════

    private static boolean isOccluded(Minecraft mc, AbstractClientPlayer target, Vec3 cam) {
        UUID  uuid = target.getUUID();
        long  now  = System.currentTimeMillis();

        long[] cached = OCCLUSION_CACHE.get(uuid);
        if (cached != null && now < cached[1]) {
            return cached[0] == 1L;
        }

        // Raycast caméra → œil du joueur
        ClipContext ctx = new ClipContext(
                cam,
                target.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target   // entité ignorée pour éviter de toucher le joueur lui-même
        );
        BlockHitResult result = mc.level.clip(ctx);
        boolean occluded = result.getType() == HitResult.Type.BLOCK;

        OCCLUSION_CACHE.put(uuid, new long[]{ occluded ? 1L : 0L, now + CACHE_TTL_MS });
        return occluded;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION DU COMPOSANT
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Construit le {@link Component} à afficher au-dessus de la tête.
     *
     * Variables du format :
     *   $prefix   → préfixe LuckPerms (vide si absent)
     *   $name     → nickname ou nom réel
     *   $realname → toujours le nom MC réel
     *
     * Si {@code obfuscated = true} :
     *   $prefix → vide (ne pas révéler le grade)
     *   $name / $realname → §k????  (longueur du nom, caractères obfusqués)
     */
    private static Component buildComponent(ClientNametagConfig.PlayerData data, boolean obfuscated) {
        String fmt = ClientNametagConfig.getFormat();
        String text;

        if (obfuscated) {
            // Obfusqué : pas de préfixe, nom remplacé par des caractères aléatoires
            int len = stripColors(data.displayName()).length();
            if (len < 1) len = 1;
            String obfName = "§k" + "?".repeat(len);
            text = fmt
                    .replace("$prefix",   "")
                    .replace("$name",     obfName)
                    .replace("$realname", obfName);
        } else {
            // Lisible : on applique les variables
            text = fmt
                    .replace("$prefix",   data.prefix())
                    .replace("$name",     data.displayName())
                    .replace("$realname", data.realName());
        }

        // Parser les codes couleur & et § (déjà présents dans prefix / displayName)
        text = text.replace("&", "§");

        return Component.literal(text);
    }

    /** Retire les codes couleur §X d'une chaîne pour compter les vrais caractères. */
    private static String stripColors(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "");
    }
}
