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

public class TempBanCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public TempBanCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.tempban")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.tempban"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found")
                    .replace("%player%", args[0]));
            return true;
        }

        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.getMessageUtil().get("general.invalid-time"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String targetIp = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : null;

        plugin.getPunishmentAPI().punish(
                target.getUniqueId(), args[0], targetIp,
                (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                sender.getName(), PunishmentType.BAN_TEMPORARY, reason, duration
        );

        if (target.isOnline()) {
            String kickMsg = plugin.getMessageUtil().getLayout("layouts.tempban")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName())
                    .replace("%expires%", TimeUtil.formatDuration(duration));
            ((Player)target).kickPlayer(kickMsg);
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.tempban")
                .replace("%player%", args[0]).replace("%duration%", args[1]));

        if (plugin.getConfig().getBoolean("options.broadcast-bans")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.tempban", args[0], sender.getName(), reason, args[1]);
        }
        return true;
    }
}