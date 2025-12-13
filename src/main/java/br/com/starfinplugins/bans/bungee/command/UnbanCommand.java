package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;

import net.md_5.bungee.api.CommandSender;

public class UnbanCommand extends AbstractCommand {

    public UnbanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "unban", "starfinbans.unban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 1) {
            msg(sender, plugin.msg().get("general.usage.unban"));
            return;
        }

        String targetName = args[0];
        var online = CmdUtil.online(targetName);

        if (online != null) {
            plugin.getPunishmentManager().unban(online.getUniqueId(), null, sender.getName());
        } else {
            plugin.getPunishmentManager().unbanByName(targetName, null, sender.getName());
        }

        msg(sender, plugin.msg().get("notification.unban")
                .replace("%player%", targetName)
                .replace("%staff%", sender.getName()));
    }
}
