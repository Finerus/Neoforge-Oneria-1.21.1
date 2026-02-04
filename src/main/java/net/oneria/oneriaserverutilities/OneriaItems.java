package net.oneria.oneriaserverutilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oneria.oneriaserverutilities.items.LicenseItem;

public class OneriaItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OneriaServerUtilities.MODID);

    public static final DeferredHolder<Item, LicenseItem> LICENSE = ITEMS.register("license",
            () -> new LicenseItem(new Item.Properties().stacksTo(1))); // Max 1 par stack
}