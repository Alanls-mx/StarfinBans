package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CheckCommand implements CommandExecutor {

    private final StarfinBansPlugin plugin;
    private final String type; // "ban" ou "mute"

    public CheckCommand(StarfinBansPlugin plugin, String type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.check" + type)) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.check" + type));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            Punishment active;

            if (type.equals("ban")) {
                active = plugin.getPunishmentAPI().isBanned(target.getUniqueId())
                        ? plugin.getStorage().getActiveBan(target.getUniqueId())
                        : null;
            } else {
                active = plugin.getPunishmentAPI().isMuted(target.getUniqueId())
                        ? plugin.getStorage().getActiveMute(target.getUniqueId())
                        : null;
            }

            if (active == null) {
                sender.sendMessage(plugin.getMessageUtil().get("general.not-punished")
                        .replace("%player%", args[0]));
            } else {
                String duration = active.getExpires() > 0
                        ? TimeUtil.formatDuration(active.getExpires() - System.currentTimeMillis())
                        : "Permanente";

                sender.sendMessage(plugin.getMessageUtil().getRaw("check.format")
                        .replace("%player%", active.getPlayerName())
                        .replace("%type%", type.toUpperCase())
                        .replace("%reason%", active.getReason())
                        .replace("%staff%", active.getStaffName())
                        .replace("%duration%", duration)
                        .replace("%date%", TimeUtil.formatDate(active.getCreatedAt()))
                );
            }
        });

        return true;
    }
}