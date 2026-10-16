package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MemoryPunishmentStorage implements PunishmentStorage {

    private final List<Punishment> punishments = new CopyOnWriteArrayList<>();

    @Override
    public void init() {}

    @Override
    public void close() {}

    @Override
    public void createPunishment(Punishment punishment) {
        punishments.add(punishment);
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return punishments.stream()
                .filter(p -> p.getTarget() != null && p.getTarget().equals(target))
                .filter(p -> p.isActive() && !p.isExpired())
                .filter(p -> p.getType().isBan() && !p.getType().isIpBan())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return punishments.stream()
                .filter(p -> p.getTarget() != null && p.getTarget().equals(target))
                .filter(p -> p.isActive() && !p.isExpired())
                .filter(p -> p.getType().isMute() && !p.getType().isIpMute())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return punishments.stream()
                .filter(p -> p.getIp() != null && p.getIp().equals(ip))
                .filter(p -> p.isActive() && !p.isExpired())
                .filter(p -> p.getType().isIpBan())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return punishments.stream()
                .filter(p -> p.getIp() != null && p.getIp().equals(ip))
                .filter(p -> p.isActive() && !p.isExpired())
                .filter(p -> p.getType().isIpMute())
                .findFirst().orElse(null);
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        return punishments.stream()
                .filter(p -> p.getTarget() != null && p.getTarget().equals(target))
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getHistoryByName(String name) {
        return punishments.stream()
                .filter(p -> name != null && name.equalsIgnoreCase(p.getPlayerName()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;
        for (int i = 0; i < punishments.size(); i++) {
            Punishment p = punishments.get(i);
            if (target.equals(p.getTarget()) && p.getType() == type && p.isActive())
                punishments.set(i, copy(p, p.getReason(), false));
        }
    }

    @Override
    public synchronized void expirePunishmentByName(String name, PunishmentType type, UUID staff, String staffName) {
        if (name == null || type == null) return;
        for (int i = 0; i < punishments.size(); i++) {
            Punishment p = punishments.get(i);
            if (name.equalsIgnoreCase(p.getPlayerName()) && p.getType() == type && p.isActive())
                punishments.set(i, copy(p, p.getReason(), false));
        }
    }

    @Override
    public synchronized int updateReason(String idPrefix, String reason) {
        PunishmentStorage.validateReasonChange(idPrefix, reason);
        idPrefix = idPrefix.toLowerCase(java.util.Locale.ROOT);
        int match = -1;
        for (int i = 0; i < punishments.size(); i++) {
            if (punishments.get(i).getId().toString().startsWith(idPrefix)) {
                if (match >= 0) return -1;
                match = i;
            }
        }
        if (match < 0) return 0;
        Punishment p = punishments.get(match);
        punishments.set(match, copy(p, reason, p.isActive()));
        return 1;
    }

    private static Punishment copy(Punishment p, String reason, boolean active) {
        return new Punishment(p.getId(), p.getTarget(), p.getPlayerName(), p.getIp(),
            p.getStaff(), p.getStaffName(), p.getType(), reason, p.getCreatedAt(), p.getExpires(), active);
    }

    @Override
    public String getLastIp(UUID target) {
        // Simplificado
        return punishments.stream()
                .filter(p -> target != null && target.equals(p.getTarget()))
                .map(Punishment::getIp)
                .findFirst().orElse(null);
    }

    @Override
    public List<String> getAlts(String ip) {
        return punishments.stream()
                .filter(p -> p.getIp() != null && p.getIp().equals(ip))
                .map(Punishment::getPlayerName)
                .distinct()
                .collect(Collectors.toList());
    }
}
