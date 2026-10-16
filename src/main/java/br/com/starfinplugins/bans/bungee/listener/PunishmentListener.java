package br.com.starfinplugins.bans.bungee.listener;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
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
        String name = event.getConnection().getName();
        String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        UUID uuid = event.getConnection().getUniqueId();

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

        if (ban == null || ban.isExpired()) return;

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) return;
        if (event.isCommand()) return;

        ProxiedPlayer p = (ProxiedPlayer) event.getSender();
        UUID uuid = p.getUniqueId();

        Punishment mute;
        try {
            mute = plugin.getStorage().getActiveMute(uuid);
            if (mute == null) {
                String ip = p.getAddress().getAddress().getHostAddress();
                mute = plugin.getStorage().getActiveIpMute(ip);
            }
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Nao foi possivel verificar os mutes no armazenamento; mensagem bloqueada por seguranca.", error);
            event.setCancelled(true);
            p.sendMessage(TextUtil.tc("&cNao foi possivel verificar sua punicao no momento. Tente novamente."));
            return;
        }
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
