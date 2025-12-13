package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;

public class AdminCommand extends AbstractCommand {

    public AdminCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "starfinbans", "starfinbans.admin", "sbans");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length == 0) {
            msg(sender, "§cUse: /starfinbans <reload|lockdown|broadcast|info>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload": {
                plugin.reloadAll();
                msg(sender, plugin.msg().get("admin.reload"));
                break;
            }
            case "lockdown": {
                boolean newState = !plugin.isLockdown();
                plugin.setLockdown(newState);

                String key = newState ? "admin.lockdown-enable" : "admin.lockdown-disable";
                ProxyServer.getInstance().broadcast(TextUtil.tc(plugin.msg().get(key)));
                break;
            }
            case "broadcast": {
                if (args.length < 2) {
                    msg(sender, "§cUse: /starfinbans broadcast <mensagem>");
                    return;
                }
                String bc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                ProxyServer.getInstance().broadcast(TextUtil.tc(plugin.msg().color(bc)));
                break;
            }
            case "info": {
                msg(sender, "§eStarfinBans (Bungee) v" + plugin.getDescription().getVersion());
                msg(sender, "§eStorage: §f" + plugin.getCfg().getString("storage-type", "sqlite"));
                msg(sender, "§eLicensed: §f" + plugin.isLicensed());
                break;
            }
            default:
                msg(sender, "§cSubcomando desconhecido.");
        }
    }
}
