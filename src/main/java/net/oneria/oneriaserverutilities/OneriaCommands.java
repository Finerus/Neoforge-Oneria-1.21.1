package net.oneria.oneriaserverutilities;

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
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = OneriaServerUtilities.MODID)
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

        // Dans la section CONFIG NODE
        configNode.then(Commands.literal("reload")
                .executes(OneriaCommands::reloadConfig));

        // Obfuscation settings
        setNode.then(Commands.literal("proximity")
                .then(Commands.argument("value", IntegerArgumentType.integer(1, 128))
                        .executes(ctx -> updateConfigInt(ctx, OneriaConfig.PROXIMITY_DISTANCE, "Proximity distance"))));

        setNode.then(Commands.literal("blur")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_BLUR, "Blur"))));

        setNode.then(Commands.literal("obfuscatedNameLength")
                .then(Commands.argument("value", IntegerArgumentType.integer(1, 16))
                        .executes(ctx -> updateConfigInt(ctx, OneriaConfig.OBFUSCATED_NAME_LENGTH, "Hidden name length"))));

        setNode.then(Commands.literal("obfuscatePrefix")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.OBFUSCATE_PREFIX, "Obfuscate prefix"))));

        setNode.then(Commands.literal("opsSeeAll")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.OPS_SEE_ALL, "Admin View"))));

        setNode.then(Commands.literal("debugSelfBlur")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.DEBUG_SELF_BLUR, "Debug Self Blur"))));

        // Schedule settings
        setNode.then(Commands.literal("enableSchedule")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_SCHEDULE, "Schedule System"))));

        setNode.then(Commands.literal("openingTime")
                .then(Commands.argument("time", StringArgumentType.word())
                        .executes(OneriaCommands::setOpeningTime)));

        setNode.then(Commands.literal("closingTime")
                .then(Commands.argument("time", StringArgumentType.word())
                        .executes(OneriaCommands::setClosingTime)));

        setNode.then(Commands.literal("kickNonStaff")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.KICK_NON_STAFF, "Kick Non-Staff"))));

        // Welcome settings
        setNode.then(Commands.literal("enableWelcome")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_WELCOME, "Welcome Message"))));

        // Platform settings
        setNode.then(Commands.literal("enablePlatforms")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_PLATFORMS, "Platforms System"))));

        // Silent commands settings
        setNode.then(Commands.literal("enableSilentCommands")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.ENABLE_SILENT_COMMANDS, "Silent Commands"))));

        setNode.then(Commands.literal("logToStaff")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.LOG_TO_STAFF, "Log to Staff"))));

        setNode.then(Commands.literal("logToConsole")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.LOG_TO_CONSOLE, "Log to Console"))));

        setNode.then(Commands.literal("notifyTarget")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.NOTIFY_TARGET, "Notify Target"))));

        // Permission settings
        setNode.then(Commands.literal("opLevelBypass")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 4))
                        .executes(ctx -> updateConfigInt(ctx, OneriaConfig.OP_LEVEL_BYPASS, "OP Level Bypass"))));

        setNode.then(Commands.literal("useLuckPermsGroups")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> updateConfigBool(ctx, OneriaConfig.USE_LUCKPERMS_GROUPS, "Use LuckPerms Groups"))));

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
        // 4. MODULE: NICKNAME (Requires OP Level 2)
        // -------------------------------------------------------------------------
        var nickNode = Commands.literal("nick")
                .requires(source -> source.hasPermission(2));

        nickNode.then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                        .executes(OneriaCommands::setNickname)
                )
                .executes(OneriaCommands::resetNickname)
        );

        nickNode.then(Commands.literal("list")
                .executes(OneriaCommands::listNicknames)
        );

        oneriaRoot.then(nickNode);

        // -------------------------------------------------------------------------
        // 5. MODULE: SCHEDULE (Public)
        // -------------------------------------------------------------------------
        oneriaRoot.then(Commands.literal("schedule")
                .executes(OneriaCommands::showSchedule));

        // Register root
        dispatcher.register(oneriaRoot);

// =========================================================================
// HANDY ALIASES
// =========================================================================
        dispatcher.register(Commands.literal("schedule")
                .executes(OneriaCommands::showSchedule));

        dispatcher.register(Commands.literal("horaires")
                .executes(OneriaCommands::showSchedule));

        dispatcher.register(Commands.literal("platform")
                .requires(src -> OneriaPermissions.isStaff(src.getPlayer()))
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
                ));
    }

    // =============================================================================
    // IMPLEMENTATION LOGIC (HANDLERS)
    // =============================================================================

    // --- CONFIG HANDLERS ---

    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        OneriaScheduleManager.reload();
        OneriaPermissions.clearCache();
        NicknameManager.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] Configuration and nicknames reloaded!"), true);
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
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + name + " : " + (val ? "§aENABLED" : "§cDISABLED")), true);
        return 1;
    }

    private static int setOpeningTime(CommandContext<CommandSourceStack> ctx) {
        String time = StringArgumentType.getString(ctx, "time");
        if (!time.matches("\\d{2}:\\d{2}")) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid format! Use HH:MM (e.g., 19:00)"));
            return 0;
        }
        OneriaConfig.OPENING_TIME.set(time);
        OneriaConfig.SPEC.save();
        OneriaScheduleManager.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] Opening time set to: " + time), true);
        return 1;
    }

    private static int setClosingTime(CommandContext<CommandSourceStack> ctx) {
        String time = StringArgumentType.getString(ctx, "time");
        if (!time.matches("\\d{2}:\\d{2}")) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid format! Use HH:MM (e.g., 23:59)"));
            return 0;
        }
        OneriaConfig.CLOSING_TIME.set(time);
        OneriaConfig.SPEC.save();
        OneriaScheduleManager.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Oneria] Closing time set to: " + time), true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6╔═══════════════════════════════════╗\n" +
                        "§6║  §e§lONERIA MOD - STATUS§r          §6║\n" +
                        "§6╠═══════════════════════════════════╣\n" +
                        "§6║ §7Obfuscation\n" +
                        "§6║  §eBlur: §f" + OneriaConfig.ENABLE_BLUR.get() + "\n" +
                        "§6║  §eProximity: §f" + OneriaConfig.PROXIMITY_DISTANCE.get() + " blocks\n" +
                        "§6║  §eObfuscate Prefix: §f" + OneriaConfig.OBFUSCATE_PREFIX.get() + "\n" +
                        "§6║  §eOPs See All: §f" + OneriaConfig.OPS_SEE_ALL.get() + "\n" +
                        "§6║\n" +
                        "§6║ §7Schedule\n" +
                        "§6║  §eEnabled: §f" + OneriaConfig.ENABLE_SCHEDULE.get() + "\n" +
                        "§6║  §eStatus: " + (OneriaScheduleManager.isServerOpen() ? "§aOPEN" : "§cCLOSED") + "\n" +
                        "§6║  §eOpening: §f" + OneriaConfig.OPENING_TIME.get() + "\n" +
                        "§6║  §eClosing: §f" + OneriaConfig.CLOSING_TIME.get() + "\n" +
                        "§6║\n" +
                        "§6║ §7Moderation\n" +
                        "§6║  §eSilent Commands: §f" + OneriaConfig.ENABLE_SILENT_COMMANDS.get() + "\n" +
                        "§6║  §ePlatforms: §f" + OneriaConfig.ENABLE_PLATFORMS.get() + "\n" +
                        "§6║  §eWelcome Message: §f" + OneriaConfig.ENABLE_WELCOME.get() + "\n" +
                        "§6╚═══════════════════════════════════╝"
        ), false);
        return 1;
    }

    // --- WHITELIST HANDLERS ---

    private static int addToWhitelist(CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(context, "player");
        List<String> list = new ArrayList<>(OneriaConfig.WHITELIST.get());
        if (!list.contains(player)) {
            list.add(player);
            OneriaConfig.WHITELIST.set(list);
            OneriaConfig.SPEC.save();
            context.getSource().sendSuccess(() -> Component.literal("§a[Oneria] " + player + " added to whitelist."), true);
        } else {
            context.getSource().sendFailure(Component.literal("§c[Oneria] " + player + " is already in whitelist."));
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
        } else {
            context.getSource().sendFailure(Component.literal("§c[Oneria] " + player + " is not in whitelist."));
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        List<? extends String> whitelist = OneriaConfig.WHITELIST.get();
        if (whitelist.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§e[Oneria] Whitelist is empty."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§e[Oneria] Whitelist: §f" + String.join(", ", whitelist)), false);
        }
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
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) {
            ctx.getSource().sendFailure(Component.literal("§cSilent commands are disabled."));
            return 0;
        }

        GameType type = parseGameMode(modeName);
        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid gamemode."));
            return 0;
        }

        target.setGameMode(type);

        String sourceName = ctx.getSource().getPlayer() != null ? ctx.getSource().getPlayer().getName().getString() : "Console";
        logToStaff(ctx.getSource(), sourceName + " set " + target.getName().getString() + " to " + modeName);

        if (OneriaConfig.NOTIFY_TARGET.get() && target != ctx.getSource().getEntity()) {
            target.sendSystemMessage(Component.literal("§7[Staff] Your gamemode has been changed to " + modeName));
        }
        return 1;
    }

    private static int silentTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) {
            ctx.getSource().sendFailure(Component.literal("§cSilent commands are disabled."));
            return 0;
        }

        ServerPlayer executor = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        executor.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        logToStaff(ctx.getSource(), executor.getName().getString() + " TP'd to " + target.getName().getString());
        return 1;
    }

    private static int silentEffect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!OneriaConfig.ENABLE_SILENT_COMMANDS.get()) {
            ctx.getSource().sendFailure(Component.literal("§cSilent commands are disabled."));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation effectId = ResourceLocationArgument.getId(ctx, "effect");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        int amplifier = IntegerArgumentType.getInteger(ctx, "amplifier");

        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(BuiltInRegistries.MOB_EFFECT.key(), effectId));

        if (effect.isPresent()) {
            target.addEffect(new MobEffectInstance(effect.get(), duration * 20, amplifier, false, false));
            String sourceName = ctx.getSource().getPlayer() != null ? ctx.getSource().getPlayer().getName().getString() : "Console";
            logToStaff(ctx.getSource(), sourceName + " gave effect " + effectId + " to " + target.getName().getString());

            if (OneriaConfig.NOTIFY_TARGET.get()) {
                target.sendSystemMessage(Component.literal("§7[Staff] An effect has been applied to you."));
            }
        } else {
            ctx.getSource().sendFailure(Component.literal("§cInvalid effect."));
            return 0;
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
        if (!OneriaConfig.ENABLE_PLATFORMS.get()) {
            source.sendFailure(Component.literal("§cPlatforms system is disabled."));
            return 0;
        }

        for (String pData : OneriaConfig.PLATFORMS.get()) {
            String[] parts = pData.split(";");
            if (parts.length >= 6 && parts[0].equals(platformId)) {
                ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(parts[2]));
                ServerLevel level = source.getServer().getLevel(dim);
                if (level == null) {
                    source.sendFailure(Component.literal("§cDimension not found: " + parts[2]));
                    continue;
                }

                Vec3 pos = new Vec3(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));

                if (target != null) {
                    target.teleportTo(level, pos.x, pos.y, pos.z, 0, 0);
                    target.sendSystemMessage(Component.literal("§e[Staff] You have been teleported to: " + parts[1]));
                    logToStaff(source, executor.getName().getString() + " TP'd " + target.getName().getString() + " to " + parts[1]);
                } else {
                    executor.teleportTo(level, pos.x, pos.y, pos.z, 0, 0);
                    executor.sendSystemMessage(Component.literal("§aTeleported to: " + parts[1]));
                }
                return 1;
            }
        }
        source.sendFailure(Component.literal("§cPlatform not found: " + platformId));
        return 0;
    }

    // --- SCHEDULE HANDLER ---

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

    // --- UTILS ---

    private static void logToStaff(CommandSourceStack source, String msg) {
        if (!OneriaConfig.LOG_TO_STAFF.get()) return;
        Component txt = Component.literal("§7§o[StaffLog] " + msg);
        source.getServer().getPlayerList().getPlayers().forEach(p -> {
            if (OneriaPermissions.isStaff(p)) p.sendSystemMessage(txt);
        });
        if (OneriaConfig.LOG_TO_CONSOLE.get()) OneriaServerUtilities.LOGGER.info("[StaffLog] " + msg);
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

    // --- NICKNAME HANDLERS ---

    private static int setNickname(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String nickname = StringArgumentType.getString(context, "nickname");

            // Support des codes couleur & et §
            String formattedNickname = nickname.replace("&", "§");

            // Stocker dans le NicknameManager
            NicknameManager.setNickname(target.getUUID(), formattedNickname);

            // Forcer la mise à jour du TabList pour tous
            target.getServer().getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(
                            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                            List.of(target)
                    )
            );

            context.getSource().sendSuccess(() ->
                    Component.literal("§a[Oneria] Nickname de §f" + target.getName().getString() +
                            "§a défini : " + formattedNickname), true);

            target.sendSystemMessage(Component.literal("§aVotre surnom a été changé en : " + formattedNickname));

            OneriaServerUtilities.LOGGER.info("Nickname set for {}: {}", target.getName().getString(), formattedNickname);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cErreur lors de la définition du surnom."));
            OneriaServerUtilities.LOGGER.error("Error setting nickname", e);
            return 0;
        }
    }

    private static int resetNickname(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");

            // Retirer du NicknameManager
            NicknameManager.removeNickname(target.getUUID());

            // Forcer la mise à jour du TabList
            target.getServer().getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(
                            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                            List.of(target)
                    )
            );

            context.getSource().sendSuccess(() ->
                    Component.literal("§a[Oneria] Nickname de §f" + target.getName().getString() + "§a réinitialisé."), true);

            target.sendSystemMessage(Component.literal("§aVotre surnom a été réinitialisé."));

            OneriaServerUtilities.LOGGER.info("Nickname reset for {}", target.getName().getString());

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cErreur lors de la réinitialisation."));
            OneriaServerUtilities.LOGGER.error("Error resetting nickname", e);
            return 0;
        }
    }

    private static int listNicknames(CommandContext<CommandSourceStack> context) {
        int count = NicknameManager.count();

        if (count == 0) {
            context.getSource().sendSuccess(() ->
                    Component.literal("§e[Oneria] Aucun nickname actif."), false);
            return 1;
        }

        StringBuilder list = new StringBuilder("§6╔═══════════════════════════════════╗\n");
        list.append("§6║ §e§lNICKNAMES ACTIFS §6(§e").append(count).append("§6)\n");
        list.append("§6╠═══════════════════════════════════╣\n");

        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            if (NicknameManager.hasNickname(player.getUUID())) {
                String nickname = NicknameManager.getNickname(player.getUUID());
                list.append("§6║ §f")
                        .append(player.getName().getString())
                        .append(" §7→ ")
                        .append(nickname)
                        .append("\n");
            }
        });

        list.append("§6╚═══════════════════════════════════╝");

        context.getSource().sendSuccess(() -> Component.literal(list.toString()), false);
        return 1;
    }
}