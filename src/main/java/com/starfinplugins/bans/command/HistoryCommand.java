package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.List;

public class HistoryCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public HistoryCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.history")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.history"));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            List<Punishment> history = plugin.getPunishmentAPI().getHistory(target.getUniqueId());

            if (history.isEmpty()) {
                sender.sendMessage(plugin.getMessageUtil().get("history.empty"));
                return;
            }

            sender.sendMessage(plugin.getMessageUtil().get("history.header").replace("%player%", args[0]));

            String lineFormat = plugin.getMessageUtil().getRaw("history.format");

            for (Punishment p : history) {
                long timeDiff = System.currentTimeMillis() - p.getCreatedAt();
                String timeAgo = TimeUtil.formatDuration(timeDiff) + " atras";

                String status = p.isActive() ? "§a(Ativo)" : "§c(Inativo)";
                if (p.isExpired()) status = "§e(Expirado)";

                sender.sendMessage(lineFormat
                        .replace("%id%", p.getId().toString().substring(0, 8))
                        .replace("%type%", p.getType().name())
                        .replace("%reason%", p.getReason())
                        .replace("%active%", status)
                        .replace("%time_ago%", timeAgo)
                );
            }
        });

        return true;
    }
}