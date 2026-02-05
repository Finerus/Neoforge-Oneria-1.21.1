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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gestionnaire d'événements pour bloquer :
 * 1. La récupération d'items craftés sans le métier requis
 * 2. L'équipement d'armures sans le métier requis
 *
 * Alternative aux Mixins qui évite les problèmes d'obfuscation NeoForge
 */
@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
public class CraftingAndArmorRestrictionEventHandler {

    /**
     * Vérifie en continu :
     * - Le résultat de craft dans les tables de craft
     * - Les armures équipées dans l'inventaire du joueur
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Vérifier toutes les 5 ticks (0.25 secondes) pour minimiser l'impact sur les performances
        if (serverPlayer.tickCount % 5 != 0) {
            return;
        }

        AbstractContainerMenu container = serverPlayer.containerMenu;

        // 1. Bloquer le craft non autorisé
        if (container instanceof CraftingMenu craftingMenu) {
            checkAndClearCraftResult(serverPlayer, craftingMenu);
        }

        // 2. Bloquer les armures non autorisées
        if (container instanceof InventoryMenu inventoryMenu) {
            checkAndClearArmorSlots(serverPlayer, inventoryMenu);
        }
    }

    /**
     * Vérifie le résultat de craft et le vide si non autorisé
     */
    private static void checkAndClearCraftResult(ServerPlayer player, CraftingMenu menu) {
        try {
            // Accéder au slot résultat (index 0)
            ItemStack result = menu.getSlot(0).getItem();

            if (!result.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());

                if (!ProfessionRestrictionManager.canCraft(player, itemId)) {
                    // Vider le slot résultat
                    menu.getSlot(0).set(ItemStack.EMPTY);

                    // Message d'erreur (pas trop fréquent)
                    if (player.tickCount % 40 == 0) { // Toutes les 2 secondes
                        String message = ProfessionRestrictionManager.getCraftBlockedMessage(itemId);
                        player.displayClientMessage(
                                Component.literal(message),
                                true // actionBar
                        );
                    }

                    // Forcer la resynchronisation pour éviter les ghost items
                    player.containerMenu.broadcastFullState();
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs silencieusement
        }
    }

    /**
     * Vérifie les slots d'armure et retire les pièces non autorisées
     * Slots d'armure dans InventoryMenu :
     * - Slot 5 : Casque (head)
     * - Slot 6 : Plastron (chest)
     * - Slot 7 : Jambières (legs)
     * - Slot 8 : Bottes (feet)
     */
    private static void checkAndClearArmorSlots(ServerPlayer player, InventoryMenu menu) {
        try {
            boolean foundUnauthorized = false;

            // Vérifier les 4 slots d'armure
            for (int slotIndex = 5; slotIndex <= 8; slotIndex++) {
                ItemStack armorPiece = menu.getSlot(slotIndex).getItem();

                if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ArmorItem) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armorPiece.getItem());

                    if (!ProfessionRestrictionManager.canCraft(player, itemId)) {
                        // Retirer la pièce d'armure non autorisée
                        menu.getSlot(slotIndex).set(ItemStack.EMPTY);

                        // Donner l'item au joueur dans son inventaire principal
                        if (!player.getInventory().add(armorPiece)) {
                            // Si l'inventaire est plein, faire tomber l'item
                            player.drop(armorPiece, false);
                        }

                        foundUnauthorized = true;
                    }
                }
            }

            if (foundUnauthorized) {
                // Message d'erreur (pas trop fréquent)
                if (player.tickCount % 40 == 0) { // Toutes les 2 secondes
                    player.displayClientMessage(
                            Component.literal("§c§lVous ne pouvez pas équiper cette armure sans le métier requis."),
                            true // actionBar
                    );
                }

                // Forcer la resynchronisation
                player.containerMenu.broadcastFullState();
            }
        } catch (Exception e) {
            // Ignorer les erreurs silencieusement
        }
    }
}