package com.starfinplugins.bans.command;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.UUID;

public class WarnCommand implements CommandExecutor {
    private final StarfinBansPlugin plugin;

    public WarnCommand(StarfinBansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !plugin.getVaultHook().hasPermission((Player)sender, "starfinbans.warn")) {
            sender.sendMessage(plugin.getMessageUtil().get("permissions.no-permission-general"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("general.usage.warn"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessageUtil().get("general.player-not-found")
                    .replace("%player%", args[0]));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        UUID staffUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

        String targetIp = null;
        if (target.isOnline()) {
            targetIp = ((Player)target).getAddress().getAddress().getHostAddress();
        }

        plugin.getPunishmentAPI().punish(
                target.getUniqueId(),
                args[0],
                targetIp,
                staffUuid,
                sender.getName(),
                PunishmentType.WARN,
                reason,
                null
        );

        sender.sendMessage(plugin.getMessageUtil().get("notification.warn")
                .replace("%player%", args[0]));

        if (target.isOnline()) {
            String warnMsg = plugin.getMessageUtil().getLayout("target.warned")
                    .replace("%reason%", reason)
                    .replace("%staff%", sender.getName());
            ((Player)target).sendMessage(warnMsg);
        }

        if (plugin.getConfig().getBoolean("options.broadcast-warns")) {
            plugin.getMessageUtil().sendBroadcast("broadcast.warn", args[0], sender.getName(), reason, null);
        }
        return true;
    }
}