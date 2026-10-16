package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public HelpCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.help")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        sender.sendMessage(plugin.getMessageUtil().get("help.header"));
        sendHelp(sender, "ban <player> <reason>", "Bane um jogador permanentemente.");
        sendHelp(sender, "tempban <player> <time> <reason>", "Bane um jogador temporariamente.");
        sendHelp(sender, "ipban <player/ip> <reason>", "Bane um IP permanentemente.");
        sendHelp(sender, "tempipban <player/ip> <time> <reason>", "Bane um IP temporariamente.");
        sendHelp(sender, "mute <player> <reason>", "Muta um jogador permanentemente.");
        sendHelp(sender, "tempmute <player> <time> <reason>", "Muta um jogador temporariamente.");
        sendHelp(sender, "ipmute <player/ip> <reason>", "Muta um IP permanentemente.");
        sendHelp(sender, "tempipmute <player/ip> <time> <reason>", "Muta um IP temporariamente.");
        sendHelp(sender, "kick <player> <reason>", "Expulsa um jogador.");
        sendHelp(sender, "warn <player> <reason>", "Alerta um jogador.");
        sendHelp(sender, "unban/unmute <player>", "Remove punicoes.");
        sendHelp(sender, "unwarn <player>", "Remove alertas ativos.");
        sendHelp(sender, "check <ban/mute> <player>", "Verifica punicoes ativas.");
        sendHelp(sender, "history <player>", "Ve o historico de punicoes.");
        sendHelp(sender, "changereason <id> <reason>", "Altera o motivo de uma punicao.");

        return true;
    }

    private void sendHelp(CommandSender sender, String cmd, String desc) {
        sender.sendMessage(plugin.getMessageUtil().getRaw("help.format")
                .replace("%command%", cmd)
                .replace("%description%", desc));
    }
}
