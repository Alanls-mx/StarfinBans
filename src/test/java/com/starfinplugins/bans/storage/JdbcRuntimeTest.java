package com.starfinplugins.bans.storage;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcRuntimeTest {

    @Test
    public void sqlitePersistsDataAcrossConnections() throws Exception {
        Path database = Files.createTempFile("starfinbans-", ".db");
        try {
            String url = "jdbc:sqlite:" + database.toAbsolutePath();
            try (Connection connection = DriverManager.getConnection(url);
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE test (id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
                statement.executeUpdate("INSERT INTO test (value) VALUES ('persisted')");
            }

            try (Connection connection = DriverManager.getConnection(url);
                 PreparedStatement statement = connection.prepareStatement("SELECT value FROM test WHERE id = 1");
                 ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("persisted", result.getString(1));
            }
        } finally {
            Files.deleteIfExists(database);
        }
    }

    @Test
    public void mysqlDriverAndPoolAreLoadableOnJava8() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Class.forName("com.zaxxer.hikari.HikariDataSource");
    }
}
