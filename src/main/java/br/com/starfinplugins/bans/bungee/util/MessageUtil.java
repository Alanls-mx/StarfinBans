package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {

    private final Configuration cfg;

    public MessageUtil(Configuration cfg) {
        this.cfg = cfg;
    }

    public String prefix() {
        return color(cfg.getString("prefix", ""));
    }

    public String get(String path) {
        if (cfg.get(path) == null) {
            return color("&cMessage not found: " + path);
        }
        return color(cfg.getString(path, ""));
    }

    public String getRaw(String path) {
        if (cfg.get(path) == null) {
            return color("&cMessage not found: " + path);
        }
        return color(cfg.getString(path, ""));
    }

    public String getLayout(String path) {
        if (cfg.get(path) == null) {
            return color("&cLayout not found: " + path);
        }
        List<String> lines = cfg.getStringList(path);
        return lines.stream()
                .map(this::color)
                .collect(Collectors.joining("\n"));
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
