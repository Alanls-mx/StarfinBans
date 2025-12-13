package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.storage.DupeLookupStorage;
import br.com.starfinplugins.bans.bungee.util.CmdUtil;
import br.com.starfinplugins.bans.bungee.util.TextUtil;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.List;

public class DupeIpCommand extends AbstractCommand {

    public DupeIpCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "dupeip", "starfinbans.dupeip");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 1) {
            msg(sender, plugin.msg().get("general.usage.dupeip"));
            return;
        }

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            String input = args[0];
            String ip = input;

            // ✅ storage precisa suportar dupe lookup
            if (!(plugin.getStorage() instanceof DupeLookupStorage)) {
                sender.sendMessage(TextUtil.tc("§c[StarfinBans] Storage atual não suporta /dupeip (DupeLookupStorage)."));
                return;
            }

            DupeLookupStorage dupeStorage = (DupeLookupStorage) plugin.getStorage();

            var p = CmdUtil.online(input);
            if (p != null) {
                ip = p.getAddress().getAddress().getHostAddress();
            } else if (!CmdUtil.looksLikeIp(ip)) {
                ip = dupeStorage.getLastIpByName(input);
                if (ip == null) {
                    sender.sendMessage(TextUtil.tc(plugin.msg().get("general.player-not-found").replace("%player%", input)));
                    return;
                }
            }

            sender.sendMessage(TextUtil.tc(plugin.msg().get("dupeip.scanning")));

            List<String> alts = dupeStorage.getAlts(ip);

            sender.sendMessage(TextUtil.tc(plugin.msg().getRaw("dupeip.header").replace("%ip%", ip)));

            if (alts.isEmpty()) {
                sender.sendMessage(TextUtil.tc("§7Nenhuma conta associada encontrada."));
                return;
            }

            for (String alt : alts) {
                var altP = CmdUtil.online(alt);
                boolean onlineAlt = altP != null;
                String status = onlineAlt ? "§aOnline" : "§cOffline";

                if (onlineAlt && plugin.getPunishmentManager().isBanned(altP.getUniqueId())) {
                    status += " §4[BANIDO]";
                }

                sender.sendMessage(TextUtil.tc(plugin.msg().getRaw("dupeip.format")
                        .replace("%player%", alt)
                        .replace("%status%", status)));
            }
        });
    }
}
