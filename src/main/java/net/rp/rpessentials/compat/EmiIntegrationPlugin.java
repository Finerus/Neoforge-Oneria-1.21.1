package net.rp.rpessentials.compat;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.client.ClientProfessionRestrictions;
import net.rp.rpessentials.config.RpConfig;

/**
 * Plugin EMI pour masquer les recettes bloquées par profession.
 * Compatible EMI 1.x / NeoForge 1.21.1.
 *
 * Note : @EmiPlugin est une annotation dans dev.emi.emi.api.
 * EmiRecipeHandler<T extends AbstractContainerMenu> → on n'utilise pas addRecipeHandler.
 * On passe par removeRecipes(Predicate<EmiRecipe>) qui est l'API correcte.
 */

public class EmiIntegrationPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        String mode;
        try {
            mode = RpConfig.JEI_INTEGRATION_MODE.get().toUpperCase();
        } catch (IllegalStateException e) {
            return;
        }
        if ("SHOW_ALL".equals(mode)) return;

        // Masque les recettes dont l'output est un item bloqué pour le joueur courant.
        registry.removeRecipes(recipe -> {
            try {
                for (EmiIngredient output : recipe.getOutputs()) {
                    for (EmiStack stack : output.getEmiStacks()) {
                        ItemStack item = stack.getItemStack();
                        if (item == null || item.isEmpty()) continue;
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());
                        if (ClientProfessionRestrictions.isCraftBlocked(id.toString())) return true;
                    }
                }
            } catch (Exception ignored) {}
            return false;
        });

        RpEssentials.LOGGER.debug("[EMI] Profession restriction filter applied (mode={})", mode);
    }
}