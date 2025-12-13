package br.com.starfinplugins.bans.bungee.listener;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;

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

        Punishment ban = uuid != null ? plugin.getStorage().getActiveBan(uuid) : null;
        if (ban == null) ban = plugin.getStorage().getActiveBanByName(name);
        if (ban == null) ban = plugin.getStorage().getActiveIpBan(ip);

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

        if (handleChatGuardian(event, p)) {
            return;
        }

        Punishment mute = plugin.getStorage().getActiveMute(uuid);
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

    private boolean handleChatGuardian(ChatEvent event, ProxiedPlayer player) {
        if (!plugin.getCfg().getBoolean("chat-guardian.enabled", false)) {
            return false;
        }

        String message = event.getMessage();
        if (message == null) {
            return false;
        }

        String lower = message.toLowerCase();
        boolean matched = false;
        for (String phrase : plugin.getCfg().getStringList("chat-guardian.blocked-phrases")) {
            if (phrase == null || phrase.trim().isEmpty()) continue;
            if (lower.contains(phrase.toLowerCase())) {
                matched = true;
                break;
            }
        }

        if (!matched) {
            return false;
        }

        event.setCancelled(true);

        String staffName = plugin.getCfg().getString("chat-guardian.staff-name", "StarfinAI");
        String reason = plugin.getCfg().getString("chat-guardian.punishment.reason", "Linguagem proibida no chat");
        String typeKey = plugin.getCfg().getString("chat-guardian.punishment.type", "tempmute");
        String durationStr = plugin.getCfg().getString("chat-guardian.punishment.duration", "30m");
        long durationMs = TimeUtil.parseDuration(durationStr);
        Long duration = durationMs > 0 ? durationMs : null;

        PunishmentType type = resolvePunishmentType(typeKey);

        plugin.getPunishmentManager().punish(
                player.getUniqueId(),
                player.getName(),
                player.getAddress().getAddress().getHostAddress(),
                null,
                staffName,
                type,
                reason,
                duration
        );

        String durationText = duration != null ? TimeUtil.formatDuration(duration) : "Permanente";
        player.sendMessage(TextUtil.tc(plugin.msg().get("chat-guardian.blocked")));
        player.sendMessage(TextUtil.tc(plugin.msg().get("chat-guardian.punished")
                .replace("%reason%", reason)
                .replace("%duration%", durationText)
        ));

        return true;
    }

    private PunishmentType resolvePunishmentType(String raw) {
        if (raw == null) return PunishmentType.MUTE_TEMPORARY;

        String key = raw.toLowerCase();
        switch (key) {
            case "ban":
            case "permanentban":
                return PunishmentType.BAN_PERMANENT;
            case "tempban":
            case "ban_temp":
                return PunishmentType.BAN_TEMPORARY;
            case "mute":
            case "permanentmute":
                return PunishmentType.MUTE_PERMANENT;
            case "tempmute":
            case "mute_temp":
            default:
                return PunishmentType.MUTE_TEMPORARY;
        }
    }
}
