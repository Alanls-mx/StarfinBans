package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;

public class IpBanCommand extends AbstractCommand {

    public IpBanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "ipban", "starfinbans.ipban");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            noPerm(sender);
            return;
        }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.ipban"));
            return;
        }

        String target = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String ip = target;
        String name = target;

        ProxiedPlayer online = CmdUtil.online(target);
        if (online != null) {
            ip = online.getAddress().getAddress().getHostAddress();
            name = online.getName();
        } else if (!CmdUtil.looksLikeIp(target)) {
            msg(sender, plugin.msg().get("general.ip-required"));
            return;
        }

        plugin.getPunishmentManager().punish(
                online != null ? online.getUniqueId() : null,
                name,
                ip,
                null,
                sender.getName(),
                PunishmentType.BAN_IP_PERMANENT,
                reason,
                null
        );

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            String pip = p.getAddress().getAddress().getHostAddress();
            if (ip.equals(pip)) {
                String kickMsg = plugin.msg().getLayout("layouts.ipban")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName())
                        .replace("%expires%", "Nunca");
                p.disconnect(TextUtil.tc(kickMsg));
            }
        }

        msg(sender, plugin.msg().get("notification.ipban").replace("%player%", name));
    }
}
