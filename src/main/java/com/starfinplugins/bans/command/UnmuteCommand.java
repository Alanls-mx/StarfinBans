package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnmuteCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public UnmuteCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !plugin.getVaultHook().hasPermission((Player)sender, "starfinbans.unmute")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.unmute"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!plugin.getPunishmentAPI().isMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageUtil().get("general.not-punished")
                    .replace("%player%", args[0]));
            return true;
        }

        plugin.getPunishmentAPI().unmute(target.getUniqueId(), (sender instanceof Player ? ((Player)sender).getUniqueId() : null), sender.getName());

        sender.sendMessage(plugin.getMessageUtil().get("notification.unmute")
                .replace("%player%", args[0])
                .replace("%staff%", sender.getName()));
        return true;
    }
}