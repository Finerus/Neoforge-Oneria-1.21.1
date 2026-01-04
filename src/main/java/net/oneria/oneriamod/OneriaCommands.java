package net.oneria.oneriamod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class OneriaCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("oneria")
                .requires(source -> source.hasPermission(2)) // Nécessite OP niveau 2

                // /oneria reload - Recharge la config
                .then(Commands.literal("reload")
                        .executes(context -> {
                            OneriaConfig.SPEC.save();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§a[Oneria] Configuration rechargée !"), true);
                            return 1;
                        }))

                // /oneria set proximityDistance <valeur>
                .then(Commands.literal("set")
                        .then(Commands.literal("proximityDistance")
                                .then(Commands.argument("distance", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> setProximityDistance(context))))

                        // /oneria set obfuscatedNameLength <valeur>
                        .then(Commands.literal("obfuscatedNameLength")
                                .then(Commands.argument("length", IntegerArgumentType.integer(1, 16))
                                        .executes(context -> setObfuscatedNameLength(context))))

                        // /oneria set enableBlur <true/false>
                        .then(Commands.literal("enableBlur")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setEnableBlur(context))))

                        // /oneria set opsSeeAll <true/false>
                        .then(Commands.literal("opsSeeAll")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setOpsSeeAll(context))))

                        // /oneria set debugSelfBlur <true/false>
                        .then(Commands.literal("debugSelfBlur")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setDebugSelfBlur(context))))

                        // /oneria set obfuscatePrefix <true/false>
                        .then(Commands.literal("obfuscatePrefix")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setObfuscatePrefix(context)))))

                // /oneria whitelist add <joueur>
                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> addToWhitelist(context))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> removeFromWhitelist(context))))
                        .then(Commands.literal("list")
                                .executes(context -> listWhitelist(context))))

                // /oneria status - Affiche la config actuelle
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context)))
        );
    }

    private static int setProximityDistance(CommandContext<CommandSourceStack> context) {
        int distance = IntegerArgumentType.getInteger(context, "distance");
        OneriaConfig.PROXIMITY_DISTANCE.set(distance);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Distance de proximité définie à " + distance + " blocs"), true);
        return 1;
    }

    private static int setObfuscatedNameLength(CommandContext<CommandSourceStack> context) {
        int length = IntegerArgumentType.getInteger(context, "length");
        OneriaConfig.OBFUSCATED_NAME_LENGTH.set(length);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Longueur du pseudo obfusqué définie à " + length + " caractères"), true);
        return 1;
    }

    private static int setEnableBlur(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        OneriaConfig.ENABLE_BLUR.set(enabled);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Obfuscation " + (enabled ? "activée" : "désactivée")), true);
        return 1;
    }

    private static int setOpsSeeAll(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        OneriaConfig.OPS_SEE_ALL.set(enabled);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Les OPs " + (enabled ? "voient" : "ne voient pas") + " tous les pseudos"), true);
        return 1;
    }

    private static int setDebugSelfBlur(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        OneriaConfig.DEBUG_SELF_BLUR.set(enabled);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Mode debug auto-obfuscation " + (enabled ? "activé" : "désactivé")), true);
        return 1;
    }

    private static int setObfuscatePrefix(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        OneriaConfig.OBFUSCATE_PREFIX.set(enabled);
        OneriaConfig.SPEC.save();
        context.getSource().sendSuccess(() ->
                Component.literal("§a[Oneria] Obfuscation des grades " + (enabled ? "activée" : "désactivée")), true);
        return 1;
    }

    private static int addToWhitelist(CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(context, "player");
        var whitelist = OneriaConfig.WHITELIST.get();
        if (!whitelist.contains(player)) {
            var newList = new java.util.ArrayList<String>();
            newList.addAll(whitelist);
            newList.add(player);
            OneriaConfig.WHITELIST.set(newList);
            OneriaConfig.SPEC.save();
            context.getSource().sendSuccess(() ->
                    Component.literal("§a[Oneria] " + player + " ajouté à la whitelist"), true);
        } else {
            context.getSource().sendFailure(
                    Component.literal("§c[Oneria] " + player + " est déjà dans la whitelist"));
        }
        return 1;
    }

    private static int removeFromWhitelist(CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(context, "player");
        var whitelist = OneriaConfig.WHITELIST.get();
        if (whitelist.contains(player)) {
            var newList = new java.util.ArrayList<String>();
            newList.addAll(whitelist);
            newList.remove(player);
            OneriaConfig.WHITELIST.set(newList);
            OneriaConfig.SPEC.save();
            context.getSource().sendSuccess(() ->
                    Component.literal("§a[Oneria] " + player + " retiré de la whitelist"), true);
        } else {
            context.getSource().sendFailure(
                    Component.literal("§c[Oneria] " + player + " n'est pas dans la whitelist"));
        }
        return 1;
    }

    private static int listWhitelist(CommandContext<CommandSourceStack> context) {
        var whitelist = OneriaConfig.WHITELIST.get();
        if (whitelist.isEmpty()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("§e[Oneria] Whitelist vide"), false);
        } else {
            context.getSource().sendSuccess(() ->
                    Component.literal("§e[Oneria] Whitelist : " + String.join(", ", whitelist)), false);
        }
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6=== Configuration Oneria ===" +
                        "\n§eObfuscation activée: §f" + OneriaConfig.ENABLE_BLUR.get() +
                        "\n§eDistance de proximité: §f" + OneriaConfig.PROXIMITY_DISTANCE.get() + " blocs" +
                        "\n§eLongueur pseudo obfusqué: §f" + OneriaConfig.OBFUSCATED_NAME_LENGTH.get() + " caractères" +
                        "\n§eObfusquer les grades: §f" + OneriaConfig.OBFUSCATE_PREFIX.get() +
                        "\n§eOPs voient tout: §f" + OneriaConfig.OPS_SEE_ALL.get() +
                        "\n§eMode debug auto-blur: §f" + OneriaConfig.DEBUG_SELF_BLUR.get() +
                        "\n§eWhitelist: §f" + String.join(", ", OneriaConfig.WHITELIST.get())
        ), false);
        return 1;
    }
}