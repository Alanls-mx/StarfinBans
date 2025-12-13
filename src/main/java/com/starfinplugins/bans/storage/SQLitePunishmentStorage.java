package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.StarfinBansPlugin;
import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLitePunishmentStorage implements PunishmentStorage {

    private final StarfinBansPlugin plugin;
    private Connection connection;

    public SQLitePunishmentStorage(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
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
            e.printStackTrace();
            plugin.getLogger().severe("Falha ao iniciar SQLite Storage!");
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
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createPunishment(Punishment p) {
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
            e.printStackTrace();
        }
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return getActivePunishment(target, "BAN");
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return getActivePunishment(target, "MUTE");
    }

    private Punishment getActivePunishment(UUID target, String typePrefix) {
        String sql = "SELECT * FROM punishments WHERE target = ? AND active = 1 AND type LIKE ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, typePrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = mapPunishment(rs);
                    // Filtrar IP bans aqui se o SQL pegar (mas typePrefix 'BAN%' pega BAN_IP... então precisamos filtrar no Java ou melhorar SQL)
                    // O método getActiveBan pede banimento DE JOGADOR.
                    if (typePrefix.equals("BAN") && p.getType().isIpBan()) return null;
                    if (typePrefix.equals("MUTE") && p.getType().isIpMute()) return null;
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return getActiveIpPunishment(ip, "BAN_IP");
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return getActiveIpPunishment(ip, "MUTE_IP");
    }

    private Punishment getActiveIpPunishment(String ip, String typePrefix) {
        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 AND type LIKE ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, typePrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPunishment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
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
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        // Desativa punições ativas desse tipo para o alvo
        String sql = "UPDATE punishments SET active = 0 WHERE target = ? AND type = ? AND active = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getLastIp(UUID target) {
        String sql = "SELECT ip FROM punishments WHERE target = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getAlts(String ip) {
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
            e.printStackTrace();
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
}