package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

// ⚠️ IMPORT DO CORE (não use bungee.model)
import br.com.starfinplugins.bans.bungee.model.PunishmentType;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;

public class IpMuteCommand extends AbstractCommand {

    public IpMuteCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "ipmute", "starfinbans.ipmute");
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.ipmute"));
            return;
        }

        final String targetInput = args[0];
        final String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        ProxiedPlayer online = CmdUtil.online(targetInput);

        String ip;
        String name;

        if (online != null) {
            ip = online.getAddress().getAddress().getHostAddress();
            name = online.getName();
        } else {
            // se não é player online, precisa ser IP válido
            if (!CmdUtil.looksLikeIp(targetInput)) {
                msg(sender, plugin.msg().get("general.ip-required"));
                return;
            }
            ip = targetInput;
            name = targetInput;
        }

        plugin.getPunishmentManager().punish(
                online != null ? online.getUniqueId() : null,
                name,
                ip,
                null,
                sender.getName(),
                PunishmentType.MUTE_IP_PERMANENT,
                reason,
                null
        );

        // Mensagem para todos que estiverem no mesmo IP (opcional)
        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            String pip = p.getAddress().getAddress().getHostAddress();
            if (ip.equals(pip)) {
                String muted = plugin.msg().get("target.muted")
                        .replace("%reason%", reason)
                        .replace("%staff%", sender.getName());
                p.sendMessage(TextUtil.tc(muted));
            }
        }

        msg(sender, plugin.msg().get("notification.ipmute")
                .replace("%player%", name)
                .replace("%staff%", sender.getName()));

        if (plugin.getCfg().getBoolean("options.broadcast-mutes", false)) {
            String bc = plugin.msg().getLayout("broadcast.ipmute")
                    .replace("%player%", name + " (" + ip + ")")
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason);
            ProxyServer.getInstance().broadcast(TextUtil.tc(bc));
        }
    }
}
