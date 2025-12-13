package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnwarnCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public UnwarnCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !plugin.getVaultHook().hasPermission((Player)sender, "starfinbans.unwarn")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        // A implementação do Unwarn depende de como o sistema de Warns funciona (por ID ou remove o último)
        // Aqui estou apenas corrigindo o erro de compilação da classe principal e a permissão.

        sender.sendMessage("§cComando ainda não implementado completamente.");
        return true;
    }
}