package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;

import java.util.List;
import java.util.UUID;

public interface PunishmentStorage {

    boolean init();
    void close();

    void createPunishment(Punishment punishment);

    void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName);

    void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName);

    void expireAllPunishmentsByName(String targetName, UUID staff, String staffName);
    void expireAllPunishmentsByIp(String ip, UUID staff, String staffName);

    boolean updateReason(UUID punishmentId, String reason);

    Punishment getActiveBan(UUID target);
    Punishment getActiveMute(UUID target);

    Punishment getActiveIpBan(String ip);
    Punishment getActiveIpMute(String ip);

    Punishment getActiveBanByName(String targetName);
    Punishment getActiveMuteByName(String targetName);

    List<Punishment> getHistory(UUID target);
    List<Punishment> getHistoryByName(String targetName);
}
