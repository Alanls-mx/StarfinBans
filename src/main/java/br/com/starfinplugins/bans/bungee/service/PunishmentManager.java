package br.com.starfinplugins.bans.service;

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
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            List<Punishment> history = storage.getHistory(target);
            expireByUuid(target, history, true, staff, staffName);
        });
    }

    @Override
    public void unbanByName(String targetName, UUID staff, String staffName) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin,
                () -> expireByName(targetName, true, staff, staffName));
    }

    @Override
    public void unmute(UUID target, UUID staff, String staffName) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            List<Punishment> history = storage.getHistory(target);
            expireByUuid(target, history, false, staff, staffName);
        });
    }

    @Override
    public void unmuteByName(String targetName, UUID staff, String staffName) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin,
                () -> expireByName(targetName, false, staff, staffName));
    }

    private void expireByName(String targetName, boolean ban, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty()) return;

        List<Punishment> history = storage.getHistoryByName(targetName);
        Punishment latestActive = latestActive(history, ban);
        for (PunishmentType type : typesFor(ban)) {
            storage.expirePunishmentByName(targetName, type, staff, staffName);
        }
        sendExpire(latestActive);
    }

    private void expireByUuid(UUID target, List<Punishment> history, boolean ban, UUID staff, String staffName) {
        if (target == null) return;
        Punishment latestActive = latestActive(history, ban);
        for (PunishmentType type : typesFor(ban)) {
            storage.expirePunishment(target, type, staff, staffName);
        }
        sendExpire(latestActive);
    }

    private Punishment latestActive(List<Punishment> history, boolean ban) {
        Punishment latestActive = null;
        for (Punishment punishment : history) {
            String type = punishment.getType().name();
            boolean matches = ban ? type.startsWith("BAN_") : type.startsWith("MUTE_");
            if (matches && punishment.isActive() && !punishment.isExpired()
                    && (latestActive == null || punishment.getCreatedAt() > latestActive.getCreatedAt())) {
                latestActive = punishment;
            }
        }
        return latestActive;
    }

    private PunishmentType[] typesFor(boolean ban) {
        return ban
                ? new PunishmentType[]{
                PunishmentType.BAN_PERMANENT, PunishmentType.BAN_TEMPORARY,
                PunishmentType.BAN_IP_PERMANENT, PunishmentType.BAN_IP_TEMPORARY
        }
                : new PunishmentType[]{
                PunishmentType.MUTE_PERMANENT, PunishmentType.MUTE_TEMPORARY,
                PunishmentType.MUTE_IP_PERMANENT, PunishmentType.MUTE_IP_TEMPORARY
        };
    }

    private void sendExpire(Punishment latestActive) {
        if (latestActive != null) {
            latestActive.setActive(false);
            web.sendExpire(latestActive);
        }
    }

    @Override
    public boolean isBanned(UUID target) {
        return storage.getActiveBan(target) != null;
    }

    @Override
    public boolean isMuted(UUID target) {
        return storage.getActiveMute(target) != null;
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
    public Punishment getActiveBanByName(String targetName) {
        return storage.getActiveBanByName(targetName);
    }

    @Override
    public Punishment getActiveMuteByName(String targetName) {
        return storage.getActiveMuteByName(targetName);
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        return storage.getHistory(target);
    }

    @Override
    public List<Punishment> getHistoryByName(String targetName) {
        return storage.getHistoryByName(targetName);
    }
}
