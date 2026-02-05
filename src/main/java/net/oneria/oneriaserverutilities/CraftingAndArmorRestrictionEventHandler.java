package net.oneria.oneriaserverutilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ✅ VERSION NETTOYÉE - Gère UNIQUEMENT le craft
 * L'armure est gérée par ProfessionRestrictionEventHandler (event-based)
 */
@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class CraftingAndArmorRestrictionEventHandler {

    // Cache pour éviter le spam de messages (1 message toutes les 2 secondes MAX)
    private static final Map<UUID, Long> lastCraftMessage = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000; // 2 secondes

    /**
     * Vérifie périodiquement le craft
     * UNIQUEMENT si le joueur n'est PAS en mode créatif
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // ✅ Ignorer les joueurs en mode créatif pour éviter la duplication
        if (serverPlayer.isCreative()) {
            return;
        }

        // Vérifier toutes les 2 ticks
        if (serverPlayer.tickCount % 2 != 0) {
            return;
        }

        AbstractContainerMenu container = serverPlayer.containerMenu;

        // Bloquer le craft non autorisé
        if (container instanceof CraftingMenu craftingMenu) {
            checkAndClearCraftResult(serverPlayer, craftingMenu);
        }

        // ❌ SUPPRIMÉ: La vérification des armures (gérée par ProfessionRestrictionEventHandler)
    }

    /**
     * Vérifie le résultat de craft et le vide si non autorisé
     */
    private static void checkAndClearCraftResult(ServerPlayer player, CraftingMenu menu) {
        try {
            ItemStack result = menu.getSlot(0).getItem();

            if (!result.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());

                if (!ProfessionRestrictionManager.canCraft(player, itemId)) {
                    // Vider le slot résultat
                    menu.getSlot(0).set(ItemStack.EMPTY);

                    // ✅ Message avec cooldown pour éviter le spam
                    if (canSendMessage(player.getUUID(), lastCraftMessage)) {
                        String message = ProfessionRestrictionManager.getCraftBlockedMessage(itemId);
                        player.displayClientMessage(
                                Component.literal(message),
                                true // actionBar
                        );
                    }

                    // Forcer la resynchronisation
                    player.containerMenu.broadcastFullState();
                }
            }
        } catch (Exception e) {
            // ✅ Log au lieu de silent fail
            OneriaServerUtilities.LOGGER.error("[CraftRestriction] Error checking craft for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Nettoie les caches quand le joueur ferme un menu
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Nettoyer le cooldown de messages pour ce joueur
            UUID uuid = player.getUUID();
            lastCraftMessage.remove(uuid);
        }
    }

    /**
     * Vérifie si on peut envoyer un message (anti-spam)
     */
    private static boolean canSendMessage(UUID playerUUID, Map<UUID, Long> cache) {
        long now = System.currentTimeMillis();
        Long lastTime = cache.get(playerUUID);

        if (lastTime == null || now - lastTime > MESSAGE_COOLDOWN) {
            cache.put(playerUUID, now);
            return true;
        }

        return false;
    }

    /**
     * Nettoie périodiquement les caches (appelé par OneriaServerUtilities)
     */
    public static void cleanupCaches() {
        long now = System.currentTimeMillis();

        // Retirer les entrées de plus de 10 secondes
        lastCraftMessage.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
    }
}