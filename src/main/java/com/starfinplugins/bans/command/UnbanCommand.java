package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class UnbanCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public UnbanCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !plugin.getVaultHook().hasPermission((Player)sender, "starfinbans.unban")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.unban"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String targetIp = null;

        if (target.isOnline()) {
            targetIp = target.getPlayer().getAddress().getAddress().getHostAddress();
        } else {
            targetIp = plugin.getPunishmentAPI().getLastIp(target.getUniqueId());
        }

        boolean hasBan = plugin.getPunishmentAPI().getActiveBan(target.getUniqueId()) != null;
        boolean hasIpBan = targetIp != null && plugin.getPunishmentAPI().getActiveIpBan(targetIp) != null;

        if (!hasBan && !hasIpBan) {
            sender.sendMessage(plugin.getMessageUtil().get("general.not-punished")
                    .replace("%player%", args[0]));
            return true;
        }

        UUID staffId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        if (hasBan) {
            plugin.getPunishmentAPI().unban(target.getUniqueId(), staffId, sender.getName());
        }
        if (hasIpBan) {
            plugin.getPunishmentAPI().unbanIp(targetIp, staffId, sender.getName());
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.unban")
                .replace("%player%", args[0])
                .replace("%staff%", sender.getName()));
        return true;
    }
}