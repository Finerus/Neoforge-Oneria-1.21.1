package net.oneria.oneriamod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = OneriaMod.MODID)
public class OneriaCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // =========================================================================
        // MAIN COMMAND: /oneria
        // =========================================================================
        var oneriaRoot = Commands.literal("oneria");

        // -------------------------------------------------------------------------
        // 1. MODULE: CONFIGURATION (Requires OP Level 2)
        // -------------------------------------------------------------------------
        var configNode = Commands.literal("config")
                .requires(source -> source.hasPermission(2));

        // Reload
        configNode.then(Commands.literal("reload")
                .executes(OneriaCommands::reloadConfig));

        // Status
        configNode.then(Commands.literal("status")
                .executes(OneriaCommands::showStatus));

        // Setters (On-the-fly modifications)
        var setNode = Commands.literal("set");

        setNode.then(Commands.literal("proximity")
                .then(Commands.argument("value", IntegerArgumentType.integer(1, 128))
                        .executes(ctx -> updateConfigInt(ctx, OneriaConfig.PROXIMITY_DISTANCE, "Proximity distance"))));

        setNode.then(Commands.literal("blur")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_BLUR, "Blur"))));

        setNode.then(Commands.literal("opsSeeAll")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.OPS_SEE_ALL, "Admin View"))));

        setNode.then(Commands.literal("obfuscateNameLength")
                .then(Commands.argument("value", IntegerArgumentType.integer(1, 16))
                        .executes(ctx -> updateConfigInt(ctx, OneriaConfig.OBFUSCATED_NAME_LENGTH, "Hidden name length"))));

        configNode.then(setNode);
        oneriaRoot.then(configNode);

        // -------------------------------------------------------------------------
        // 2. MODULE: STAFF & MODERATION (Requires 'isStaff' permission)
        // -------------------------------------------------------------------------
        var staffNode = Commands.literal("staff")
                .requires(src -> OneriaPermissions.isStaff(src.getPlayer()));

        // Silent Gamemode
        staffNode.then(Commands.literal("gamemode")
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("survival").suggest("creative").suggest("adventure").suggest("spectator");
                            return builder.buildFuture();
                        })
                        .executes(OneriaCommands::silentGamemodeSelf)
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(OneriaCommands::silentGamemodeTarget)
                        )
                )
        );

        // Silent Teleport
        staffNode.then(Commands.literal("tp")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(OneriaCommands::silentTeleport)
                )
        );

        // Silent Effects
        staffNode.then(Commands.literal("effect")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    BuiltInRegistries.MOB_EFFECT.keySet().forEach(loc -> builder.suggest(loc.toString()));
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                                                .executes(OneriaCommands::silentEffect)
                                        )
                                )
                        )
                )
        );

        // Platforms
        staffNode.then(Commands.literal("platform")
                .executes(OneriaCommands::platformSelf)
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(OneriaCommands::platformTarget)
                        .then(Commands.argument("platform_id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    OneriaConfig.PLATFORMS.get().forEach(p -> builder.suggest(p.split(";")[0]));
                                    return builder.buildFuture();
                                })
                                .executes(OneriaCommands::platformTargetSpecific)
                        )
                )
        );

        oneriaRoot.then(staffNode);

        // -------------------------------------------------------------------------
        // 3. MODULE: WHITELIST (Requires OP Level 2)
        // -------------------------------------------------------------------------
        var whitelistNode = Commands.literal("whitelist")
                .requires(source -> source.hasPermission(2));

        whitelistNode.then(Commands.literal("add")
                .then(Commands.argument("player", StringArgumentType.string())
                        .executes(OneriaCommands::addToWhitelist)));

        whitelistNode.then(Commands.literal("remove")
                .then(Commands.argument("player", StringArgumentType.string())
                        .executes(OneriaCommands::removeFromWhitelist)));

        whitelistNode.then(Commands.literal("list")
                .executes(OneriaCommands::listWhitelist));

        oneriaRoot.then(whitelistNode);

        // -------------------------------------------------------------------------
        // 4. MODULE: SCHEDULE (Public)
        // -------------------------------------------------------------------------
        oneriaRoot.then(Commands.literal("schedule")
                .executes(OneriaCommands::showSchedule));

        // Register root
        dispatcher.register(oneriaRoot);

        // =========================================================================
        // HANDY ALIAS: /schedule (Redirects to /oneria schedule)
        // =========================================================================
        dispatcher.register(Commands.literal("schedule")
                .executes(OneriaCommands::showSchedule));
    }

    // =============================================================================
    // IMPLEMENTATION LOGIC (HANDLERS)
    // =============================================================================

    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        OneriaScheduleManager.reload();
        OneriaPermissions.clearCache();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] Configuration reloaded successfully!"), true);
        return 1;
    }

    private static int updateConfigInt(CommandContext<CommandSourceStack> ctx, net.neoforged.neoforge.common.ModConfigSpec.IntValue config, String name) {
        int val = IntegerArgumentType.getInteger(ctx, "value");
        config.set(val);
        OneriaConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + name + " set to: " + val), true);
        return 1;
    }

    private static int updateConfigBool(CommandContext<CommandSourceStack> ctx, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue config, String name) {
        boolean val = BoolArgumentType.getBool(ctx, "value");
        config.set(val);
        OneriaConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + name + " : " + (val ? "ENABLED" : "DISABLED")), true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6=== Oneria Mod Status ===" +
                        "\n§eBlur: §f" + OneriaConfig.ENABLE_BLUR.get() +
                        "\n§eProximity: §f" + OneriaConfig.PROXIMITY_DISTANCE.get() + " blocks" +
                        "\n§eOPs Full View: §f" + OneriaConfig.OPS_SEE_ALL.get() +
                        "\n§eSchedule: §f" + (OneriaScheduleManager.isServerOpen() ? "§aOPEN" : "§cCLOSED")
        ), false);
        return 1;
    }

    // --- WHITELIST HANDLERS (FIXED ERROR) ---

    private static int addToWhitelist(CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(context, "player");
        List<String> list = new ArrayList<>(OneriaConfig.WHITELIST.get());
        if (!list.contains(player)) {
            list.add(player);
            OneriaConfig.WHITELIST.set(list);
            OneriaConfig.SPEC.save();
            context.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + player + " added to whitelist."), true);
        }
        return 1;
    }

    private static int removeFromWhitelist(CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(context, "player");
        List<String> list = new ArrayList<>(OneriaConfig.WHITELIST.get());
        if (list.remove(player)) {
            OneriaConfig.WHITELIST.set(list);
            OneriaConfig.SPEC.save();
            context.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + player + " removed from whitelist."), true);
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§eWhitelist: " + String.join(", ", OneriaConfig.WHITELIST.get())), false);
        return 1;
    }

    // --- STAFF HANDLERS (SILENT) ---

    private static int silentGamemodeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setGamemode(ctx, ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "mode"));
    }

    private static int silentGamemodeTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setGamemode(ctx, EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "mode"));
    }

    private static int setGamemode(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String modeName) {
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) return 0;
        GameType type = parseGameMode(modeName);
        if (type == null) return 0;

        target.setGameMode(type);

        String sourceName = ctx.getSource().getPlayer() != null ? ctx.getSource().getPlayer().getName().getString() : "Console";
        logToStaff(ctx.getSource(), sourceName + " set " + target.getName().getString() + " to " + modeName);

        if (OneriaConfig.NOTIFY_TARGET.get() && target != ctx.getSource().getEntity()) {
            target.sendSystemMessage(Component.literal("§7[Staff] Your game mode has been changed to " + modeName));
        }
        return 1;
    }

    private static int silentTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) return 0;
        ServerPlayer executor = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        executor.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        logToStaff(ctx.getSource(), executor.getName().getString() + " TP'd to " + target.getName().getString());
        return 1;
    }

    private static int silentEffect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) return 0;
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation effectId = ResourceLocationArgument.getId(ctx, "effect");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        int amplifier = IntegerArgumentType.getInteger(ctx, "amplifier");

        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(BuiltInRegistries.MOB_EFFECT.key(), effectId));

        if (effect.isPresent()) {
            target.addEffect(new MobEffectInstance(effect.get(), duration * 20, amplifier, false, false));
            String sourceName = ctx.getSource().getPlayer() != null ? ctx.getSource().getPlayer().getName().getString() : "Console";
            logToStaff(ctx.getSource(), sourceName + " gave effect " + effectId + " to " + target.getName().getString());
        }
        return 1;
    }

    // --- PLATFORMS HANDLERS ---

    private static int platformSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return teleportToPlatform(ctx.getSource(), ctx.getSource().getPlayerOrException(), null, "platform1");
    }

    private static int platformTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return teleportToPlatform(ctx.getSource(), ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "target"), "platform1");
    }

    private static int platformTargetSpecific(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return teleportToPlatform(ctx.getSource(), ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "platform_id"));
    }

    private static int teleportToPlatform(CommandSourceStack source, ServerPlayer executor, ServerPlayer target, String platformId) {
        if (!OneriaConfig.ENABLE_PLATFORMS.get()) return 0;

        for (String pData : OneriaConfig.PLATFORMS.get()) {
            String[] parts = pData.split(";");
            if (parts.length >= 6 && parts[0].equals(platformId)) {
                ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(parts[2]));
                ServerLevel level = source.getServer().getLevel(dim);
                if (level == null) continue;

                Vec3 pos = new Vec3(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));

                if (target != null) {
                    target.teleportTo(level, pos.x, pos.y, pos.z, 0, 0);
                    target.sendSystemMessage(Component.literal("§e[Roleplay] You have been moved to: " + parts[1]));
                    logToStaff(source, executor.getName().getString() + " TP'd " + target.getName().getString() + " to " + parts[1]);
                } else {
                    executor.teleportTo(level, pos.x, pos.y, pos.z, 0, 0);
                }
                return 1;
            }
        }
        source.sendFailure(Component.literal("§cPlatform not found."));
        return 0;
    }

    // --- UTILS ---

    private static int showSchedule(CommandContext<CommandSourceStack> ctx) {
        boolean isOpen = OneriaScheduleManager.isServerOpen();
        String timeInfo = OneriaScheduleManager.getTimeUntilNextEvent();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§8§m----------------------------------\n" +
                        " §6§lSERVER SCHEDULE\n" +
                        " §7Current Status: " + (isOpen ? "§a§lOPEN" : "§c§lCLOSED") + "\n" +
                        " §7Opening: §e" + OneriaConfig.OPENING_TIME.get() + "\n" +
                        " §7Closing: §e" + OneriaConfig.CLOSING_TIME.get() + "\n\n" +
                        " §f" + timeInfo + "\n" +
                        "§8§m----------------------------------"
        ), false);
        return 1;
    }

    private static void logToStaff(CommandSourceStack source, String msg) {
        if (!OneriaConfig.LOG_TO_STAFF.get()) return;
        Component txt = Component.literal("§7§o[StaffLog] " + msg);
        source.getServer().getPlayerList().getPlayers().forEach(p -> {
            if (OneriaPermissions.isStaff(p)) p.sendSystemMessage(txt);
        });
        if (OneriaConfig.LOG_TO_CONSOLE.get()) OneriaMod.LOGGER.info("[StaffLog] " + msg);
    }

    private static GameType parseGameMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "s", "0" -> GameType.SURVIVAL;
            case "creative", "c", "1" -> GameType.CREATIVE;
            case "adventure", "a", "2" -> GameType.ADVENTURE;
            case "spectator", "sp", "3" -> GameType.SPECTATOR;
            default -> null;
        };
    }
}