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

public class MySqlPunishmentStorageBungee implements PunishmentStorage {

    private final StarfinBansBungeePlugin plugin;
    private final HikariDataSource ds;

    public MySqlPunishmentStorageBungee(StarfinBansBungeePlugin plugin, Configuration cfg) {
        this.plugin = plugin;

        String url = "jdbc:mysql://" + cfg.getString("mysql.host") + ":" +
                cfg.getInt("mysql.port") + "/" +
                cfg.getString("mysql.database") +
                "?useSSL=" + cfg.getBoolean("mysql.use-ssl", false);

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        hc.setUsername(cfg.getString("mysql.user"));
        hc.setPassword(cfg.getString("mysql.password"));
        hc.setMaximumPoolSize(cfg.getInt("mysql.pool-size", 10));

        this.ds = new HikariDataSource(hc);
    }

    @Override
    public void init() {}

    @Override
    public void close() {
        ds.close();
    }

    // ⚠️ Demais métodos seguem sua interface PunishmentStorage
}
