package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class KickCommand extends AbstractCommand {

    public KickCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "kick", "starfinbans.kick");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.kick"));
            return;
        }

        String targetName = args[0];
        var target = CmdUtil.online(targetName);

        if (target == null) {
            msg(sender, plugin.msg().get("general.player-not-found").replace("%player%", targetName));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Register kick as punishment (optional)
        plugin.getPunishmentManager().punish(
                target.getUniqueId(),
                target.getName(),
                target.getAddress().getAddress().getHostAddress(),
                null,
                sender.getName(),
                PunishmentType.KICK,
                reason,
                null
        );

        String kickMsg = plugin.msg().getLayout("layouts.kick")
                .replace("%reason%", reason)
                .replace("%staff%", sender.getName());

        target.disconnect(TextUtil.tc(kickMsg));

        msg(sender, plugin.msg().get("notification.kick").replace("%player%", targetName));

        if (plugin.getCfg().getBoolean("options.broadcast-kicks", true)) {
            String bc = plugin.msg().getLayout("broadcast.kick")
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
