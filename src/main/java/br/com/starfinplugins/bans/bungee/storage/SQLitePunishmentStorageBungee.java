package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class SQLitePunishmentStorageBungee implements PunishmentStorage, DupeLookupStorage {

    private final StarfinBansBungeePlugin plugin;
    private Connection connection;

    public SQLitePunishmentStorageBungee(StarfinBansBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized void init() {
        try {
            File file = new File(plugin.getDataFolder(), "database.db");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTables();

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeError) {
                    e.addSuppressed(closeError);
                }
                connection = null;
            }
            throw storageFailure("inicializar o armazenamento SQLite", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "target VARCHAR(36), " +
                    "target_name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "staff VARCHAR(36), " +
                    "staff_name VARCHAR(16), " +
                    "type VARCHAR(32), " +
                    "reason TEXT, " +
                    "created_at LONG, " +
                    "expires LONG, " +
                    "active BOOLEAN" +
                    ")");
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception error) {
            throw storageFailure("fechar o armazenamento SQLite", error);
        }
    }

    @Override
    public synchronized void createPunishment(Punishment p) {
        String sql = "INSERT INTO punishments " +
                "(id,target,target_name,ip,staff,staff_name,type,reason,created_at,expires,active) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getId().toString());
            ps.setString(2, p.getTarget() != null ? p.getTarget().toString() : null);
            ps.setString(3, p.getPlayerName());
            ps.setString(4, p.getIp());
            ps.setString(5, p.getStaff() != null ? p.getStaff().toString() : null);
            ps.setString(6, p.getStaffName());
            ps.setString(7, p.getType().name());
            ps.setString(8, p.getReason());
            ps.setLong(9, p.getCreatedAt());
            ps.setLong(10, p.getExpires());
            ps.setBoolean(11, p.isActive());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageFailure("gravar punição no SQLite", e);
        }
    }

    // ---------------- ACTIVE (UUID) ----------------

    @Override
    public synchronized Punishment getActiveIpBan(String ip) {
        if (ip == null || ip.isEmpty()) return null;
        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 " +
                "AND (expires < 0 OR expires > ?) " +
                "AND type IN ('BAN_IP_PERMANENT', 'BAN_IP_TEMPORARY') " +
                "ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            statement.setLong(2, System.currentTimeMillis());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    Punishment punishment = map(rows);
                    return punishment.isExpired() ? null : punishment;
                }
            }
        } catch (SQLException error) {
            throw storageFailure("consultar banimento por IP no SQLite", error);
        }
        return null;
    }

    @Override
    public synchronized Punishment getActiveIpMute(String ip) {
        if (ip == null || ip.isEmpty()) return null;
        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 " +
                "AND (expires < 0 OR expires > ?) " +
                "AND type IN ('MUTE_IP_PERMANENT', 'MUTE_IP_TEMPORARY') " +
                "ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            statement.setLong(2, System.currentTimeMillis());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    Punishment punishment = map(rows);
                    return punishment.isExpired() ? null : punishment;
                }
            }
        } catch (SQLException error) {
            throw storageFailure("consultar mute por IP no SQLite", error);
        }
        return null;
    }

    @Override
    public synchronized Punishment getActiveBan(UUID target) {
        return getActivePunishmentByTarget(target, "BAN");
    }

    @Override
    public synchronized Punishment getActiveMute(UUID target) {
        return getActivePunishmentByTarget(target, "MUTE");
    }

    private Punishment getActivePunishmentByTarget(UUID target, String prefix) {
        if (target == null) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE target = ? AND active = 1 AND type IN (?,?) " +
                "AND (expires < 0 OR expires > ?) " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, "BAN".equals(prefix) ? "BAN_PERMANENT" : "MUTE_PERMANENT");
            ps.setString(3, "BAN".equals(prefix) ? "BAN_TEMPORARY" : "MUTE_TEMPORARY");
            ps.setLong(4, System.currentTimeMillis());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = map(rs);

                    return p;
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar punição ativa no SQLite", e);
        }

        return null;
    }

    // ---------------- ACTIVE (NAME) ----------------

    @Override
    public synchronized Punishment getActiveBanByName(String targetName) {
        return getActivePunishmentByName(targetName, "BAN");
    }

    @Override
    public synchronized Punishment getActiveMuteByName(String targetName) {
        return getActivePunishmentByName(targetName, "MUTE");
    }

    private Punishment getActivePunishmentByName(String targetName, String prefix) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE lower(target_name) = lower(?) AND active = 1 AND type IN (?,?) " +
                "AND (expires < 0 OR expires > ?) " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.setString(2, "BAN".equals(prefix) ? "BAN_PERMANENT" : "MUTE_PERMANENT");
            ps.setString(3, "BAN".equals(prefix) ? "BAN_TEMPORARY" : "MUTE_TEMPORARY");
            ps.setLong(4, System.currentTimeMillis());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = map(rs);

                    return p;
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar punição por nome no SQLite", e);
        }

        return null;
    }

    // ---------------- HISTORY ----------------

    @Override
    public synchronized List<Punishment> getHistory(UUID target) {
        if (target == null) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE target = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw storageFailure("consultar histórico no SQLite", e);
        }

        return list;
    }

    @Override
    public synchronized List<Punishment> getHistoryByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE lower(target_name) = lower(?) ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw storageFailure("consultar histórico por nome no SQLite", e);
        }

        return list;
    }

    // ---------------- EXPIRE ----------------

    @Override
    public synchronized void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;

        String sql = "UPDATE punishments SET active = 0 WHERE target = ? AND type = ? AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageFailure("revogar punição no SQLite", e);
        }
    }

    @Override
    public synchronized void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty() || type == null) return;

        String sql = "UPDATE punishments SET active = 0 " +
                "WHERE lower(target_name) = lower(?) AND type = ? AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageFailure("revogar punição por nome no SQLite", e);
        }
    }

    @Override
    public synchronized void expireAllPunishmentsByName(String targetName, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty()) return;

        String sql = "UPDATE punishments SET active = 0 WHERE lower(target_name) = lower(?) AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageFailure("revogar punições por nome no SQLite", e);
        }
    }

    // ---------------- DUPE LOOKUP (extra, sem quebrar interface) ----------------

    @Override
    public synchronized String getLastIpByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT ip FROM punishments WHERE lower(target_name) = lower(?) " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ip");
            }
        } catch (SQLException e) {
            throw storageFailure("consultar último IP no SQLite", e);
        }

        return null;
    }

    @Override
    public synchronized List<String> getAlts(String ip) {
        if (ip == null || ip.isEmpty()) return Collections.emptyList();

        List<String> alts = new ArrayList<>();
        String sql = "SELECT DISTINCT target_name FROM punishments WHERE ip = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("target_name");
                    if (name != null && !name.isEmpty()) alts.add(name);
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar contas por IP no SQLite", e);
        }

        return alts;
    }

    // ---------------- MAPPING ----------------

    private Punishment map(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String target = rs.getString("target");
        String staff = rs.getString("staff");

        return new Punishment(
                UUID.fromString(id),
                target != null ? UUID.fromString(target) : null,
                rs.getString("target_name"),
                rs.getString("ip"),
                staff != null ? UUID.fromString(staff) : null,
                rs.getString("staff_name"),
                PunishmentType.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getLong("expires"),
                rs.getBoolean("active")
        );
    }

    private IllegalStateException storageFailure(String operation, Exception error) {
        plugin.getLogger().log(Level.SEVERE,
                "Falha ao " + operation + "; a operação não foi confirmada.", error);
        return new IllegalStateException("Falha ao " + operation + ".", error);
    }
}
