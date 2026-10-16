package com.starfinplugins.bans.listener;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import com.starfinplugins.bans.util.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.logging.Level;

public class PunishmentListener implements Listener {

    private final StarfinBansPlugin plugin;

    public PunishmentListener(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.isLicensed()) return;
        Punishment ban;
        try {
            ban = plugin.getStorage().getActiveBan(event.getUniqueId());
            if (ban == null) ban = plugin.getStorage().getActiveBanByName(event.getName());
            if (ban == null) {
                String ip = event.getAddress().getHostAddress();
                ban = plugin.getStorage().getActiveIpBan(ip);
            }
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Nao foi possivel verificar os banimentos no armazenamento; conexao recusada por seguranca.", error);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    "Nao foi possivel verificar sua punicao no momento. Tente novamente mais tarde.");
            return;
        }

        if (ban != null) {
            // Verifica se a punição já expirou
            if (ban.isExpired()) {
                // Punição expirou, permite a entrada (o status deve ser atualizado no banco em outro momento ou lazy-load)
                return;
            }

            String layoutKey = "layouts.ban";
            String duration = "Permanente";
            PunishmentType type = ban.getType();

            if (type == PunishmentType.BAN_TEMPORARY) {
                layoutKey = "layouts.tempban";
                duration = TimeUtil.formatDuration(ban.getExpires() - System.currentTimeMillis());
            } else if (type == PunishmentType.BAN_IP_PERMANENT) {
                layoutKey = "layouts.ipban";
            } else if (type == PunishmentType.BAN_IP_TEMPORARY) {
                layoutKey = "layouts.tempipban";
                duration = TimeUtil.formatDuration(ban.getExpires() - System.currentTimeMillis());
            }

            // Garante que o layout existe, se não usa um fallback genérico
            String layout = plugin.getMessageUtil().getLayout(layoutKey);
            if (layout == null || layout.isEmpty()) {
                layout = "§cVoce esta banido do servidor.\n§7Motivo: " + ban.getReason();
            } else {
                layout = layout
                        .replace("%reason%", ban.getReason())
                        .replace("%staff%", ban.getStaffName())
                        .replace("%expires%", duration)
                        .replace("%duration%", duration);
            }

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, layout);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.isLicensed()) return;
        Punishment mute;
        try {
            mute = plugin.getStorage().getActiveMute(event.getPlayer().getUniqueId());
            if (mute == null) mute = plugin.getStorage().getActiveMuteByName(event.getPlayer().getName());
            if (mute == null) {
                String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
                mute = plugin.getStorage().getActiveIpMute(ip);
            }
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "Nao foi possivel verificar os mutes no armazenamento; mensagem bloqueada por seguranca.", error);
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNao foi possivel verificar sua punicao no momento. Tente novamente.");
            return;
        }

        if (mute != null) {
            if (mute.isExpired()) {
                return;
            }

            event.setCancelled(true);

            String duration = "Permanente";
            if (mute.getExpires() > 0) {
                duration = TimeUtil.formatDuration(mute.getExpires() - System.currentTimeMillis());
            }

            // Usa a chave correta 'target.muted' do messages.yml
            event.getPlayer().sendMessage(plugin.getMessageUtil().get("target.muted")
                    .replace("%reason%", mute.getReason())
                    .replace("%duration%", duration)
                    .replace("%expires%", duration)
                    .replace("%staff%", mute.getStaffName())
            );
        }
    }
}
