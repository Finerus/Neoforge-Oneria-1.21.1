package net.oneria.oneriaserverutilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
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
 * VERSION OPTIMISÉE - Corrige :
 * 1. Messages dupliqués (serveur ET client envoyaient des messages)
 * 2. Duplication d'items en mode créatif
 * 3. Utilise canEquip() au lieu de canCraft() pour les armures
 */
@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class CraftingAndArmorRestrictionEventHandler {

    // Cache pour éviter le spam de messages (1 message toutes les 2 secondes MAX)
    private static final Map<UUID, Long> lastCraftMessage = new HashMap<>();
    private static final Map<UUID, Long> lastArmorMessage = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000; // 2 secondes

    /**
     * Vérifie périodiquement le craft et les armures
     * UNIQUEMENT si le joueur n'est PAS en mode créatif
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // ✅ FIX: Ignorer les joueurs en mode créatif pour éviter la duplication
        if (serverPlayer.isCreative()) {
            return;
        }

        // Vérifier toutes les 5 ticks (0.25 secondes)
        if (serverPlayer.tickCount % 5 != 0) {
            return;
        }

        AbstractContainerMenu container = serverPlayer.containerMenu;

        // Bloquer le craft non autorisé
        if (container instanceof CraftingMenu craftingMenu) {
            checkAndClearCraftResult(serverPlayer, craftingMenu);
        }

        // Bloquer les armures non autorisées
        if (container instanceof InventoryMenu inventoryMenu) {
            checkAndClearArmorSlots(serverPlayer, inventoryMenu);
        }
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

                    // ✅ FIX: Message avec cooldown pour éviter le spam
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
            // Ignorer les erreurs silencieusement
        }
    }

    /**
     * Vérifie les slots d'armure et retire les pièces non autorisées
     */
    private static void checkAndClearArmorSlots(ServerPlayer player, InventoryMenu menu) {
        try {
            boolean foundUnauthorized = false;

            // Vérifier les 4 slots d'armure (5 = tête, 6 = torse, 7 = jambes, 8 = pieds)
            for (int slotIndex = 5; slotIndex <= 8; slotIndex++) {
                ItemStack armorPiece = menu.getSlot(slotIndex).getItem();

                if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ArmorItem) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armorPiece.getItem());

                    // ✅ FIX: Utiliser canEquip() au lieu de canCraft()
                    if (!ProfessionRestrictionManager.canEquip(player, itemId)) {
                        // Retirer l'armure
                        menu.getSlot(slotIndex).set(ItemStack.EMPTY);

                        // Remettre dans l'inventaire ou drop
                        if (!player.getInventory().add(armorPiece)) {
                            player.drop(armorPiece, false);
                        }

                        foundUnauthorized = true;
                    }
                }
            }

            if (foundUnauthorized) {
                // ✅ FIX: Message avec cooldown
                if (canSendMessage(player.getUUID(), lastArmorMessage)) {
                    player.displayClientMessage(
                            Component.literal("§c§lVous ne pouvez pas équiper cette armure sans le métier requis."),
                            true
                    );
                }

                player.containerMenu.broadcastFullState();
            }
        } catch (Exception e) {
            // Ignorer les erreurs silencieusement
        }
    }

    /**
     * Nettoie les caches quand le joueur ferme un menu
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Nettoyer les cooldowns de messages pour ce joueur
            UUID uuid = player.getUUID();
            lastCraftMessage.remove(uuid);
            lastArmorMessage.remove(uuid);
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
        lastArmorMessage.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
    }
}