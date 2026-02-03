package net.oneria.oneriaserverutilities.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.oneria.oneriaserverutilities.NicknameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.chat.Component;

@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * Intercepte getCustomName() pour afficher le nickname au-dessus de la tête du joueur
     */
    @Inject(
            method = "getCustomName",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onGetCustomName(CallbackInfoReturnable<Component> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Component nametagDisplay = NicknameManager.getNametagDisplay(serverPlayer);
            if (nametagDisplay != null) {
                cir.setReturnValue(nametagDisplay);
            }
        }
    }
}