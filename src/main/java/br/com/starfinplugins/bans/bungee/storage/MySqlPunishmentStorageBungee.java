package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import net.md_5.bungee.config.Configuration;
import java.util.List;
import java.util.UUID;

/**
 * The former class silently exposed an unimplemented MySQL backend.
 * Refuse to initialize until transactional storage exists. Never pretend
 * that punishment data was written successfully.
 */
public final class MySqlPunishmentStorageBungee implements PunishmentStorage {
    public MySqlPunishmentStorageBungee(StarfinBansBungeePlugin plugin, Configuration cfg) {
        throw new UnsupportedOperationException(
            "MySQL Bungee ainda nao foi implementado. Use SQLite com backup ou desative este modulo.");
    }
    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Bungee MySQL indisponivel");
    }
    @Override public void init() { throw unsupported(); }
    @Override public void close() { }
    @Override public void createPunishment(Punishment value) { throw unsupported(); }
    @Override public void expirePunishment(UUID id, PunishmentType type, UUID staff, String staffName) { throw unsupported(); }
    @Override public void expirePunishmentByName(String name, PunishmentType type, UUID staff, String staffName) { throw unsupported(); }
    @Override public void expireAllPunishmentsByName(String name, UUID staff, String staffName) { throw unsupported(); }
    @Override public Punishment getActiveBan(UUID id) { throw unsupported(); }
    @Override public Punishment getActiveIpBan(String ip) { throw unsupported(); }
    @Override public Punishment getActiveMute(UUID id) { throw unsupported(); }
    @Override public Punishment getActiveBanByName(String name) { throw unsupported(); }
    @Override public Punishment getActiveMuteByName(String name) { throw unsupported(); }
    @Override public List<Punishment> getHistory(UUID id) { throw unsupported(); }
    @Override public List<Punishment> getHistoryByName(String name) { throw unsupported(); }
}
