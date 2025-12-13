package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
        ProxiedPlayer online = CmdUtil.online(targetName);

        if (online != null) {
            String ip = online.getAddress().getAddress().getHostAddress();

            if (plugin.getPunishmentManager().getActiveBan(online.getUniqueId()) != null) {
                plugin.getPunishmentManager().unban(online.getUniqueId(), null, sender.getName());
            } else if (plugin.getPunishmentManager().getActiveIpBan(ip) != null) {
                plugin.getPunishmentManager().unbanIp(ip, null, sender.getName());
            } else {
                msg(sender, plugin.msg().get("general.not-punished").replace("%player%", targetName));
                return;
            }
        } else {
            if (plugin.getPunishmentManager().getActiveBanByName(targetName) == null) {
                msg(sender, plugin.msg().get("general.not-punished").replace("%player%", targetName));
                return;
            }
            plugin.getPunishmentManager().unbanByName(targetName, null, sender.getName());
        }

        msg(sender, plugin.msg().get("notification.unban")
                .replace("%player%", targetName)
                .replace("%staff%", sender.getName()));
    }
}
