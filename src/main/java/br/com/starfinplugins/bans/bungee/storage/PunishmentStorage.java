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

    /** Returns -1 for an ambiguous ID prefix, 0 when absent, or 1 when updated. */
    int updateReason(String idPrefix, String reason);

    static void validateReasonChange(String idPrefix, String reason) {
        if (idPrefix == null || !idPrefix.matches("(?i)[0-9a-f]{8}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                || reason == null || reason.trim().isEmpty() || reason.length() > 500)
            throw new IllegalArgumentException("ID ou motivo invalido");
    }

    Punishment getActiveIpBan(String ip);
    Punishment getActiveIpMute(String ip);
    Punishment getActiveBan(UUID target);
    Punishment getActiveMute(UUID target);

    Punishment getActiveBanByName(String targetName);
    Punishment getActiveMuteByName(String targetName);

    List<Punishment> getHistory(UUID target);
    List<Punishment> getHistoryByName(String targetName);
}
