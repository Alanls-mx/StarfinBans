package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.storage.DupeLookupStorage;
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
            String ip = online.getAddress().getAddress().getHostAddress();

            if (plugin.getPunishmentManager().getActiveMute(online.getUniqueId()) != null) {
                plugin.getPunishmentManager().unmute(online.getUniqueId(), null, sender.getName());
            } else if (plugin.getPunishmentManager().getActiveIpMute(ip) != null) {
                plugin.getPunishmentManager().unmuteIp(ip, null, sender.getName());
            } else {
                msg(sender, plugin.msg().get("general.not-punished").replace("%player%", targetName));
                return;
            }
        } else {
            if (plugin.getPunishmentManager().getActiveMuteByName(targetName) != null) {
                plugin.getPunishmentManager().unmuteByName(targetName, null, sender.getName());
            } else {
                String lastIp = getLastIpByName(targetName);
                if (lastIp != null && plugin.getPunishmentManager().getActiveIpMute(lastIp) != null) {
                    plugin.getPunishmentManager().unmuteIp(lastIp, null, sender.getName());
                } else {
                    msg(sender, plugin.msg().get("general.not-punished").replace("%player%", targetName));
                    return;
                }
            }
        }

        msg(sender, plugin.msg().get("notification.unmute")
                .replace("%player%", targetName)
                .replace("%staff%", sender.getName()));
    }

    private String getLastIpByName(String targetName) {
        if (plugin.getStorage() instanceof DupeLookupStorage) {
            return ((DupeLookupStorage) plugin.getStorage()).getLastIpByName(targetName);
        }
        return null;
    }
}
