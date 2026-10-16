package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class MuteCommand extends AbstractCommand {

    public MuteCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "mute", "starfinbans.mute");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.mute"));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

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
                PunishmentType.MUTE_PERMANENT,
                reason,
                null
        );

        if (online != null) {
            String muted = plugin.msg().get("target.muted")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName())
                    .replace("%duration%", "Permanente")
                    .replace("%expires%", "Permanente");
            online.sendMessage(TextUtil.tc(muted));
        }

        msg(sender, plugin.msg().get("notification.mute").replace("%player%", targetName));

        if (plugin.getCfg().getBoolean("options.broadcast-mutes", false)) {
            String bc = plugin.msg().getLayout("broadcast.mute")
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
