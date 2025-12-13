package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class WarnCommand extends AbstractCommand {

    public WarnCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "warn", "starfinbans.warn");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.warn"));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        var online = CmdUtil.online(targetName);

        boolean allowOffline = plugin.getCfg().getBoolean("options.allow-offline-punish", true);
        if (online == null && !allowOffline) {
            msg(sender, plugin.msg().get("general.player-not-found").replace("%player%", targetName));
            return;
        }

        plugin.getPunishmentManager().punish(
                online != null ? online.getUniqueId() : null,
                targetName,
                online != null ? online.getAddress().getAddress().getHostAddress() : null,
                null,
                sender.getName(),
                PunishmentType.WARN,
                reason,
                null
        );

        msg(sender, plugin.msg().get("notification.warn").replace("%player%", targetName));

        if (online != null) {
            String warn = plugin.msg().getLayout("target.warned")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName());
            online.sendMessage(TextUtil.tc(warn));
        }

        if (plugin.getCfg().getBoolean("options.broadcast-warns", true)) {
            String bc = plugin.msg().getLayout("broadcast.warn")
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
