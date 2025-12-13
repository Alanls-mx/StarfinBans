package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;
import java.util.UUID;

public class ChangeReasonCommand extends AbstractCommand {

    public ChangeReasonCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "changereason", "starfinbans.changereason");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        if (args.length < 2) {
            msg(sender, plugin.msg().get("general.usage.changereason"));
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            msg(sender, "§cID de punição inválido. Use o UUID completo exibido no /history.");
            return;
        }

        String newReason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!plugin.getStorage().updateReason(id, newReason)) {
            msg(sender, "§cNenhuma punição ativa encontrada com esse ID.");
            return;
        }

        msg(sender, plugin.msg().get("notification.changereason")
                .replace("%id%", id.toString())
                .replace("%reason%", newReason));

        ProxyServer.getInstance().getConsole().sendMessage(plugin.msg().get("notification.changereason")
                .replace("%id%", id.toString())
                .replace("%reason%", newReason));
    }
}
