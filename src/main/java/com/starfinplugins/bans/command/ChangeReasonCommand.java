package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.UUID;

public class ChangeReasonCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public ChangeReasonCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.changereason")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.changereason"));
            return true;
        }

        UUID id;
        try {
            id = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cID de punição inválido. Use o UUID completo exibido no /history.");
            return true;
        }

        String newReason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        boolean updated = plugin.getStorage().updateReason(id, newReason);

        if (!updated) {
            sender.sendMessage("§cNenhuma punição ativa encontrada com esse ID.");
            return true;
        }

        sender.sendMessage(plugin.getMessageUtil().get("notification.changereason")
                .replace("%id%", id.toString())
                .replace("%reason%", newReason));

        if (plugin.getConfig().getBoolean("options.broadcast-changes", false)) {
            plugin.getMessageUtil().sendBroadcast("broadcast.changereason", id.toString(), sender.getName(), newReason, null);
        }

        return true;
    }
}