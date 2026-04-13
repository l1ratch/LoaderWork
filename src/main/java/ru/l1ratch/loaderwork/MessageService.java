package ru.l1ratch.loaderwork;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;

public class MessageService {

    private final LoaderWork plugin;

    public MessageService(LoaderWork plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.<String, String>emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = render(key, placeholders, true);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void actionBar(Player player, String key, Map<String, String> placeholders) {
        actionBarRaw(player, render(key, placeholders, false));
    }

    public void actionBarRaw(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(colorize(message)));
    }

    public String render(String key, Map<String, String> placeholders, boolean withPrefix) {
        FileConfiguration config = plugin.getConfig();
        String template = config.getString("messages." + key, "");
        if (template == null || template.isEmpty()) {
            return "";
        }

        String rendered = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        if (withPrefix) {
            rendered = config.getString("messages.prefix", "") + rendered;
        }

        return colorize(rendered);
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
