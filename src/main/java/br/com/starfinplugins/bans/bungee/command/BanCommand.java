package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;

public class BanCommand extends AbstractCommand {

    public BanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "ban", "starfinbans.ban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.ban"));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        ProxiedPlayer online = CmdUtil.online(targetName);
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
                PunishmentType.BAN_PERMANENT,
                reason,
                null
        );

        if (online != null) {
            String kickMsg = plugin.msg().getLayout("layouts.ban")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName());
            online.disconnect(TextUtil.tc(kickMsg));
        }

        msg(sender, plugin.msg().get("notification.ban").replace("%player%", targetName));

        if (plugin.getCfg().getBoolean("options.broadcast-bans", true)) {
            String bc = plugin.msg().getLayout("broadcast.ban")
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
