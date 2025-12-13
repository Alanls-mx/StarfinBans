package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MemoryPunishmentStorageBungee implements PunishmentStorage {

    private final List<Punishment> punishments = new CopyOnWriteArrayList<>();

    @Override public void init() {}
    @Override public void close() {}

    @Override
    public void createPunishment(Punishment punishment) {
        punishments.add(punishment);
    }

    @Override
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        for (Punishment p : punishments) {
            if (p.isActive()
                    && p.getTarget() != null
                    && p.getTarget().equals(target)
                    && p.getType() == type) {
                p.setActive(false);
            }
        }
    }

    @Override
    public void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName) {
        for (Punishment p : punishments) {
            if (p.isActive()
                    && p.getPlayerName() != null
                    && p.getPlayerName().equalsIgnoreCase(targetName)
                    && p.getType() == type) {
                p.setActive(false);
            }
        }
    }

    @Override
    public void expireAllPunishmentsByName(String targetName, UUID staff, String staffName) {
        for (Punishment p : punishments) {
            if (p.isActive()
                    && p.getPlayerName() != null
                    && p.getPlayerName().equalsIgnoreCase(targetName)) {
                p.setActive(false);
            }
        }
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return punishments.stream()
                .filter(p -> p.isActive()
                        && p.getTarget() != null
                        && p.getTarget().equals(target)
                        && p.getType().name().startsWith("BAN"))
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return punishments.stream()
                .filter(p -> p.isActive()
                        && p.getTarget() != null
                        && p.getTarget().equals(target)
                        && p.getType().name().startsWith("MUTE"))
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveBanByName(String targetName) {
        return punishments.stream()
                .filter(p -> p.isActive()
                        && p.getPlayerName() != null
                        && p.getPlayerName().equalsIgnoreCase(targetName)
                        && p.getType().name().startsWith("BAN"))
                .findFirst().orElse(null);
    }

    @Override
    public Punishment getActiveMuteByName(String targetName) {
        return punishments.stream()
                .filter(p -> p.isActive()
                        && p.getPlayerName() != null
                        && p.getPlayerName().equalsIgnoreCase(targetName)
                        && p.getType().name().startsWith("MUTE"))
                .findFirst().orElse(null);
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        List<Punishment> list = new ArrayList<>();
        for (Punishment p : punishments) {
            if (p.getTarget() != null && p.getTarget().equals(target)) {
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public List<Punishment> getHistoryByName(String targetName) {
        List<Punishment> list = new ArrayList<>();
        for (Punishment p : punishments) {
            if (p.getPlayerName() != null && p.getPlayerName().equalsIgnoreCase(targetName)) {
                list.add(p);
            }
        }
        return list;
    }
}
