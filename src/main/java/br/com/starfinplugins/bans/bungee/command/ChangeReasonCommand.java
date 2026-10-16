package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import java.util.Arrays;
import java.util.Locale;

public class ChangeReasonCommand extends AbstractCommand {
    public ChangeReasonCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "changereason", "starfinbans.changereason");
    }

    @Override
    protected void executeAuthorized(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }
        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.changereason"));
            return;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        if (!id.matches("[0-9a-f]{8}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            msg(sender, "§cUse o ID de 8 caracteres mostrado em /history ou o UUID completo.");
            return;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty() || reason.length() > 500) {
            msg(sender, "§cO motivo deve ter entre 1 e 500 caracteres.");
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                if (!plugin.isLicensed()) return;
                int changed = plugin.getStorage().updateReason(id, reason);
                if (changed == 1)
                    msg(sender, plugin.msg().get("notification.changereason")
                        .replace("%id%", id).replace("%reason%", reason));
                else if (changed == -1) msg(sender, "§cID ambiguo. Use o UUID completo da punicao.");
                else msg(sender, "§cPunicao nao encontrada para esse ID.");
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Falha ao alterar motivo: " + error.getMessage());
                msg(sender, "§cFalha ao alterar o motivo no banco de dados.");
            }
        });
    }
}
