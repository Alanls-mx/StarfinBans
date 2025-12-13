package br.com.starfinplugins.bans.bungee.service;

import br.com.starfinplugins.bans.bungee.api.PunishmentAPI;
import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.web.WebIntegrationService;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import net.md_5.bungee.api.ProxyServer;

import java.util.List;
import java.util.UUID;

public class PunishmentManager implements PunishmentAPI {

    private final StarfinBansBungeePlugin plugin;
    private final PunishmentStorage storage;
    private final WebIntegrationService web;

    public PunishmentManager(
            StarfinBansBungeePlugin plugin,
            PunishmentStorage storage,
            WebIntegrationService web
    ) {
        this.plugin = plugin;
        this.storage = storage;
        this.web = web;
    }

    @Override
    public void punish(UUID target, String targetName, String ip, UUID staff, String staffName,
                       PunishmentType type, String reason, Long duration) {

        long createdAt = System.currentTimeMillis();
        long expires = (duration != null && duration > 0) ? createdAt + duration : -1;

        Punishment p = new Punishment(
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

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            storage.createPunishment(p);
            web.sendCreate(p);
        });
    }

    @Override
    public void unban(UUID target, UUID staff, String staffName) {
        Punishment p = storage.getActiveBan(target);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            expireByNameAndIp(p, staff, staffName);
        });
    }

    @Override
    public void unbanByName(String targetName, UUID staff, String staffName) {
        Punishment p = storage.getActiveBanByName(targetName);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            expireByNameAndIp(p, staff, staffName);
        });
    }

    @Override
    public void unbanIp(String ip, UUID staff, String staffName) {
        Punishment p = storage.getActiveIpBan(ip);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            storage.expireAllPunishmentsByIp(ip, staff, staffName);
            web.sendExpire(p);
        });
    }

    @Override
    public void unmute(UUID target, UUID staff, String staffName) {
        Punishment p = storage.getActiveMute(target);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            expireByNameAndIp(p, staff, staffName);
        });
    }

    @Override
    public void unmuteByName(String targetName, UUID staff, String staffName) {
        Punishment p = storage.getActiveMuteByName(targetName);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            expireByNameAndIp(p, staff, staffName);
        });
    }

    @Override
    public void unmuteIp(String ip, UUID staff, String staffName) {
        Punishment p = storage.getActiveIpMute(ip);
        if (p == null) return;

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            storage.expireAllPunishmentsByIp(ip, staff, staffName);
            web.sendExpire(p);
        });
    }

    @Override public boolean isBanned(UUID target) { return storage.getActiveBan(target) != null; }
    @Override public boolean isMuted(UUID target) { return storage.getActiveMute(target) != null; }

    @Override public Punishment getActiveBan(UUID target) { return storage.getActiveBan(target); }
    @Override public Punishment getActiveMute(UUID target) { return storage.getActiveMute(target); }

    @Override public Punishment getActiveIpBan(String ip) { return storage.getActiveIpBan(ip); }
    @Override public Punishment getActiveIpMute(String ip) { return storage.getActiveIpMute(ip); }

    @Override public Punishment getActiveBanByName(String targetName) { return storage.getActiveBanByName(targetName); }
    @Override public Punishment getActiveMuteByName(String targetName) { return storage.getActiveMuteByName(targetName); }

    @Override public List<Punishment> getHistory(UUID target) { return storage.getHistory(target); }
    @Override public List<Punishment> getHistoryByName(String targetName) { return storage.getHistoryByName(targetName); }

    private void expireByNameAndIp(Punishment p, UUID staff, String staffName) {
        storage.expireAllPunishmentsByName(p.getPlayerName(), staff, staffName);
        if (p.getIp() != null && !p.getIp().isEmpty()) {
            storage.expireAllPunishmentsByIp(p.getIp(), staff, staffName);
        }
        web.sendExpire(p);
    }
}
