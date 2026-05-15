package net.rp.rpessentials.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rp.rpessentials.config.MessagesConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class LicenseItem extends Item {

    public LicenseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   net.minecraft.client.player.LocalPlayer player,
                                                   net.minecraft.world.entity.HumanoidArm arm,
                                                   net.minecraft.world.item.ItemStack stack,
                                                   float partialTick, float equipProgress, float swingProgress) {
                net.rp.rpessentials.client.LicenseCardRenderer.applyCardTransform(
                        poseStack, arm, equipProgress, swingProgress);
                return true;
            }
        });
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.nbt.CompoundTag tag = stack.get(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.getBoolean("revoked")) {
                tooltip.add(Component.literal(MessagesConfig.get(MessagesConfig.LICENSE_REVOKED_TITLE))
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                tooltip.add(Component.literal(MessagesConfig.get(MessagesConfig.LICENSE_REVOKED_BODY))
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            }
        }
        tooltip.add(Component.literal(MessagesConfig.get(MessagesConfig.LICENSE_TOOLTIP_OFFICIAL))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(MessagesConfig.get(MessagesConfig.LICENSE_TOOLTIP_NONTRANSFERABLE))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}