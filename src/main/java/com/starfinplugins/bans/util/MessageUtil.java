
package com.starfinplugins.bans.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {

    private final FileConfiguration config;

    public MessageUtil(FileConfiguration config) {
        this.config = config;
    }

    public String get(String path) {
        String value = config.getString(path);
        if (value == null) return ChatColor.RED + "Message not found: " + path;
        String prefix = config.getString("prefix", "&9&lSTARFINPLUGINS &8» &r");
        return ChatColor.translateAlternateColorCodes('&', value.replace("%prefix%", prefix));
    }

    public String getRaw(String path) {
        return get(path);
    }

    public String getLayout(String path) {
        if (!config.contains(path)) return "§cLayout not found: " + path;
        List<String> lines = config.getStringList(path);
        String prefix = config.getString("prefix", "&9&lSTARFINPLUGINS &8» &r");
        return lines.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line.replace("%prefix%", prefix)))
                .collect(Collectors.joining("\n"));
    }

    public void sendBroadcast(String path, String player, String staff, String reason, String duration) {
        if (!config.contains(path)) return;
        String msg = get(path)
                .replace("%player%", player)
                .replace("%staff%", staff)
                .replace("%reason%", reason != null ? reason : "")
                .replace("%duration%", duration != null ? duration : "");
        Bukkit.broadcastMessage(msg);
    }
}
