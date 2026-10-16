package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

public class UnwarnCommand extends AbstractCommand {
    public UnwarnCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "unwarn", "starfinbans.unwarn");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }
        if (args.length != 1) {
            msg(sender, plugin.msg().get("general.usage.unwarn"));
            return;
        }
        String target = args[0];
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                if (!plugin.isLicensed()) return;
                boolean active = plugin.getStorage().getHistoryByName(target).stream()
                    .anyMatch(p -> p.getType() == PunishmentType.WARN && p.isActive());
                if (active) plugin.getStorage().expirePunishmentByName(target, PunishmentType.WARN,
                    null, sender.getName());
                msg(sender, active
                    ? plugin.msg().get("notification.unwarn").replace("%player%", target)
                    : "§eNenhum alerta ativo encontrado para " + target + ".");
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Falha ao remover alertas: " + error.getMessage());
                msg(sender, "§cFalha ao remover alertas no banco de dados.");
            }
        });
    }
}
