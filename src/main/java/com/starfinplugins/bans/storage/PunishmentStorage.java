package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.util.List;
import java.util.UUID;

public interface PunishmentStorage {
    void init();
    void close();

    void createPunishment(Punishment punishment);

    Punishment getActiveBan(UUID target);
    Punishment getActiveMute(UUID target);

    default Punishment getActiveBanByName(String name) { return null; }
    default Punishment getActiveMuteByName(String name) { return null; }

    Punishment getActiveIpBan(String ip);
    Punishment getActiveIpMute(String ip);

    List<Punishment> getHistory(UUID target);

    void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName);

    String getLastIp(UUID target);
    List<String> getAlts(String ip);
}
