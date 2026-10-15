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
        requireAuthorization();
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
            if (!plugin.isLicensed()) return;
            storage.createPunishment(punishment);
        });
    }

    @Override
    public void unban(UUID target, UUID staff, String staffName) {
        requireAuthorization();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.isLicensed()) return;
            storage.expirePunishment(target, PunishmentType.BAN_PERMANENT, staff, staffName);
            storage.expirePunishment(target, PunishmentType.BAN_TEMPORARY, staff, staffName);
            storage.expirePunishment(target, PunishmentType.BAN_IP_PERMANENT, staff, staffName);

            // Evento de Unpunish
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerUnpunishEvent event = new PlayerUnpunishEvent(target, PunishmentType.BAN_PERMANENT, staff, staffName);
                Bukkit.getPluginManager().callEvent(event);
            });
        });
    }

    @Override
    public void unmute(UUID target, UUID staff, String staffName) {
        requireAuthorization();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.isLicensed()) return;
            storage.expirePunishment(target, PunishmentType.MUTE_PERMANENT, staff, staffName);
            storage.expirePunishment(target, PunishmentType.MUTE_TEMPORARY, staff, staffName);
            storage.expirePunishment(target, PunishmentType.MUTE_IP_PERMANENT, staff, staffName);

            // Evento de Unpunish
            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerUnpunishEvent event = new PlayerUnpunishEvent(target, PunishmentType.MUTE_PERMANENT, staff, staffName);
                Bukkit.getPluginManager().callEvent(event);
            });
        });
    }

    @Override
    public boolean isBanned(UUID target) {
        requireAuthorization();
        return storage.getActiveBan(target) != null;
    }

    @Override
    public boolean isMuted(UUID target) {
        requireAuthorization();
        return storage.getActiveMute(target) != null;
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        requireAuthorization();
        return storage.getHistory(target);
    }

    private void requireAuthorization() {
        if (!plugin.isLicensed()) throw new IllegalStateException("starfin_bans nao autorizado pelo StarfinCore");
    }
}
