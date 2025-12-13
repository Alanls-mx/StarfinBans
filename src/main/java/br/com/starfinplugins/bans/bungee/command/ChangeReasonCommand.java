package br.com.starfinplugins.bans.bungee.command;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import net.md_5.bungee.api.CommandSender;

public class ChangeReasonCommand extends AbstractCommand {

    public ChangeReasonCommand(StarfinBansBungeePlugin plugin) {
        super(plugin, "changereason", "starfinbans.changereason");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { noPerm(sender); return; }

        msg(sender, "§cFuncionalidade changereason não implementada (precisa de updateReason(UUID punishmentId, String reason) no Storage).");
    }
}
