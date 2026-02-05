package net.oneria.oneriaserverutilities.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.oneria.oneriaserverutilities.client.ClientProfessionRestrictions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MIXIN CLIENT-SIDE
 * Bloque les clics sur les menus AVANT qu'ils soient envoyés au serveur
 * = Aucun bypass possible
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenuClient {

    @Shadow
    public abstract ItemStack getCarried();

    /**
     * Intercepte TOUS les clics sur les slots
     * Méthode côté CLIENT qui est appelée AVANT d'envoyer au serveur
     */
    @Inject(
            method = "clicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        // Vérifier uniquement côté client
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // === CRAFT TABLE ===
        if (menu instanceof CraftingMenu) {
            // Slot 0 = résultat de craft
            if (slotId == 0) {
                ItemStack result = menu.getSlot(0).getItem();
                if (!result.isEmpty()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());

                    if (ClientProfessionRestrictions.isCraftBlocked(itemId.toString())) {
                        ci.cancel();
                        showMessage(localPlayer, "§c§lMétier requis pour crafter cet item");
                    }
                }
            }
        }

        // === INVENTAIRE (ARMURES) ===
        if (menu instanceof InventoryMenu) {
            // Slots 5-8 = armure
            if (slotId >= 5 && slotId <= 8) {
                ItemStack carried = this.getCarried();
                if (!carried.isEmpty() && carried.getItem() instanceof ArmorItem) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(carried.getItem());

                    if (ClientProfessionRestrictions.isEquipmentBlocked(itemId.toString())) {
                        ci.cancel();
                        showMessage(localPlayer, "§c§lMétier requis pour équiper cette armure");
                    }
                }
            }
        }
    }

    /**
     * Intercepte les Shift-Click
     */
    @Inject(
            method = "quickMoveStack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onQuickMoveStack(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // === CRAFT TABLE ===
        if (menu instanceof CraftingMenu && slotIndex == 0) {
            ItemStack result = menu.getSlot(0).getItem();
            if (!result.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());

                if (ClientProfessionRestrictions.isCraftBlocked(itemId.toString())) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    showMessage(localPlayer, "§c§lMétier requis pour crafter cet item");
                }
            }
        }

        // === INVENTAIRE (ARMURES) ===
        if (menu instanceof InventoryMenu) {
            ItemStack stackToMove = menu.getSlot(slotIndex).getItem();
            if (!stackToMove.isEmpty() && stackToMove.getItem() instanceof ArmorItem) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stackToMove.getItem());

                if (ClientProfessionRestrictions.isEquipmentBlocked(itemId.toString())) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    showMessage(localPlayer, "§c§lMétier requis pour équiper cette armure");
                }
            }
        }
    }

    /**
     * Affiche un message dans l'action bar
     */
    private void showMessage(LocalPlayer player, String message) {
        if (Minecraft.getInstance().gui != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }
}