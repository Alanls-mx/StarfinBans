package com.starfinplugins.bans.placeholder;

import com.starfinplugins.bans.StarfinBansPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class StarfinBansPlaceholderExpansion extends PlaceholderExpansion {
    private final StarfinBansPlugin plugin;

    public StarfinBansPlaceholderExpansion(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "starfinbans"; }

    @Override
    public String getAuthor() { return "StarfinPlugins"; }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || !plugin.isLicensed()) return "";
        if (identifier.equals("banned")) return String.valueOf(plugin.getPunishmentAPI().isBanned(player.getUniqueId()));
        if (identifier.equals("muted")) return String.valueOf(plugin.getPunishmentAPI().isMuted(player.getUniqueId()));
        return null;
    }
}
