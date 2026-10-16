package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnwarnCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public UnwarnCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.unwarn")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.unwarn"));
            return true;
        }
        String targetName = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!plugin.isLicensed()) return;
                boolean active = plugin.getStorage().getHistoryByName(targetName).stream()
                    .anyMatch(p -> p.getType() == PunishmentType.WARN && p.isActive());
                if (active) plugin.getStorage().expirePunishmentByName(targetName, PunishmentType.WARN,
                    null, sender.getName());
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(active
                    ? plugin.getMessageUtil().get("notification.unwarn").replace("%player%", targetName)
                    : "§eNenhum alerta ativo encontrado para " + targetName + "."));
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Falha ao remover alertas: " + error.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFalha ao remover alertas no banco de dados."));
            }
        });
        return true;
    }
}
