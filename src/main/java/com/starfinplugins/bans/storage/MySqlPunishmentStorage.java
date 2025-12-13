package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MySqlPunishmentStorage implements PunishmentStorage {

    private final StarfinBansPlugin plugin;
    private HikariDataSource dataSource;

    public MySqlPunishmentStorage(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            FileConfiguration cfg = plugin.getConfig();
            String host = cfg.getString("database.host", "localhost");
            int port = cfg.getInt("database.port", 3306);
            String database = cfg.getString("database.database", "minecraft");
            String user = cfg.getString("database.user", "root");
            String password = cfg.getString("database.password", "");
            boolean useSSL = cfg.getBoolean("database.use-ssl", false);
            int poolSize = cfg.getInt("database.pool-size", 10);

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + useSSL
                    + "&useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";

            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(url);
            hc.setUsername(user);
            hc.setPassword(password);
            hc.setMaximumPoolSize(poolSize);
            hc.setPoolName("StarfinBans-MySQL");

            dataSource = new HikariDataSource(hc);

            try (Connection connection = dataSource.getConnection()) {
                createTables(connection);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao iniciar MySQL Storage!", e);
        }
    }

    private void createTables(Connection connection) {
        String sql = "CREATE TABLE IF NOT EXISTS punishments ("
                + "id VARCHAR(36) PRIMARY KEY, "
                + "target VARCHAR(36), "
                + "target_name VARCHAR(16), "
                + "ip VARCHAR(45), "
                + "staff VARCHAR(36), "
                + "staff_name VARCHAR(16), "
                + "type VARCHAR(32), "
                + "reason TEXT, "
                + "created_at BIGINT, "
                + "expires BIGINT, "
                + "active BOOLEAN"
                + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao criar tabelas no MySQL", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void createPunishment(Punishment p) {
        String sql = "INSERT INTO punishments (id, target, target_name, ip, staff, staff_name, type, reason, created_at, expires, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
            plugin.getLogger().warning("MySQL createPunishment: " + e.getMessage());
        }
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return getActivePunishment(target, "BAN");
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return getActiveIpPunishment(ip, "BAN_IP");
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return getActivePunishment(target, "MUTE");
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return getActiveIpPunishment(ip, "MUTE_IP");
    }

    private Punishment getActivePunishment(UUID target, String typePrefix) {
        if (target == null) return null;

        String sql = "SELECT * FROM punishments WHERE target = ? AND active = 1 AND type LIKE ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, typePrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = mapPunishment(rs);
                    if (typePrefix.equals("BAN") && p.getType().isIpBan()) return null;
                    if (typePrefix.equals("MUTE") && p.getType().isIpMute()) return null;
                    return p;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getActivePunishment: " + e.getMessage());
        }
        return null;
    }

    private Punishment getActiveIpPunishment(String ip, String typePrefix) {
        if (ip == null || ip.isEmpty()) return null;

        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 AND type LIKE ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, typePrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPunishment(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getActiveIpPunishment: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        List<Punishment> list = new ArrayList<>();
        if (target == null) return list;

        String sql = "SELECT * FROM punishments WHERE target = ? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getHistory: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;

        String sql = "UPDATE punishments SET active = 0 WHERE target = ? AND type = ? AND active = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expirePunishment: " + e.getMessage());
        }
    }

    @Override
    public void expireAllPunishmentsByIp(String ip, UUID staff, String staffName) {
        if (ip == null || ip.isEmpty()) return;

        String sql = "UPDATE punishments SET active = 0 WHERE ip = ? AND active = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expireAllPunishmentsByIp: " + e.getMessage());
        }
    }

    @Override
    public String getLastIp(UUID target) {
        if (target == null) return null;

        String sql = "SELECT ip FROM punishments WHERE target = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getLastIp: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<String> getAlts(String ip) {
        List<String> alts = new ArrayList<>();
        if (ip == null || ip.isEmpty()) return alts;

        String sql = "SELECT DISTINCT target_name FROM punishments WHERE ip = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alts.add(rs.getString("target_name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getAlts: " + e.getMessage());
        }
        return alts;
    }

    @Override
    public boolean updateReason(UUID punishmentId, String reason) {
        if (punishmentId == null || reason == null) return false;

        String sql = "UPDATE punishments SET reason = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, punishmentId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL updateReason: " + e.getMessage());
        }
        return false;
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
}