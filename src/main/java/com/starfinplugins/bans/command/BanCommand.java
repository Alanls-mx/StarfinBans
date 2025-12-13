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

public class BanCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public BanCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !plugin.getVaultHook().hasPermission((Player)sender, "starfinbans.ban")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.ban"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found")
                    .replace("%player%", args[0]));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String targetIp = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : null;

        plugin.getPunishmentAPI().punish(
                target.getUniqueId(), args[0], targetIp,
                (sender instanceof Player ? ((Player)sender).getUniqueId() : null),
                sender.getName(), PunishmentType.BAN_PERMANENT, reason, null
        );

        if (target.isOnline()) {
            String kickMsg = plugin.getMessageUtil().getLayout("layouts.ban")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName());
            ((Player)target).kickPlayer(kickMsg);
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.ban")
                .replace("%player%", args[0]));

        if (plugin.getConfig().getBoolean("options.broadcast-bans")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.ban", args[0], sender.getName(), reason, null);
        }

        return true;
    }
}