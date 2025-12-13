package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MemoryPunishmentStorage implements PunishmentStorage {

    private final List<Punishment> punishments = new ArrayList<>();

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
                .filter(Punishment::isActive)
                .filter(p -> p.getType().isBan() && !p.getType().isIpBan())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return punishments.stream()
                .filter(p -> p.getTarget() != null && p.getTarget().equals(target))
                .filter(Punishment::isActive)
                .filter(p -> p.getType().isMute() && !p.getType().isIpMute())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return punishments.stream()
                .filter(p -> p.getIp() != null && p.getIp().equals(ip))
                .filter(Punishment::isActive)
                .filter(p -> p.getType().isIpBan())
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return punishments.stream()
                .filter(p -> p.getIp() != null && p.getIp().equals(ip))
                .filter(Punishment::isActive)
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
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        // Em memória, apenas removemos ou marcamos como inativo (aqui não dá pra mudar o campo final 'active' facilmente sem recriar,
        // mas num banco SQL faríamos um UPDATE. Para simplificar o exemplo em memória:)
        if (target == null || type == null) return;

        punishments.removeIf(p -> target.equals(p.getTarget()) && p.getType() == type && p.isActive());
    }

    @Override
    public void expireAllPunishmentsByIp(String ip, UUID staff, String staffName) {
        if (ip == null || ip.isEmpty()) return;

        punishments.removeIf(p -> ip.equals(p.getIp()) && p.isActive());
    }

    @Override
    public String getLastIp(UUID target) {
        // Simplificado
        if (target == null) return null;

        return punishments.stream()
                .filter(p -> target.equals(p.getTarget()))
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