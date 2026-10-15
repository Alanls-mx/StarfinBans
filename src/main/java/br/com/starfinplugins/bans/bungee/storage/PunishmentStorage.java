package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;

import java.util.List;
import java.util.UUID;

public interface PunishmentStorage {

    void init();
    void close();

    void createPunishment(Punishment punishment);

    void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName);

    void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName);

    void expireAllPunishmentsByName(String targetName, UUID staff, String staffName);

    Punishment getActiveIpBan(String ip);
    Punishment getActiveBan(UUID target);
    Punishment getActiveMute(UUID target);

    Punishment getActiveBanByName(String targetName);
    Punishment getActiveMuteByName(String targetName);

    List<Punishment> getHistory(UUID target);
    List<Punishment> getHistoryByName(String targetName);
}
