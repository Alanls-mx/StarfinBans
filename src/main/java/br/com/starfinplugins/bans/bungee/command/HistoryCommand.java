package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.List;

public class HistoryCommand extends AbstractCommand {

    public HistoryCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "history", "starfinbans.history");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 1) {
            msg(sender, plugin.msg().get("general.usage.history"));
            return;
        }

        String targetName = args[0];

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            List<Punishment> history = plugin.getStorage().getHistoryByName(targetName);

            if (history.isEmpty()) {
                sender.sendMessage(TextUtil.tc(plugin.msg().get("history.empty")));
                return;
            }

            sender.sendMessage(TextUtil.tc(plugin.msg().get("history.header").replace("%player%", targetName)));

            String format = plugin.msg().getRaw("history.format");

            for (Punishment p : history) {
                long timeDiff = System.currentTimeMillis() - p.getCreatedAt();
                String timeAgo = TimeUtil.formatDuration(timeDiff) + " atrás";

                String status = p.isActive() ? "§a(Ativo)" : "§c(Inativo)";
                if (p.isExpired()) status = "§e(Expirado)";

                sender.sendMessage(TextUtil.tc(format
                        .replace("%id%", p.getId().toString())
                        .replace("%type%", p.getType().name())
                        .replace("%reason%", safe(p.getReason()))
                        .replace("%active%", status)
                        .replace("%time_ago%", timeAgo)
                ));
            }
        });
    }

    private String safe(String s) { return s == null ? "" : s; }
}
