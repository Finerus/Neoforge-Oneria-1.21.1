package net.rp.rpessentials.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.client.ClientProfessionRestrictions;
import net.rp.rpessentials.config.RpConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin JEI pour masquer les recettes bloquées par profession.
 * Compatible JEI 19.x / NeoForge 1.21.1.
 */
@JeiPlugin
public class JeiIntegrationPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(RpEssentials.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        String mode;
        try {
            mode = RpConfig.JEI_INTEGRATION_MODE.get().toUpperCase();
        } catch (IllegalStateException e) {
            return;
        }
        if ("SHOW_ALL".equals(mode)) return;

        filterType(runtime, RecipeTypes.CRAFTING);
        filterType(runtime, RecipeTypes.SMITHING);

        RpEssentials.LOGGER.debug("[JEI] Profession filter applied (mode={})", mode);
    }

    private <T> void filterType(IJeiRuntime runtime, mezz.jei.api.recipe.RecipeType<T> type) {
        try {
            List<T> toHide = new ArrayList<>();
            runtime.getRecipeManager().createRecipeLookup(type).get().forEach(recipe -> {
                try {
                    if (!(recipe instanceof Recipe<?> r)) return;
                    ItemStack result = r.getResultItem(RegistryAccess.EMPTY);
                    if (result.isEmpty()) return;
                    String id = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
                    if (ClientProfessionRestrictions.isCraftBlocked(id)) toHide.add(recipe);
                } catch (Exception ignored) {}
            });
            if (!toHide.isEmpty()) {
                runtime.getRecipeManager().hideRecipes(type, toHide);
            }
        } catch (Exception e) {
            RpEssentials.LOGGER.warn("[JEI] Error filtering {}: {}", type, e.getMessage());
        }
    }
}