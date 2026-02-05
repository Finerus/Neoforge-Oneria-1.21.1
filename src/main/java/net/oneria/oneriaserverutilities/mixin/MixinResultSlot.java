package net.oneria.oneriaserverutilities.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.oneria.oneriaserverutilities.ProfessionRestrictionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin pour empêcher la récupération d'items craftés non autorisés
 * Bloque au niveau du slot de résultat pour éviter la consommation des ressources
 */
@Mixin(ResultSlot.class)
public abstract class MixinResultSlot {

    /**
     * Intercepte la récupération d'un item crafté (normal et shift-click)
     */
    @Inject(
            method = "onTake",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onTakeCraftedItem(Player player, ItemStack craftedItem, CallbackInfo ci) {
        // Uniquement côté serveur
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Vérifier si l'item peut être crafté
        Item item = craftedItem.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        if (!ProfessionRestrictionManager.canCraft(serverPlayer, itemId)) {
            // Bloquer la récupération de l'item
            ci.cancel();

            // Envoyer un message dans l'action bar (pas de spam chat)
            String message = ProfessionRestrictionManager.getCraftBlockedMessage(itemId);
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(message),
                    true // Action bar
            );
        }
    }
}