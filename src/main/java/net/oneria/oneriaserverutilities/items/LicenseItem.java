package net.oneria.oneriaserverutilities.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LicenseItem extends Item {

    public LicenseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
        }

        if (stack.has(net.minecraft.core.component.DataComponents.LORE)) {
        }

        tooltip.add(Component.literal("§7Document officiel").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§8Non transférable").withStyle(ChatFormatting.DARK_GRAY));
    }

}