package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.PunishmentType;
import com.starfinplugins.bans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;

public class TempIpBanCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public TempIpBanCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.tempipban")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.tempipban"));
            return true;
        }

        String target = args[0];
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.getMessageUtil().get("general.invalid-time"));
            return true;
        }

        String ip = target;
        String playerName = target;
        Player p = Bukkit.getPlayer(target);
        OfflinePlayer offlineTarget = null;

        if (p != null) {
            ip = p.getAddress().getAddress().getHostAddress();
            playerName = p.getName();
            offlineTarget = p;
        } else if (!target.contains(".")) {
            offlineTarget = Bukkit.getOfflinePlayer(target);
            if (!offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found")
                        .replace("%player%", target));
                return true;
            }
            sender.sendMessage(plugin.getMessageUtil().get("general.ip-required"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Punição do IP
        plugin.getPunishmentAPI().punish(
                p != null ? p.getUniqueId() : null,
                playerName, ip,
                (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                sender.getName(),
                PunishmentType.BAN_IP_TEMPORARY,
                reason, duration
        );

        // Punição do Jogador (Nick) no IP
        if (offlineTarget != null) {
            plugin.getPunishmentAPI().punish(
                    offlineTarget.getUniqueId(),
                    playerName,
                    ip,
                    (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                    sender.getName(),
                    PunishmentType.BAN_TEMPORARY,
                    "IP Ban: " + reason,
                    duration
            );
        }

        // Kick online players com esse IP
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(ip)) {
                String kickMsg = plugin.getMessageUtil().getLayout("layouts.tempipban")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName())
                        .replace("%expires%", TimeUtil.formatDuration(duration));
                online.kickPlayer(kickMsg);
            }
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.tempipban")
                .replace("%player%", playerName).replace("%duration%", args[1]));

        if (plugin.getConfig().getBoolean("options.broadcast-bans")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.tempipban", playerName + " (" + ip + ")", sender.getName(), reason, args[1]);
        }
        return true;
    }
}