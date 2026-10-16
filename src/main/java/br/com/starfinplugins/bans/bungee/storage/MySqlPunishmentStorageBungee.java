package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class MySqlPunishmentStorageBungee implements PunishmentStorage, DupeLookupStorage {

    private static final String TABLE = "punishments";
    private final StarfinBansBungeePlugin plugin;
    private final HikariConfig poolConfig;
    private HikariDataSource dataSource;

    public MySqlPunishmentStorageBungee(StarfinBansBungeePlugin plugin, Configuration config) {
        this.plugin = plugin;
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "starfinbans");
        boolean ssl = config.getBoolean("database.use-ssl", false);

        poolConfig = new HikariConfig();
        poolConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + ssl + "&serverTimezone=UTC&characterEncoding=utf8");
        poolConfig.setUsername(config.getString("database.user", "starfinbans"));
        poolConfig.setPassword(config.getString("database.password", ""));
        poolConfig.setMaximumPoolSize(Math.max(1, config.getInt("database.pool-size", 5)));
        poolConfig.setConnectionTimeout(Math.max(250, config.getInt("database.connection-timeout", 10000)));
        poolConfig.setPoolName("StarfinBans-Bungee-MySQL");
    }

    @Override
    public synchronized void init() {
        if (dataSource != null && !dataSource.isClosed()) return;
        HikariDataSource pool = new HikariDataSource(poolConfig);
        try (Connection connection = pool.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY,"
                    + "target VARCHAR(36) NULL,"
                    + "target_name VARCHAR(16) NULL,"
                    + "ip VARCHAR(45) NULL,"
                    + "staff VARCHAR(36) NULL,"
                    + "staff_name VARCHAR(16) NULL,"
                    + "type VARCHAR(32) NOT NULL,"
                    + "reason TEXT NULL,"
                    + "created_at BIGINT NOT NULL,"
                    + "expires BIGINT NOT NULL,"
                    + "active BOOLEAN NOT NULL,"
                    + "INDEX idx_sb_target_active (target, active, created_at),"
                    + "INDEX idx_sb_name_active (target_name, active, created_at),"
                    + "INDEX idx_sb_ip_active (ip, active, created_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            ensureIndex(connection, "idx_sb_target_active", "target, active, created_at");
            ensureIndex(connection, "idx_sb_name_active", "target_name, active, created_at");
            ensureIndex(connection, "idx_sb_ip_active", "ip, active, created_at");
            statement.execute("CREATE TABLE IF NOT EXISTS starfinbans_schema_version ("
                    + "version INT NOT NULL PRIMARY KEY,"
                    + "installed_at BIGINT NOT NULL"
                    + ") ENGINE=InnoDB");
            try (PreparedStatement migration = connection.prepareStatement(
                    "INSERT IGNORE INTO starfinbans_schema_version (version, installed_at) VALUES (1, ?)")) {
                migration.setLong(1, System.currentTimeMillis());
                migration.executeUpdate();
            }
            dataSource = pool;
        } catch (SQLException | RuntimeException error) {
            pool.close();
            throw storageFailure("inicializar o banco MySQL", error);
        }
    }

    private void ensureIndex(Connection connection, String indexName, String columns) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ? LIMIT 1")) {
            statement.setString(1, TABLE);
            statement.setString(2, indexName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + indexName + " ON " + TABLE + " (" + columns + ")");
        } catch (SQLException error) {
            if (error.getErrorCode() != 1061 || !indexExists(connection, indexName)) {
                throw error;
            }
        }
    }

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ? LIMIT 1")) {
            statement.setString(1, TABLE);
            statement.setString(2, indexName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    @Override
    public void createPunishment(Punishment punishment) {
        if (punishment == null || punishment.getId() == null || punishment.getType() == null) {
            throw new IllegalArgumentException("Punição inválida.");
        }
        executeUpdate("INSERT INTO " + TABLE
                        + " (id,target,target_name,ip,staff,staff_name,type,reason,created_at,expires,active)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                statement -> bindPunishment(statement, punishment));
    }

    @Override
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;
        executeUpdate("UPDATE " + TABLE + " SET active = 0 WHERE target = ? AND type = ? AND active = 1",
                statement -> {
                    statement.setString(1, target.toString());
                    statement.setString(2, type.name());
                });
    }

    @Override
    public void expirePunishmentByName(String name, PunishmentType type, UUID staff, String staffName) {
        if (name == null || name.isEmpty() || type == null) return;
        executeUpdate("UPDATE " + TABLE
                        + " SET active = 0 WHERE LOWER(target_name) = LOWER(?) AND type = ? AND active = 1",
                statement -> {
                    statement.setString(1, name);
                    statement.setString(2, type.name());
                });
    }

    @Override
    public void expireAllPunishmentsByName(String name, UUID staff, String staffName) {
        if (name == null || name.isEmpty()) return;
        executeUpdate("UPDATE " + TABLE
                        + " SET active = 0 WHERE LOWER(target_name) = LOWER(?) AND active = 1",
                statement -> statement.setString(1, name));
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return findActiveByUuid(target, "BAN_PERMANENT", "BAN_TEMPORARY");
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return findActiveByUuid(target, "MUTE_PERMANENT", "MUTE_TEMPORARY");
    }

    private Punishment findActiveByUuid(UUID target, String permanent, String temporary) {
        if (target == null) return null;
        return findOne("SELECT * FROM " + TABLE
                        + " WHERE target = ? AND active = 1 AND type IN (?,?) AND (expires < 0 OR expires > ?)"
                        + " ORDER BY created_at DESC LIMIT 1",
                statement -> {
                    statement.setString(1, target.toString());
                    statement.setString(2, permanent);
                    statement.setString(3, temporary);
                    statement.setLong(4, System.currentTimeMillis());
                });
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return findActiveByIp(ip, "BAN_IP_PERMANENT", "BAN_IP_TEMPORARY");
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return findActiveByIp(ip, "MUTE_IP_PERMANENT", "MUTE_IP_TEMPORARY");
    }

    private Punishment findActiveByIp(String ip, String permanent, String temporary) {
        if (ip == null || ip.isEmpty()) return null;
        return findOne("SELECT * FROM " + TABLE
                        + " WHERE ip = ? AND active = 1 AND type IN (?,?) AND (expires < 0 OR expires > ?)"
                        + " ORDER BY created_at DESC LIMIT 1",
                statement -> {
                    statement.setString(1, ip);
                    statement.setString(2, permanent);
                    statement.setString(3, temporary);
                    statement.setLong(4, System.currentTimeMillis());
                });
    }

    @Override
    public Punishment getActiveBanByName(String name) {
        return findActiveByName(name, "BAN_PERMANENT", "BAN_TEMPORARY");
    }

    @Override
    public Punishment getActiveMuteByName(String name) {
        return findActiveByName(name, "MUTE_PERMANENT", "MUTE_TEMPORARY");
    }

    private Punishment findActiveByName(String name, String permanent, String temporary) {
        if (name == null || name.isEmpty()) return null;
        return findOne("SELECT * FROM " + TABLE
                        + " WHERE LOWER(target_name) = LOWER(?) AND active = 1 AND type IN (?,?)"
                        + " AND (expires < 0 OR expires > ?) ORDER BY created_at DESC LIMIT 1",
                statement -> {
                    statement.setString(1, name);
                    statement.setString(2, permanent);
                    statement.setString(3, temporary);
                    statement.setLong(4, System.currentTimeMillis());
                });
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        if (target == null) return Collections.emptyList();
        return findMany("SELECT * FROM " + TABLE + " WHERE target = ? ORDER BY created_at DESC",
                statement -> statement.setString(1, target.toString()));
    }

    @Override
    public List<Punishment> getHistoryByName(String name) {
        if (name == null || name.isEmpty()) return Collections.emptyList();
        return findMany("SELECT * FROM " + TABLE
                        + " WHERE LOWER(target_name) = LOWER(?) ORDER BY created_at DESC",
                statement -> statement.setString(1, name));
    }

    @Override
    public String getLastIpByName(String name) {
        if (name == null || name.isEmpty()) return null;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT ip FROM " + TABLE
                     + " WHERE LOWER(target_name) = LOWER(?) ORDER BY created_at DESC LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString("ip") : null;
            }
        } catch (SQLException error) {
            throw storageFailure("consultar último IP", error);
        }
    }

    @Override
    public List<String> getAlts(String ip) {
        if (ip == null || ip.isEmpty()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT target_name FROM " + TABLE + " WHERE ip = ?")) {
            statement.setString(1, ip);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String name = rows.getString(1);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            return names;
        } catch (SQLException error) {
            throw storageFailure("consultar contas por IP", error);
        }
    }

    private Punishment findOne(String sql, Binder binder) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        } catch (SQLException error) {
            throw storageFailure("consultar punição", error);
        }
    }

    private List<Punishment> findMany(String sql, Binder binder) {
        List<Punishment> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(map(rows));
            }
            return result;
        } catch (SQLException error) {
            throw storageFailure("consultar histórico", error);
        }
    }

    private void executeUpdate(String sql, Binder binder) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw storageFailure("gravar dados de punição", error);
        }
    }

    private void bindPunishment(PreparedStatement statement, Punishment punishment) throws SQLException {
        statement.setString(1, punishment.getId().toString());
        statement.setString(2, punishment.getTarget() == null ? null : punishment.getTarget().toString());
        statement.setString(3, punishment.getPlayerName());
        statement.setString(4, punishment.getIp());
        statement.setString(5, punishment.getStaff() == null ? null : punishment.getStaff().toString());
        statement.setString(6, punishment.getStaffName());
        statement.setString(7, punishment.getType().name());
        statement.setString(8, punishment.getReason());
        statement.setLong(9, punishment.getCreatedAt());
        statement.setLong(10, punishment.getExpires());
        statement.setBoolean(11, punishment.isActive());
    }

    private Punishment map(ResultSet rows) throws SQLException {
        String target = rows.getString("target");
        String staff = rows.getString("staff");
        return new Punishment(UUID.fromString(rows.getString("id")),
                target == null ? null : UUID.fromString(target),
                rows.getString("target_name"), rows.getString("ip"),
                staff == null ? null : UUID.fromString(staff),
                rows.getString("staff_name"), PunishmentType.valueOf(rows.getString("type")),
                rows.getString("reason"), rows.getLong("created_at"), rows.getLong("expires"),
                rows.getBoolean("active"));
    }

    private Connection connection() throws SQLException {
        HikariDataSource current = dataSource;
        if (current == null || current.isClosed()) {
            throw new IllegalStateException("MySQL não está inicializado.");
        }
        return current.getConnection();
    }

    private IllegalStateException storageFailure(String operation, Exception error) {
        plugin.getLogger().log(Level.SEVERE,
                "Falha ao " + operation + " no MySQL; a operação não foi confirmada.", error);
        return new IllegalStateException("Falha ao " + operation + " no armazenamento MySQL.", error);
    }

    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
