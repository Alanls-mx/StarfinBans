package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public abstract class AbstractCommand extends Command {

    protected final StarfinBansBungeePlugin plugin;

    protected AbstractCommand(StarfinBansBungeePlugin plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    protected void msg(CommandSender sender, String message) {
        sender.sendMessage(TextUtil.tc(message));
    }

    protected boolean noPerm(CommandSender sender) {
        msg(sender, plugin.msg().get("permissions.no-permission-general"));
        return true;
    }
}
