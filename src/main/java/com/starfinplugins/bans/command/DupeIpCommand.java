package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class DupeIpCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public DupeIpCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("starfinbans.dupeip")) { // Permissão atualizada
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }
        if (args.length < 1) {
            // Garanta que esta chave existe no messages.yml ou use uma genérica
            String usage = plugin.getMessageUtil().get("general.usage.dupeip");
            if (usage.startsWith("Message not found")) usage = "/dupeip <jogador>";
            sender.sendMessage(usage);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String targetIp = args[0];
            Player targetP = Bukkit.getPlayer(args[0]);

            if (targetP != null) {
                targetIp = targetP.getAddress().getAddress().getHostAddress();
            } else if (!targetIp.contains(".")) {
                // Se não é IP e o jogador está offline, tentar buscar último IP no banco
                // getOfflinePlayer pode ser lento, mas estamos numa task async, então ok
                targetIp = plugin.getStorage().getLastIp(Bukkit.getOfflinePlayer(args[0]).getUniqueId());
                if (targetIp == null) {
                    sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found"));
                    return;
                }
            }

            sender.sendMessage(plugin.getMessageUtil().get("dupeip.scanning"));

            List<String> alts = plugin.getStorage().getAlts(targetIp);

            sender.sendMessage(plugin.getMessageUtil().getRaw("dupeip.header").replace("%ip%", targetIp));

            if (alts.isEmpty()) {
                sender.sendMessage("§7Nenhuma conta associada encontrada.");
            } else {
                for (String alt : alts) {
                    Player altP = Bukkit.getPlayer(alt);
                    boolean online = altP != null;
                    String status = online ? "§aOnline" : "§cOffline";

                    // Verifica banimento (apenas se online para evitar lag de UUID fetch se não tiver cache)
                    if (online && plugin.getPunishmentAPI().isBanned(altP.getUniqueId())) {
                        status += " §4[BANIDO]";
                    }

                    sender.sendMessage(plugin.getMessageUtil().getRaw("dupeip.format")
                            .replace("%player%", alt).replace("%status%", status));
                }
            }
        });
        return true;
    }
}