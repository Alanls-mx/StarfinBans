package br.com.starfinplugins.bans.bungee.listener;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/** Remains active while authorization is pending or revoked. */
public final class LicenseGateListener implements Listener {
    private final StarfinBansBungeePlugin plugin;
    public LicenseGateListener(StarfinBansBungeePlugin plugin) { this.plugin = plugin; }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        if (!plugin.isLicensed()) {
            event.setCancelled(true);
            event.setCancelReason("Autorizacao de moderacao indisponivel. Tente novamente mais tarde.");
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (!plugin.isLicensed()) event.setCancelled(true);
    }
}
