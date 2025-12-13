package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class AdminCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public AdminCommand(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.admin")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUse: /starfinbans <reload|lockdown|broadcast|info>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.reloadMessages();
                sender.sendMessage(plugin.getMessageUtil().get("admin.reload"));
                break;
            case "lockdown":
                boolean newState = !plugin.isLockdown();
                plugin.setLockdown(newState);
                String msg = newState ? "admin.lockdown-enable" : "admin.lockdown-disable";
                Bukkit.broadcastMessage(plugin.getMessageUtil().get(msg));
                break;
            case "broadcast":
                if (args.length < 2) return false;
                String bc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bc));
                break;
            case "info":
                sender.sendMessage("§eStarfinBans v" + plugin.getDescription().getVersion());
                break;
            default:
                sender.sendMessage("§cSubcomando desconhecido.");
        }
        return true;
    }
}