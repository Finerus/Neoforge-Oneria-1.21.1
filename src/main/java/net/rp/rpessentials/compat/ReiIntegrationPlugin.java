package net.rp.rpessentials.compat;

import dev.architectury.event.EventResult;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.client.ClientProfessionRestrictions;
import net.rp.rpessentials.config.RpConfig;

/**
 * Plugin REI pour masquer les recettes bloquées par profession.
 * Compatible REI 16.x / NeoForge 1.21.1.
 *
 * TriState vient de me.shedaniel.rei.api.common.util.TriState.
 * DisplayVisibilityPredicate.isVisible retourne TriState.
 */
public class ReiIntegrationPlugin implements REIClientPlugin {

    @Override
    public void registerEntries(EntryRegistry registry) {
        // Optionnel : masque les items eux-mêmes de l'index REI.
        // Commenté par défaut car trop agressif — les items bloqués seraient
        // invisibles partout dans REI, pas seulement dans les recettes.
        // Décommenter si le mode HIDE_BLOCKED doit aussi masquer les items.
        /*
        if (!"SHOW_ALL".equals(getMode())) {
            registry.removeEntryIf(entry -> {
                if (entry.getType() != VanillaEntryTypes.ITEM) return false;
                try {
                    ItemStack stack = (ItemStack) entry.getValue();
                    if (stack == null || stack.isEmpty()) return false;
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    return ClientProfessionRestrictions.isCraftBlocked(id.toString());
                } catch (Exception e) { return false; }
            });
        }
        */
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if ("SHOW_ALL".equals(getMode())) return;

        registry.registerVisibilityPredicate((category, display) -> {
            try {
                for (EntryIngredient output : display.getOutputEntries()) {
                    for (EntryStack<?> stack : output) {
                        if (stack.getType() != VanillaEntryTypes.ITEM) continue;
                        @SuppressWarnings("unchecked")
                        ItemStack item = ((EntryStack<ItemStack>) stack).getValue();
                        if (item == null || item.isEmpty()) continue;
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());
                        if (ClientProfessionRestrictions.isCraftBlocked(id.toString())) {
                            return EventResult.interruptFalse();
                        }
                    }
                }
            } catch (Exception ignored) {}
            return EventResult.pass();
        });

        RpEssentials.LOGGER.debug("[REI] Profession visibility predicate registered (mode={})", getMode());
    }

    private static String getMode() {
        try {
            return RpConfig.JEI_INTEGRATION_MODE.get().toUpperCase();
        } catch (IllegalStateException e) {
            return "SHOW_ALL";
        }
    }
}