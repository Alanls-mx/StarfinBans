package com.starfinplugins.bans.api;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.util.List;
import java.util.UUID;

public interface PunishmentAPI {
    void punish(UUID target, String targetName, String ip, UUID staff, String staffName, PunishmentType type, String reason, Long duration);

    void unban(UUID target, UUID staff, String staffName);
    void unbanIp(String ip, UUID staff, String staffName);
    void unmute(UUID target, UUID staff, String staffName);
    void unmuteIp(String ip, UUID staff, String staffName);

    boolean isBanned(UUID target);
    boolean isMuted(UUID target);

    Punishment getActiveBan(UUID target);
    Punishment getActiveMute(UUID target);
    Punishment getActiveIpBan(String ip);
    Punishment getActiveIpMute(String ip);

    String getLastIp(UUID target);

    List<Punishment> getHistory(UUID target);
}