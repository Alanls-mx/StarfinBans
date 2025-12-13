package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

public class CheckBanCommand extends AbstractCommand {

    public CheckBanCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "checkban", "starfinbans.checkban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 1) {
            msg(sender, plugin.msg().get("general.usage.checkban"));
            return;
        }

        String targetName = args[0];

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            Punishment p = plugin.getStorage().getActiveBanByName(targetName);

            if (p == null || p.isExpired()) {
                sender.sendMessage(TextUtil.tc(plugin.msg().get("general.not-punished").replace("%player%", targetName)));
                return;
            }

            String duration = p.getExpires() > 0 ? TimeUtil.formatDuration(p.getExpires() - System.currentTimeMillis()) : "Permanente";

            String line = plugin.msg().getRaw("check.format")
                    .replace("%player%", targetName)
                    .replace("%type%", p.getType().name())
                    .replace("%reason%", safe(p.getReason()))
                    .replace("%staff%", safe(p.getStaffName()))
                    .replace("%duration%", duration);

            sender.sendMessage(TextUtil.tc(line));
        });
    }

    private String safe(String s) { return s == null ? "" : s; }
}
