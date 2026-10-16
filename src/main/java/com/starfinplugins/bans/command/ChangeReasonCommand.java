package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.Locale;

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
        String id = args[0].toLowerCase(Locale.ROOT);
        if (!id.matches("[0-9a-f]{8}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            sender.sendMessage("§cUse o ID de 8 caracteres mostrado em /history ou o UUID completo.");
            return true;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty() || reason.length() > 500) {
            sender.sendMessage("§cO motivo deve ter entre 1 e 500 caracteres.");
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!plugin.isLicensed()) return;
                int changed = plugin.getStorage().updateReason(id, reason);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (changed == 1)
                        sender.sendMessage(plugin.getMessageUtil().get("notification.changereason")
                            .replace("%id%", id).replace("%reason%", reason));
                    else if (changed == -1)
                        sender.sendMessage("§cID ambiguo. Use o UUID completo da punicao.");
                    else sender.sendMessage("§cPunicao nao encontrada para esse ID.");
                });
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Falha ao alterar motivo: " + error.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFalha ao alterar o motivo no banco de dados."));
            }
        });
        return true;
    }
}
