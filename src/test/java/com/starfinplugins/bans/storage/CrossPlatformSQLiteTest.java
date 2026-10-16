package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.SQLitePunishmentStorageBungee;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.Assert.*;

public class CrossPlatformSQLiteTest {
    @Test
    public void bothAdaptersShareReasonsWarningsAndHistory() throws Exception {
        Path directory = Files.createTempDirectory("starfinbans-shared-");
        Path database = directory.resolve("database.db");
        Logger logger = Logger.getLogger("starfinbans-test");
        SQLitePunishmentStorage spigot = new SQLitePunishmentStorage(database.toFile(), logger);
        SQLitePunishmentStorageBungee bungee = new SQLitePunishmentStorageBungee(database.toFile(), logger);
        try {
            spigot.init();
            bungee.init();
            UUID player = UUID.randomUUID();
            UUID id = UUID.fromString("12345678-0000-4000-8000-000000000001");
            spigot.createPunishment(new Punishment(id, player, "Tester", null, null, "Staff",
                PunishmentType.WARN, "Original", System.currentTimeMillis(), -1, true));
            assertEquals("Original", bungee.getHistoryByName("tester").get(0).getReason());
            assertEquals(1, bungee.updateReason("12345678", "Corrigido"));
            assertEquals("Corrigido", spigot.getHistoryByName("TESTER").get(0).getReason());
            spigot.expirePunishmentByName("Tester", PunishmentType.WARN, null, "Staff");
            assertFalse(bungee.getHistoryByName("Tester").get(0).isActive());
            UUID proxyId = UUID.fromString("87654321-0000-4000-8000-000000000001");
            bungee.createPunishment(new br.com.starfinplugins.bans.bungee.model.Punishment(proxyId,
                null, "Tester", null, null, "Staff", br.com.starfinplugins.bans.bungee.model.PunishmentType.WARN,
                "Proxy", System.currentTimeMillis(), -1, true));
            assertEquals(2, spigot.getHistoryByName("Tester").size());
            spigot.expirePunishmentByName("Tester", PunishmentType.WARN, null, "Staff");
            assertFalse(bungee.getHistoryByName("Tester").stream().anyMatch(p -> p.isActive()));
            UUID second = UUID.fromString("12345678-0000-4000-8000-000000000002");
            spigot.createPunishment(new Punishment(second, player, "Tester", null, null, "Staff",
                PunishmentType.WARN, "Segundo", System.currentTimeMillis(), -1, true));
            assertEquals(-1, spigot.updateReason("12345678", "Ambiguo"));
            assertTrue(bungee.getHistoryByName("Tester").stream()
                .anyMatch(p -> p.getId().equals(second) && "Segundo".equals(p.getReason())));
        } finally {
            bungee.close();
            spigot.close();
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }
    }
}
