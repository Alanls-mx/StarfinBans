package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;

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

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID invalido.");
            return true;
        }

        String newReason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // TODO: Implementar updateReason(int id, String reason) no PunishmentStorage
        // plugin.getStorage().updateReason(id, newReason);
        sender.sendMessage("§cFuncionalidade changereason requer implementacao no Storage (aguardando atualizacao).");

        /*
        // Código futuro quando Storage suportar:
        sender.sendMessage(plugin.getMessageUtil().get("notification.changereason")
                .replace("%id%", String.valueOf(id))
                .replace("%reason%", newReason));

        if (plugin.getConfig().getBoolean("options.broadcast-changes")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.changereason", String.valueOf(id), sender.getName(), newReason, null);
        }
        */

        return true;
    }
}