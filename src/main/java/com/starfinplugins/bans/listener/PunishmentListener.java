package com.starfinplugins.bans.listener;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import com.starfinplugins.bans.util.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PunishmentListener implements Listener {

    private final StarfinBansPlugin plugin;

    public PunishmentListener(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        // 1. Verifica Ban por UUID (Banimento direto no jogador)
        Punishment ban = plugin.getStorage().getActiveBan(event.getUniqueId());

        // 2. Se não banido por UUID, verifica por IP (Banimento de IP)
        if (ban == null) {
            String ip = event.getAddress().getHostAddress();
            ban = plugin.getStorage().getActiveIpBan(ip);
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
                layout = "§cVocê está banido do servidor.\n§7Motivo: " + ban.getReason();
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
        if (handleChatGuardian(event)) {
            return;
        }

        // 1. Verifica Mute por UUID
        Punishment mute = plugin.getStorage().getActiveMute(event.getPlayer().getUniqueId());

        // 2. Se não mutado por UUID, verifica por IP
        if (mute == null) {
            String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
            mute = plugin.getStorage().getActiveIpMute(ip);
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

    private boolean handleChatGuardian(AsyncPlayerChatEvent event) {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg == null || !cfg.getBoolean("chat-guardian.enabled", false)) {
            return false;
        }

        String message = event.getMessage();
        if (message == null) {
            return false;
        }

        String lower = message.toLowerCase();
        boolean matched = false;
        for (String phrase : cfg.getStringList("chat-guardian.blocked-phrases")) {
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

        String staffName = cfg.getString("chat-guardian.staff-name", "StarfinAI");
        String reason = cfg.getString("chat-guardian.punishment.reason", "Linguagem proibida no chat");
        String typeKey = cfg.getString("chat-guardian.punishment.type", "tempmute");
        String durationStr = cfg.getString("chat-guardian.punishment.duration", "30m");
        long durationMs = TimeUtil.parseDuration(durationStr);
        Long duration = durationMs > 0 ? durationMs : null;

        PunishmentType type = resolvePunishmentType(typeKey);

        plugin.getPunishmentAPI().punish(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                event.getPlayer().getAddress().getAddress().getHostAddress(),
                null,
                staffName,
                type,
                reason,
                duration
        );

        String durationText = duration != null ? TimeUtil.formatDuration(duration) : "Permanente";
        event.getPlayer().sendMessage(plugin.getMessageUtil().get("chat-guardian.blocked"));
        event.getPlayer().sendMessage(plugin.getMessageUtil().get("chat-guardian.punished")
                .replace("%reason%", reason)
                .replace("%duration%", durationText)
        );
        return true;
    }

    private PunishmentType resolvePunishmentType(String raw) {
        if (raw == null) {
            return PunishmentType.MUTE_TEMPORARY;
        }
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