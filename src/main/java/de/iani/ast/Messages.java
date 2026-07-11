package de.iani.ast;

import de.iani.ast.config.PluginConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class Messages {

    private static PluginConfig config;

    private Messages() {
        throw new RuntimeException();
    }

    public static void init(PluginConfig pluginConfig) {
        config = pluginConfig;
    }

    public static Component activation(boolean b) {
        return config.activation(b);
    }

    public static void sendSuccess(CommandSender target, String message) {
        sendSuccess(target, Component.text(message));
    }

    public static void sendSuccess(CommandSender target, Component message) {
        message = config.successColor(message);
        send(target, message);
    }

    public static void sendError(CommandSender target, String message) {
        sendError(target, Component.text(message));
    }

    public static void sendError(CommandSender target, Component message) {
        message = config.errorColor(message);
        send(target, message);
    }

    public static void send(CommandSender target, String message) {
        send(target, Component.text(message));
    }

    public static void send(CommandSender target, Component message) {
        message = config.defaultColor(message);
        target.sendMessage(config.prefix().append(message));
    }
}
