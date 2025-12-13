package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;

public class TempIpMuteCommand extends AbstractCommand {

    public TempIpMuteCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "tempipmute", "starfinbans.tempipmute");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 3) {
            msg(sender, plugin.msg().get("general.usage.tempipmute"));
            return;
        }

        String target = args[0];
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) { msg(sender, plugin.msg().get("general.invalid-time")); return; }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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
                PunishmentType.MUTE_IP_TEMPORARY,
                reason,
                duration
        );

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            String pip = p.getAddress().getAddress().getHostAddress();
            if (ip.equals(pip)) {
                String muted = plugin.msg().get("target.muted")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName())
                        .replace("%duration%", args[1])
                        .replace("%expires%", args[1]);
                p.sendMessage(TextUtil.tc(muted));
            }
        }

        msg(sender, plugin.msg().get("notification.tempipmute")
                .replace("%player%", name)
                .replace("%duration%", args[1]));

        if (plugin.getCfg().getBoolean("options.broadcast-mutes", false)) {
            String bc = plugin.msg().getLayout("broadcast.tempipmute")
                    .replace("%player%", name + " (" + ip + ")")
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason)
                    .replace("%duration%", args[1]);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
