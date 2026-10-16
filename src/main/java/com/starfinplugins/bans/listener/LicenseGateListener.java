package com.starfinplugins.bans.listener;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/** Active before the first Web check and after remote revocation. */
public final class LicenseGateListener implements Listener {
    private final StarfinBansPlugin plugin;
    public LicenseGateListener(StarfinBansPlugin plugin) { this.plugin = plugin; }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.isLicensed()) event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
            "Autorizacao de moderacao indisponivel. Tente novamente mais tarde.");
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.isLicensed()) event.setCancelled(true);
    }
}
