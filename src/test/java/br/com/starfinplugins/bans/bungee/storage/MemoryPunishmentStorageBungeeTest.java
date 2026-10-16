package br.com.starfinplugins.bans.bungee.storage;

import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class MemoryPunishmentStorageBungeeTest {
    @Test
    public void reasonChangeAndWarningRemovalByNamePreserveHistory() {
        MemoryPunishmentStorageBungee storage = new MemoryPunishmentStorageBungee();
        UUID id = UUID.fromString("12345678-0000-4000-8000-000000000001");
        storage.createPunishment(new Punishment(id, null, "Tester", null, null, "Staff",
            PunishmentType.WARN, "Original", System.currentTimeMillis(), -1, true));
        assertEquals(1, storage.updateReason("12345678", "Corrigido"));
        assertEquals("Corrigido", storage.getHistoryByName("tester").get(0).getReason());
        storage.expirePunishmentByName("tester", PunishmentType.WARN, null, "Staff");
        assertEquals(1, storage.getHistoryByName("Tester").size());
        assertFalse(storage.getHistoryByName("Tester").get(0).isActive());
    }
}
