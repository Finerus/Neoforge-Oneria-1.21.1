package net.rp.rpessentials.profession;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.rp.rpessentials.RpEssentials;
import net.rp.rpessentials.config.ProfessionConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire d'événements pour les restrictions de métiers.
 *
 * Craft    → MixinResultSlot      (mayPickup)
 * Armure   → MixinArmorSlot       (mayPlace) + MixinArmorItemUse (use)
 * Le reste → events NeoForge ci-dessous
 */
@EventBusSubscriber(modid = RpEssentials.MODID)
public class ProfessionRestrictionEventHandler {

    // Anti-spam messages (cooldown 3 secondes)
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 3000L;

    // Anti-spam attaques (cooldown 2 secondes)
    private static final Map<UUID, Long> lastAttackWarning = new ConcurrentHashMap<>();
    private static final long ATTACK_WARNING_COOLDOWN = 2000L;

    // =========================================================================
    // UTILITAIRE ANTI-SPAM
    // =========================================================================

    private static boolean canSendMessage(UUID playerId, Map<UUID, Long> cache, long cooldown) {
        long now = System.currentTimeMillis();
        Long lastTime = cache.get(playerId);
        if (lastTime == null || now - lastTime > cooldown) {
            cache.put(playerId, now);
            return true;
        }
        return false;
    }

    // =========================================================================
    // BLOCK BREAK RESTRICTIONS
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        Block block = event.getState().getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (!ProfessionRestrictionManager.canBreakBlock(player, blockId)) {
            event.setCanceled(true);
            if (canSendMessage(player.getUUID(), lastMessageTime, MESSAGE_COOLDOWN)) {
                player.displayClientMessage(
                        Component.literal(ProfessionRestrictionManager.getBlockBreakBlockedMessage(blockId)),
                        true
                );
            }
            RpEssentials.LOGGER.debug("[ProfessionRestrictions] Blocked block break for {}: {}",
                    player.getName().getString(), blockId);
        }
    }

    // =========================================================================
    // ITEM USE RESTRICTIONS (clic droit item + clic droit sur bloc)
    // Les deux passent par le même check — helper factorisé.
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isCreative()) return;
        checkItemUse(event.getEntity(), event.getItemStack(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isCreative()) return;
        checkItemUse(event.getEntity(), event.getItemStack(), event);
    }

    private static void checkItemUse(net.minecraft.world.entity.Entity entity,
                                     ItemStack itemStack,
                                     net.neoforged.bus.api.ICancellableEvent event) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (itemStack.isEmpty()) return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());

        if (!ProfessionRestrictionManager.canUseItem(player, itemId)) {
            event.setCanceled(true);
            if (canSendMessage(player.getUUID(), lastMessageTime, MESSAGE_COOLDOWN)) {
                player.displayClientMessage(
                        Component.literal(ProfessionRestrictionManager.getItemUseBlockedMessage(itemId)),
                        true
                );
            }
        }
    }

    // =========================================================================
    // LEFT-CLICK BLOCK (outil non autorisé)
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        ItemStack itemStack = player.getItemInHand(event.getHand());
        if (itemStack.isEmpty()) return;

        Item item = itemStack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        if (!ProfessionRestrictionManager.canUseItem(player, itemId)) {
            event.setCanceled(true);
            if (canSendMessage(player.getUUID(), lastMessageTime, MESSAGE_COOLDOWN)) {
                player.displayClientMessage(
                        Component.literal(ProfessionRestrictionManager.getItemUseBlockedMessage(itemId)),
                        true
                );
            }
        }
    }

    // =========================================================================
    // WEAPON/TOOL ATTACK RESTRICTIONS
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        ItemStack weaponStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (weaponStack.isEmpty()) return;

        Item weapon = weaponStack.getItem();
        ResourceLocation weaponId = BuiltInRegistries.ITEM.getKey(weapon);

        if (!ProfessionRestrictionManager.canEquip(player, weaponId)) {
            event.setCanceled(true);
            if (canSendMessage(player.getUUID(), lastAttackWarning, ATTACK_WARNING_COOLDOWN)) {
                player.displayClientMessage(
                        Component.literal(ProfessionRestrictionManager.getEquipmentBlockedMessage(weaponId)),
                        true
                );
            }
            RpEssentials.LOGGER.debug("[ProfessionRestrictions] Blocked weapon attack for {}: {}",
                    player.getName().getString(), weaponId);
        }
    }

    // =========================================================================
    // TOOLTIP INFORMATION
    // Covers all 5 restriction types:
    //  - globalBlockedCrafts
    //  - globalUnbreakableBlocks  ← was missing
    //  - globalBlockedItems
    //  - globalBlockedEquipment
    //  - containerOpenRestrictions ← was missing
    // For blocks, the item resource location matches the block id for all
    // standard blocks (minecraft:diamond_ore item == minecraft:diamond_ore block).
    // =========================================================================

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) return;

        Item item = itemStack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation blockId = itemId; // for standard blocks, item id == block id

        try {
            // ── Craft restriction ─────────────────────────────────────────────
            if (ProfessionRestrictionManager.isGloballyBlocked(
                    itemId, ProfessionConfig.GLOBAL_BLOCKED_CRAFTS.get())) {
                String professions = ProfessionRestrictionManager.getRequiredProfessions(
                        itemId, ProfessionConfig.PROFESSION_ALLOWED_CRAFTS.get());
                event.getToolTip().add(Component.literal("§7Craft: §c✘ §8— §7" + professions));
            }

            // ── Block break restriction ───────────────────────────────────────
            // Checks using blockId (same value as itemId for standard blocks).
            if (ProfessionRestrictionManager.isGloballyBlocked(
                    blockId, ProfessionConfig.GLOBAL_UNBREAKABLE_BLOCKS.get())) {
                String professions = ProfessionRestrictionManager.getRequiredProfessions(
                        blockId, ProfessionConfig.PROFESSION_ALLOWED_BLOCKS.get());
                event.getToolTip().add(Component.literal("§7Mining: §c✘ §8— §7" + professions));
            }

            // ── Item use restriction ──────────────────────────────────────────
            if (ProfessionRestrictionManager.isGloballyBlocked(
                    itemId, ProfessionConfig.GLOBAL_BLOCKED_ITEMS.get())) {
                String professions = ProfessionRestrictionManager.getRequiredProfessions(
                        itemId, ProfessionConfig.PROFESSION_ALLOWED_ITEMS.get());
                event.getToolTip().add(Component.literal("§7Usage: §c✘ §8— §7" + professions));
            }

            // ── Equipment restriction ─────────────────────────────────────────
            if (ProfessionRestrictionManager.isGloballyBlocked(
                    itemId, ProfessionConfig.GLOBAL_BLOCKED_EQUIPMENT.get())) {
                String professions = ProfessionRestrictionManager.getRequiredProfessions(
                        itemId, ProfessionConfig.PROFESSION_ALLOWED_EQUIPMENT.get());
                event.getToolTip().add(Component.literal("§7Equip: §c✘ §8— §7" + professions));
            }

            // ── Container open restriction ────────────────────────────────────
            // Only relevant when the item is a placeable block (has a block form).
            // We check the container restrictions list directly since the format is
            // block_id;profession1,profession2 rather than a simple global blocked list.
            List<? extends String> containerRestrictions =
                    ProfessionConfig.CONTAINER_OPEN_RESTRICTIONS.get();
            if (!containerRestrictions.isEmpty()) {
                String blockIdStr = blockId.toString();
                for (String entry : containerRestrictions) {
                    if (!entry.contains(";")) continue;
                    String[] parts = entry.split(";", 2);
                    // Use the mod's own pattern-matching so wildcards work correctly
                    if (!net.rp.rpessentials.RpEssentialsPatternUtils.matchesPattern(
                            blockIdStr, parts[0].trim())) continue;

                    // Build the required professions display string
                    java.util.Set<String> required = new java.util.LinkedHashSet<>();
                    for (String prof : parts[1].split(",")) {
                        String profId = prof.trim();
                        net.rp.rpessentials.profession.ProfessionRestrictionManager.ProfessionData data =
                                ProfessionRestrictionManager.getProfessionData(profId);
                        required.add(data != null ? data.getFormattedName() : profId);
                    }
                    String professions = required.isEmpty()
                            ? net.rp.rpessentials.config.MessagesConfig.get(
                            net.rp.rpessentials.config.MessagesConfig.PROFESSION_NONE_AVAILABLE)
                            : String.join("§7, ", required);
                    event.getToolTip().add(Component.literal("§7Open: §c✘ §8— §7" + professions));
                    break; // one match is enough
                }
            }

        } catch (IllegalStateException ignored) {
            // Config not yet loaded — skip tooltip additions silently
        }
    }

    // =========================================================================
    // NETTOYAGE DES CACHES (appelé depuis RpEssentials.onServerTick toutes les 400 ticks)
    // =========================================================================

    public static void cleanupCaches() {
        long now = System.currentTimeMillis();
        lastMessageTime.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
        lastAttackWarning.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
    }
}