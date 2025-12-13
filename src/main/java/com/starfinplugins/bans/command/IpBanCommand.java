package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;

public class IpBanCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public IpBanCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.ipban")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.ipban"));
            return true;
        }

        String target = args[0];
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

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Punish IP
        plugin.getPunishmentAPI().punish(
                p != null ? p.getUniqueId() : null,
                playerName, ip,
                (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                sender.getName(),
                PunishmentType.BAN_IP_PERMANENT,
                reason, null
        );

        // Punish Nickname
        if (offlineTarget != null) {
            plugin.getPunishmentAPI().punish(
                    offlineTarget.getUniqueId(),
                    playerName,
                    ip,
                    (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                    sender.getName(),
                    PunishmentType.BAN_PERMANENT,
                    "IP Ban: " + reason,
                    null
            );
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(ip)) {
                String kickMsg = plugin.getMessageUtil().getLayout("layouts.ipban")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName())
                        .replace("%expires%", "Nunca");
                online.kickPlayer(kickMsg);
            }
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.ipban")
                .replace("%player%", playerName).replace("%staff%", sender.getName()));

        if (plugin.getConfig().getBoolean("options.broadcast-bans")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.ipban", playerName + " (" + ip + ")", sender.getName(), reason, null);
        }
        return true;
    }
}