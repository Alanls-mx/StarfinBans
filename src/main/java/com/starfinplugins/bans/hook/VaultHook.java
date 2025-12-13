package com.starfinplugins.bans.hook;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Permission permission = null;

    public boolean setupPermissions() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

    public boolean hasPermission(Player player, String node) {
        if (permission != null && permission.isEnabled()) {
            return permission.has(player, node);
        }
        return player.hasPermission(node);
    }
}