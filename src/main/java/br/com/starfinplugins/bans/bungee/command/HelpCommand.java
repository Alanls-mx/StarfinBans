package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;

import net.md_5.bungee.api.CommandSender;

public class HelpCommand extends AbstractCommand {

    public HelpCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "help", "starfinbans.help");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        msg(sender, plugin.msg().get("help.header"));
        send(sender, "ban <player> <reason>", "Bane um jogador permanentemente.");
        send(sender, "tempban <player> <time> <reason>", "Bane um jogador temporariamente.");
        send(sender, "ipban <player/ip> <reason>", "Bane um IP permanentemente.");
        send(sender, "tempipban <player/ip> <time> <reason>", "Bane um IP temporariamente.");
        send(sender, "mute <player> <reason>", "Muta um jogador permanentemente.");
        send(sender, "tempmute <player> <time> <reason>", "Muta um jogador temporariamente.");
        send(sender, "ipmute <player/ip> <reason>", "Muta um IP permanentemente.");
        send(sender, "tempipmute <player/ip> <time> <reason>", "Muta um IP temporariamente.");
        send(sender, "kick <player> <reason>", "Expulsa um jogador.");
        send(sender, "warn <player> <reason>", "Alerta um jogador.");
        send(sender, "unban/unmute <player>", "Remove punicoes.");
        send(sender, "checkban/checkmute <player>", "Verifica punicoes ativas.");
        send(sender, "history <player>", "Ve o historico por nome.");
        send(sender, "dupeip <player/ip>", "Ve contas no mesmo IP.");
        send(sender, "starfinbans <reload|lockdown|broadcast|info>", "Administracao.");
    }

    private void send(CommandSender sender, String cmd, String desc) {
        msg(sender, plugin.msg().getRaw("help.format")
                .replace("%command%", cmd)
                .replace("%description%", desc));
    }
}
