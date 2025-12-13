package com.starfinplugins.bans.service;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.api.PunishmentAPI; // Import necessário
import com.starfinplugins.bans.api.event.PlayerPunishEvent;
import com.starfinplugins.bans.api.event.PlayerUnpunishEvent;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import com.starfinplugins.bans.storage.PunishmentStorage;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public class PunishmentManager implements PunishmentAPI { // Implementação explícita da interface

    private final StarfinBansPlugin plugin;
    private final PunishmentStorage storage;

    public PunishmentManager(StarfinBansPlugin plugin, PunishmentStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public void punish(UUID target, String targetName, String ip, UUID staff, String staffName, PunishmentType type, String reason, Long duration) {
        long createdAt = System.currentTimeMillis();
        long expires = (duration != null && duration > 0) ? createdAt + duration : -1;

        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                target,
                targetName,
                ip,
                staff,
                staffName,
                type,
                reason,
                createdAt,
                expires,
                true
        );

        // Dispara evento (síncrono para permitir cancelamento, se necessário)
        PlayerPunishEvent event = new PlayerPunishEvent(punishment);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // Salva no banco de dados de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.createPunishment(punishment);
        });
    }

    @Override
    public void unban(UUID target, UUID staff, String staffName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.expirePunishment(target, PunishmentType.BAN_PERMANENT, staff, staffName);
            storage.expirePunishment(target, PunishmentType.BAN_TEMPORARY, staff, staffName);

            String lastIp = storage.getLastIp(target);
            if (lastIp != null) {
                storage.expireAllPunishmentsByIp(lastIp, staff, staffName);
            }

            // Evento de Unpunish
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerUnpunishEvent event = new PlayerUnpunishEvent(target, PunishmentType.BAN_PERMANENT, staff, staffName);
                Bukkit.getPluginManager().callEvent(event);
            });
        });
    }

    @Override
    public void unbanIp(String ip, UUID staff, String staffName) {
        if (ip == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.expireAllPunishmentsByIp(ip, staff, staffName);
        });
    }

    @Override
    public void unmute(UUID target, UUID staff, String staffName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.expirePunishment(target, PunishmentType.MUTE_PERMANENT, staff, staffName);
            storage.expirePunishment(target, PunishmentType.MUTE_TEMPORARY, staff, staffName);

            String lastIp = storage.getLastIp(target);
            if (lastIp != null) {
                storage.expireAllPunishmentsByIp(lastIp, staff, staffName);
            }

            // Evento de Unpunish
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerUnpunishEvent event = new PlayerUnpunishEvent(target, PunishmentType.MUTE_PERMANENT, staff, staffName);
                Bukkit.getPluginManager().callEvent(event);
            });
        });
    }

    @Override
    public void unmuteIp(String ip, UUID staff, String staffName) {
        if (ip == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.expireAllPunishmentsByIp(ip, staff, staffName);
        });
    }

    @Override
    public boolean isBanned(UUID target) {
        if (target == null) return false;

        if (storage.getActiveBan(target) != null) return true;

        String lastIp = storage.getLastIp(target);
        return lastIp != null && storage.getActiveIpBan(lastIp) != null;
    }

    @Override
    public boolean isMuted(UUID target) {
        if (target == null) return false;

        if (storage.getActiveMute(target) != null) return true;

        String lastIp = storage.getLastIp(target);
        return lastIp != null && storage.getActiveIpMute(lastIp) != null;
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return storage.getActiveBan(target);
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return storage.getActiveMute(target);
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return storage.getActiveIpBan(ip);
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return storage.getActiveIpMute(ip);
    }

    @Override
    public String getLastIp(UUID target) {
        return storage.getLastIp(target);
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        return storage.getHistory(target);
    }
}