package net.oneria.oneriaserverutilities.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * HANDLER CLIENT - Vide le slot SANS messages
 * Les messages sont gérés UNIQUEMENT par le serveur pour éviter la duplication
 */
@EventBusSubscriber(modid = "oneriaserverutilities", value = Dist.CLIENT)
public class ClientProfessionRestrictionHandler {

    /**
     * Vérifie périodiquement et vide le slot SILENCIEUSEMENT
     * PAS de messages - le serveur s'en charge
     */
    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        // Ignorer les joueurs en mode créatif (le serveur ne vérifie pas non plus)
        if (player.isCreative()) {
            return;
        }

        // Vérifier toutes les 5 ticks (0.25s)
        if (player.tickCount % 5 != 0) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;

        // Vider le craft non autorisé SILENCIEUSEMENT
        if (menu instanceof CraftingMenu) {
            checkCraftResult(player, (CraftingMenu) menu);
        }

        // Retirer les armures non autorisées SILENCIEUSEMENT
        if (menu instanceof InventoryMenu) {
            checkArmorSlots(player, (InventoryMenu) menu);
        }
    }

    /**
     * Vide le slot de craft SANS message
     */
    private static void checkCraftResult(LocalPlayer player, CraftingMenu menu) {
        try {
            ItemStack result = menu.getSlot(0).getItem();

            if (!result.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());

                if (ClientProfessionRestrictions.isCraftBlocked(itemId.toString())) {
                    // Vider SILENCIEUSEMENT - le serveur enverra le message
                    menu.getSlot(0).set(ItemStack.EMPTY);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Retire les armures SANS message
     */
    private static void checkArmorSlots(LocalPlayer player, InventoryMenu menu) {
        try {
            // Slots 5-8 = armure
            for (int slotIndex = 5; slotIndex <= 8; slotIndex++) {
                ItemStack armorPiece = menu.getSlot(slotIndex).getItem();

                if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ArmorItem) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armorPiece.getItem());

                    if (ClientProfessionRestrictions.isEquipmentBlocked(itemId.toString())) {
                        // Retirer SILENCIEUSEMENT - le serveur enverra le message
                        menu.getSlot(slotIndex).set(ItemStack.EMPTY);

                        // Remettre dans l'inventaire
                        if (!player.getInventory().add(armorPiece)) {
                            player.drop(armorPiece, false);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}