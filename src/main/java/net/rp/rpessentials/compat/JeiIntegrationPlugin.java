package net.rp.rpessentials.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() == null) return;

            List<T> toHide = new ArrayList<>();

            runtime.getRecipeManager().createRecipeLookup(type).get().forEach(recipe -> {
                try {
                    if (!(recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder)) return;

                    // En 1.21.2+, getResultItem est remplacé par display() -> RecipeDisplay -> resultDisplay()
                    for (net.minecraft.world.item.crafting.display.RecipeDisplay display : holder.value().display()) {
                        net.minecraft.world.item.crafting.display.SlotDisplay result = display.result();
                        // SlotDisplay.ItemSlotDisplay contient directement l'item
                        if (result instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay itemDisplay) {
                            String id = BuiltInRegistries.ITEM.getKey(itemDisplay.item().value()).toString();
                            if (ClientProfessionRestrictions.isCraftBlocked(id)) {
                                toHide.add(recipe);
                                return;
                            }
                        } else if (result instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay stackDisplay) {
                            String id = BuiltInRegistries.ITEM.getKey(stackDisplay.stack().getItem()).toString();
                            if (ClientProfessionRestrictions.isCraftBlocked(id)) {
                                toHide.add(recipe);
                                return;
                            }
                        }
                    }
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