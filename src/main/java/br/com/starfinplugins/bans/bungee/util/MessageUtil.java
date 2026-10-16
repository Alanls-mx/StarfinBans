package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.text.Normalizer;
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
            return color("&c" + normalizeLegacyText("Message not found: " + path));
        }
        return color(normalizeLegacyText(cfg.getString(path, "")));
    }

    public String getRaw(String path) {
        if (cfg.get(path) == null) {
            return color("&c" + normalizeLegacyText("Message not found: " + path));
        }
        return color(normalizeLegacyText(cfg.getString(path, "")));
    }

    public String getLayout(String path) {
        if (cfg.get(path) == null) {
            return color("&c" + normalizeLegacyText("Layout not found: " + path));
        }
        List<String> lines = cfg.getStringList(path);
        return lines.stream()
                .map(line -> color(normalizeLegacyText(line)))
                .collect(Collectors.joining("\n"));
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', normalizeLegacyText(s));
    }

    public static String normalizeLegacyText(String text) {
        if (text == null) return "";
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return ascii.replaceAll("[^\\x00-\\x7F]", "");
    }
}
