package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class TempBanCommand extends AbstractCommand {

    public TempBanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "tempban", "starfinbans.tempban");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 3) {
            msg(sender, plugin.msg().get("general.usage.tempban"));
            return;
        }

        String targetName = args[0];
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) { msg(sender, plugin.msg().get("general.invalid-time")); return; }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        net.md_5.bungee.api.connection.ProxiedPlayer online = CmdUtil.online(targetName);
        String ip = online != null ? online.getAddress().getAddress().getHostAddress() : null;

        boolean allowOffline = plugin.getCfg().getBoolean("options.allow-offline-punish", true);
        if (online == null && !allowOffline) {
            msg(sender, plugin.msg().get("general.player-not-found").replace("%player%", targetName));
            return;
        }

        plugin.getPunishmentManager().punish(
                online != null ? online.getUniqueId() : null,
                targetName,
                ip,
                null,
                sender.getName(),
                PunishmentType.BAN_TEMPORARY,
                reason,
                duration
        );

        if (online != null) {
            String kickMsg = plugin.msg().getLayout("layouts.tempban")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName())
                    .replace("%expires%", TimeUtil.formatDuration(duration));
            online.disconnect(TextUtil.tc(kickMsg));
        }

        msg(sender, plugin.msg().get("notification.tempban")
                .replace("%player%", targetName)
                .replace("%duration%", args[1]));

        if (plugin.getCfg().getBoolean("options.broadcast-bans", true)) {
            String bc = plugin.msg().getLayout("broadcast.tempban")
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason)
                    .replace("%duration%", args[1]);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
