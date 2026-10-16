package br.com.starfinplugins.bans.bungee.listener;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;
import br.com.starfinplugins.bans.bungee.service.OnlinePunishmentService;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;
import java.util.logging.Level;

public class PunishmentListener implements Listener {

    private final StarfinBansBungeePlugin plugin;

    public PunishmentListener(StarfinBansBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        if (!plugin.isLicensed()) return;
        String name = event.getConnection().getName();
        String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        UUID uuid = event.getConnection().getUniqueId();
        event.registerIntent(plugin);
        try {
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                try {
                    checkLogin(event, name, ip, uuid);
                } catch (RuntimeException error) {
                    plugin.getLogger().log(Level.SEVERE, "Falha ao verificar o login; conexao recusada.", error);
                    event.setCancelled(true);
                    event.setCancelReason("Nao foi possivel verificar sua punicao no momento. Tente novamente mais tarde.");
                } finally {
                    event.completeIntent(plugin);
                }
            });
        } catch (RuntimeException error) {
            event.setCancelled(true);
            event.setCancelReason("Verificacao de punicao indisponivel. Tente novamente mais tarde.");
            event.completeIntent(plugin);
        }
    }

    private void checkLogin(LoginEvent event, String name, String ip, UUID uuid) {
        if (!plugin.isLicensed()) {
            event.setCancelled(true);
            event.setCancelReason("Autorizacao de moderacao indisponivel. Tente novamente mais tarde.");
            return;
        }
        Punishment ban;
        try {
            ban = uuid != null ? plugin.getStorage().getActiveBan(uuid) : null;
            if (ban == null) ban = plugin.getStorage().getActiveBanByName(name);
            if (ban == null) ban = plugin.getStorage().getActiveIpBan(ip);
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Nao foi possivel verificar os banimentos no armazenamento; conexao recusada por seguranca.", error);
            event.setCancelled(true);
            event.setCancelReason("Nao foi possivel verificar sua punicao no momento. Tente novamente mais tarde.");
            return;
        }

        if (ban == null || ban.isExpired()) {
            plugin.getOnlinePunishments().prime(uuid, name, ip);
            return;
        }

        String duration = "Permanente";
        if (ban.getExpires() > 0) {
            duration = TimeUtil.formatDuration(ban.getExpires() - System.currentTimeMillis());
        }

        String layout = plugin.msg().getLayout("layouts.ban")
                .replace("%reason%", ban.getReason())
                .replace("%staff%", ban.getStaffName())
                .replace("%duration%", duration)
                .replace("%expires%", duration);

        event.setCancelled(true);
        event.setCancelReason(layout);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (plugin.getOnlinePunishments() != null) {
            plugin.getOnlinePunishments().forget(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (!plugin.isLicensed()) return;
        if (!(event.getSender() instanceof ProxiedPlayer)) return;
        if (event.isCommand()) return;

        ProxiedPlayer p = (ProxiedPlayer) event.getSender();
        UUID uuid = p.getUniqueId();

        OnlinePunishmentService.Snapshot snapshot = plugin.getOnlinePunishments().snapshot(uuid);
        if (snapshot == null) {
            event.setCancelled(true);
            p.sendMessage(TextUtil.tc("&cNao foi possivel verificar sua punicao no momento. Tente novamente."));
            return;
        }
        Punishment mute = snapshot.getMute();
        if (mute == null) return;
        if (mute.isExpired()) return;

        event.setCancelled(true);

        String duration = mute.getExpires() > 0
                ? TimeUtil.formatDuration(mute.getExpires() - System.currentTimeMillis())
                : "Permanente";

        String msg = plugin.msg().get("target.muted")
                .replace("%reason%", mute.getReason())
                .replace("%staff%", mute.getStaffName())
                .replace("%duration%", duration)
                .replace("%expires%", duration);

        p.sendMessage(TextUtil.tc(msg));
    }
}
