package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;

public class KickCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public KickCommand(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.kick")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.kick"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String kickMsg = plugin.getMessageUtil().getLayout("layouts.kick")
                .replace("%reason%", reason)
                .replace("%staff%", sender.getName());
        target.kickPlayer(kickMsg);

        sender.sendMessage(plugin.getMessageUtil().get("notification.kick")
                .replace("%player%", args[0]));

        if (plugin.getConfig().getBoolean("options.broadcast-kicks")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.kick", args[0], sender.getName(), reason, null);
        }

        return true;
    }
}