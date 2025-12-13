package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLitePunishmentStorageBungee implements PunishmentStorage, DupeLookupStorage {

    private final StarfinBansBungeePlugin plugin;
    private Connection connection;

    public SQLitePunishmentStorageBungee(StarfinBansBungeePlugin plugin) {
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
            plugin.getLogger().severe("Falha ao iniciar SQLite: " + e.getMessage());
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
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void createPunishment(Punishment p) {
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
            plugin.getLogger().warning("SQLite createPunishment: " + e.getMessage());
        }
    }

    // ---------------- ACTIVE (UUID) ----------------

    @Override
    public Punishment getActiveBan(UUID target) {
        return getActivePunishmentByTarget(target, "BAN");
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return getActivePunishmentByTarget(target, "MUTE");
    }

    private Punishment getActivePunishmentByTarget(UUID target, String prefix) {
        if (target == null) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE target = ? AND active = 1 AND type LIKE ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, prefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = map(rs);

                    if ("BAN".equals(prefix) && isIpBan(p.getType())) return null;
                    if ("MUTE".equals(prefix) && isIpMute(p.getType())) return null;

                    return p;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getActivePunishmentByTarget: " + e.getMessage());
        }

        return null;
    }

    // ---------------- ACTIVE (NAME) ----------------

    @Override
    public Punishment getActiveBanByName(String targetName) {
        return getActivePunishmentByName(targetName, "BAN");
    }

    @Override
    public Punishment getActiveMuteByName(String targetName) {
        return getActivePunishmentByName(targetName, "MUTE");
    }

    private Punishment getActivePunishmentByName(String targetName, String prefix) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE lower(target_name) = lower(?) AND active = 1 AND type LIKE ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.setString(2, prefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = map(rs);

                    if ("BAN".equals(prefix) && isIpBan(p.getType())) return null;
                    if ("MUTE".equals(prefix) && isIpMute(p.getType())) return null;

                    return p;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getActivePunishmentByName: " + e.getMessage());
        }

        return null;
    }

    // ---------------- HISTORY ----------------

    @Override
    public List<Punishment> getHistory(UUID target) {
        if (target == null) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE target = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getHistory: " + e.getMessage());
        }

        return list;
    }

    @Override
    public List<Punishment> getHistoryByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE lower(target_name) = lower(?) ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getHistoryByName: " + e.getMessage());
        }

        return list;
    }

    // ---------------- EXPIRE ----------------

    @Override
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;

        String sql = "UPDATE punishments SET active = 0 WHERE target = ? AND type = ? AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite expirePunishment: " + e.getMessage());
        }
    }

    @Override
    public void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty() || type == null) return;

        String sql = "UPDATE punishments SET active = 0 " +
                "WHERE lower(target_name) = lower(?) AND type = ? AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite expirePunishmentByName: " + e.getMessage());
        }
    }

    @Override
    public void expireAllPunishmentsByName(String targetName, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty()) return;

        String sql = "UPDATE punishments SET active = 0 WHERE lower(target_name) = lower(?) AND active = 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite expireAllPunishmentsByName: " + e.getMessage());
        }
    }

    // ---------------- DUPE LOOKUP (extra, sem quebrar interface) ----------------

    @Override
    public String getLastIpByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT ip FROM punishments WHERE lower(target_name) = lower(?) " +
                "ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ip");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getLastIpByName: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<String> getAlts(String ip) {
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
            plugin.getLogger().warning("SQLite getAlts: " + e.getMessage());
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

    private boolean isIpBan(PunishmentType t) {
        return t == PunishmentType.BAN_IP_PERMANENT || t == PunishmentType.BAN_IP_TEMPORARY;
    }

    private boolean isIpMute(PunishmentType t) {
        return t == PunishmentType.MUTE_IP_PERMANENT || t == PunishmentType.MUTE_IP_TEMPORARY;
    }
}
