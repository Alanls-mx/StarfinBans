package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class TempIpBanCommand extends AbstractCommand {

    public TempIpBanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "tempipban", "starfinbans.tempipban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 3) {
            msg(sender, plugin.msg().get("general.usage.tempipban"));
            return;
        }

        String target = args[0];
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) { msg(sender, plugin.msg().get("general.invalid-time")); return; }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        String ip = target;
        String name = target;

        net.md_5.bungee.api.connection.ProxiedPlayer online = CmdUtil.online(target);
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
                PunishmentType.BAN_IP_TEMPORARY,
                reason,
                duration
        );

        // Kick all with same IP
        for (net.md_5.bungee.api.connection.ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            String pip = p.getAddress().getAddress().getHostAddress();
            if (ip.equals(pip)) {
                String kickMsg = plugin.msg().getLayout("layouts.tempipban")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName())
                        .replace("%expires%", TimeUtil.formatDuration(duration));
                p.disconnect(TextUtil.tc(kickMsg));
            }
        }

        msg(sender, plugin.msg().get("notification.tempipban")
                .replace("%player%", name)
                .replace("%duration%", args[1]));

        if (plugin.getCfg().getBoolean("options.broadcast-bans", true)) {
            String bc = plugin.msg().getLayout("broadcast.tempipban")
                    .replace("%player%", name + " (" + ip + ")")
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason)
                    .replace("%duration%", args[1]);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
