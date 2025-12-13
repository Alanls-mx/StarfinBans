package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class UnmuteCommand extends AbstractCommand {

    public UnmuteCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "unmute", "starfinbans.unmute");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            noPerm(sender);
            return;
        }

        if (args.length < 1) {
            msg(sender, plugin.msg().get("general.usage.unmute"));
            return;
        }

        String targetName = args[0];
        ProxiedPlayer online = CmdUtil.online(targetName);

        if (online != null) {
            plugin.getPunishmentManager().unmute(online.getUniqueId(), null, sender.getName());
        } else {
            plugin.getPunishmentManager().unmuteByName(targetName, null, sender.getName());
        }

        msg(sender, plugin.msg().get("notification.unmute")
                .replace("%player%", targetName)
                .replace("%staff%", sender.getName()));
    }
}
