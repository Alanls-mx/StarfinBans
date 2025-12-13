package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.md_5.bungee.config.Configuration;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySqlPunishmentStorageBungee implements PunishmentStorage, DupeLookupStorage {

    private final StarfinBansBungeePlugin plugin;
    private final HikariDataSource ds;

    public MySqlPunishmentStorageBungee(StarfinBansBungeePlugin plugin, Configuration cfg) {
        this.plugin = plugin;

        String host = read(cfg, "host", "localhost");
        int port = readInt(cfg, "port", 3306);
        String database = read(cfg, "database", "minecraft");
        String user = read(cfg, "user", "root");
        String password = read(cfg, "password", "");
        boolean useSSL = readBool(cfg, "use-ssl", false);
        int poolSize = readInt(cfg, "pool-size", 10);

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL
                + "&useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        hc.setUsername(user);
        hc.setPassword(password);
        hc.setMaximumPoolSize(poolSize);
        hc.setPoolName("StarfinBansBungee-MySQL");

        this.ds = new HikariDataSource(hc);
    }

    private String read(Configuration cfg, String key, String def) {
        return cfg.getString("database." + key, cfg.getString("mysql." + key, def));
    }

    private int readInt(Configuration cfg, String key, int def) {
        return cfg.getInt("database." + key, cfg.getInt("mysql." + key, def));
    }

    private boolean readBool(Configuration cfg, String key, boolean def) {
        return cfg.getBoolean("database." + key, cfg.getBoolean("mysql." + key, def));
    }

    @Override
    public boolean init() {
        try (Connection conn = ds.getConnection()) {
            createTables(conn);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao iniciar MySQL", e);
            return false;
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    @Override
    public void createPunishment(Punishment p) {
        String sql = "INSERT INTO punishments " +
                "(id,target,target_name,ip,staff,staff_name,type,reason,created_at,expires,active) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = ds.getConnection();
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
    public void expirePunishment(UUID target, PunishmentType type, UUID staff, String staffName) {
        if (target == null || type == null) return;

        String sql = "UPDATE punishments SET active = 0 WHERE target = ? AND type = ? AND active = 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expirePunishment: " + e.getMessage());
        }
    }

    @Override
    public void expirePunishmentByName(String targetName, PunishmentType type, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty() || type == null) return;

        String sql = "UPDATE punishments SET active = 0 " +
                "WHERE lower(target_name) = lower(?) AND type = ? AND active = 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expirePunishmentByName: " + e.getMessage());
        }
    }

    @Override
    public void expireAllPunishmentsByName(String targetName, UUID staff, String staffName) {
        if (targetName == null || targetName.isEmpty()) return;

        String sql = "UPDATE punishments SET active = 0 WHERE lower(target_name) = lower(?) AND active = 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expireAllPunishmentsByName: " + e.getMessage());
        }
    }

    @Override
    public void expireAllPunishmentsByIp(String ip, UUID staff, String staffName) {
        if (ip == null || ip.isEmpty()) return;

        String sql = "UPDATE punishments SET active = 0 WHERE ip = ? AND active = 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL expireAllPunishmentsByIp: " + e.getMessage());
        }
    }

    @Override
    public boolean updateReason(UUID punishmentId, String reason) {
        if (punishmentId == null || reason == null) return false;

        String sql = "UPDATE punishments SET reason = ? WHERE id = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, punishmentId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL updateReason: " + e.getMessage());
        }

        return false;
    }

    @Override
    public Punishment getActiveBan(UUID target) {
        return getActivePunishmentByTarget(target, "BAN");
    }

    @Override
    public Punishment getActiveMute(UUID target) {
        return getActivePunishmentByTarget(target, "MUTE");
    }

    @Override
    public Punishment getActiveIpBan(String ip) {
        return getActivePunishmentByIp(ip, "BAN_IP");
    }

    @Override
    public Punishment getActiveIpMute(String ip) {
        return getActivePunishmentByIp(ip, "MUTE_IP");
    }

    @Override
    public Punishment getActiveBanByName(String targetName) {
        return getActivePunishmentByName(targetName, "BAN");
    }

    @Override
    public Punishment getActiveMuteByName(String targetName) {
        return getActivePunishmentByName(targetName, "MUTE");
    }

    @Override
    public List<Punishment> getHistory(UUID target) {
        if (target == null) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE target = ? ORDER BY created_at DESC";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getHistory: " + e.getMessage());
        }

        return list;
    }

    @Override
    public List<Punishment> getHistoryByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE lower(target_name) = lower(?) ORDER BY created_at DESC";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getHistoryByName: " + e.getMessage());
        }

        return list;
    }

    @Override
    public String getLastIpByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT ip FROM punishments WHERE lower(target_name) = lower(?) " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ip");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getLastIpByName: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<String> getAlts(String ip) {
        if (ip == null || ip.isEmpty()) return Collections.emptyList();

        List<String> alts = new ArrayList<>();
        String sql = "SELECT DISTINCT target_name FROM punishments WHERE ip = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("target_name");
                    if (name != null && !name.isEmpty()) alts.add(name);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getAlts: " + e.getMessage());
        }

        return alts;
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "target VARCHAR(36), " +
                    "target_name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "staff VARCHAR(36), " +
                    "staff_name VARCHAR(16), " +
                    "type VARCHAR(32), " +
                    "reason TEXT, " +
                    "created_at BIGINT, " +
                    "expires BIGINT, " +
                    "active BOOLEAN" +
                    ")");
        }
    }

    private Punishment getActivePunishmentByTarget(UUID target, String prefix) {
        if (target == null) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE target = ? AND active = 1 AND type LIKE ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
            plugin.getLogger().warning("MySQL getActivePunishmentByTarget: " + e.getMessage());
        }

        return null;
    }

    private Punishment getActivePunishmentByName(String targetName, String prefix) {
        if (targetName == null || targetName.isEmpty()) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE lower(target_name) = lower(?) AND active = 1 AND type LIKE ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
            plugin.getLogger().warning("MySQL getActivePunishmentByName: " + e.getMessage());
        }

        return null;
    }

    private Punishment getActivePunishmentByIp(String ip, String prefix) {
        if (ip == null || ip.isEmpty()) return null;

        String sql = "SELECT * FROM punishments " +
                "WHERE ip = ? AND active = 1 AND type LIKE ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, prefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getActivePunishmentByIp: " + e.getMessage());
        }

        return null;
    }

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
