package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLitePunishmentStorage implements PunishmentStorage {

    private final StarfinBansPlugin plugin;
    private Connection connection;

    public SQLitePunishmentStorage(StarfinBansPlugin plugin) {
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

    private void createTables() {
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
        } catch (SQLException e) {
            throw storageFailure("criar a tabela de punições SQLite", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            throw storageFailure("fechar o armazenamento SQLite", e);
        }
    }

    @Override
    public synchronized void createPunishment(Punishment p) {
        String sql = "INSERT INTO punishments (id, target, target_name, ip, staff, staff_name, type, reason, created_at, expires, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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

    @Override
    public synchronized Punishment getActiveBan(UUID target) {
        return getActivePunishment(target, "BAN");
    }

    @Override
    public synchronized Punishment getActiveMute(UUID target) {
        return getActivePunishment(target, "MUTE");
    }

    private Punishment getActivePunishment(UUID target, String typePrefix) {
        String sql = "SELECT * FROM punishments WHERE target = ? AND active = 1 AND type IN (?,?) " +
                "AND (expires < 0 OR expires > ?) ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, typePrefix.equals("BAN") ? "BAN_PERMANENT" : "MUTE_PERMANENT");
            ps.setString(3, typePrefix.equals("BAN") ? "BAN_TEMPORARY" : "MUTE_TEMPORARY");
            ps.setLong(4, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = mapPunishment(rs);
                    return p;
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar punição ativa no SQLite", e);
        }
        return null;
    }

    @Override
    public synchronized Punishment getActiveIpBan(String ip) {
        return getActiveIpPunishment(ip, "BAN_IP");
    }

    @Override
    public synchronized Punishment getActiveIpMute(String ip) {
        return getActiveIpPunishment(ip, "MUTE_IP");
    }

    private Punishment getActiveIpPunishment(String ip, String typePrefix) {
        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 AND type LIKE ? " +
                "AND (expires < 0 OR expires > ?) ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, typePrefix + "%");
            ps.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPunishment(rs);
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar punição por IP no SQLite", e);
        }
        return null;
    }

    @Override
    public synchronized List<Punishment> getHistory(UUID target) {
        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE target = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar histórico no SQLite", e);
        }
        return list;
    }

    @Override
    public synchronized List<Punishment> getHistoryByName(String name) {
        List<Punishment> list = new ArrayList<>();
        if (name == null || name.isEmpty()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM punishments WHERE LOWER(target_name) = LOWER(?) ORDER BY created_at DESC")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPunishment(rs));
            }
        } catch (SQLException error) {
            throw storageFailure("consultar histórico por nome no SQLite", error);
        }
        return list;
    }

    @Override
    public synchronized void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        // Desativa punições ativas desse tipo para o alvo
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
    public synchronized void expirePunishmentByName(String name, PunishmentType type, UUID staff, String staffName) {
        if (name == null || name.isEmpty() || type == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE punishments SET active = 0 WHERE LOWER(target_name) = LOWER(?) AND type = ? AND active = 1")) {
            ps.setString(1, name);
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException error) {
            throw storageFailure("revogar punição por nome no SQLite", error);
        }
    }

    @Override
    public synchronized int updateReason(String idPrefix, String reason) {
        PunishmentStorage.validateReasonChange(idPrefix, reason);
        try (PreparedStatement find = connection.prepareStatement("SELECT id FROM punishments WHERE id LIKE ? LIMIT 2")) {
            find.setString(1, idPrefix + "%");
            String id;
            try (ResultSet rows = find.executeQuery()) {
                if (!rows.next()) return 0;
                id = rows.getString(1);
                if (rows.next()) return -1;
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE punishments SET reason = ? WHERE id = ?")) {
                update.setString(1, reason);
                update.setString(2, id);
                return update.executeUpdate();
            }
        } catch (SQLException error) {
            throw storageFailure("alterar motivo no SQLite", error);
        }
    }

    @Override
    public synchronized String getLastIp(UUID target) {
        String sql = "SELECT ip FROM punishments WHERE target = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar último IP no SQLite", e);
        }
        return null;
    }

    @Override
    public synchronized List<String> getAlts(String ip) {
        List<String> alts = new ArrayList<>();
        String sql = "SELECT DISTINCT target_name FROM punishments WHERE ip = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alts.add(rs.getString("target_name"));
                }
            }
        } catch (SQLException e) {
            throw storageFailure("consultar contas por IP no SQLite", e);
        }
        return alts;
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        return new Punishment(
                UUID.fromString(rs.getString("id")),
                rs.getString("target") != null ? UUID.fromString(rs.getString("target")) : null,
                rs.getString("target_name"),
                rs.getString("ip"),
                rs.getString("staff") != null ? UUID.fromString(rs.getString("staff")) : null,
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
