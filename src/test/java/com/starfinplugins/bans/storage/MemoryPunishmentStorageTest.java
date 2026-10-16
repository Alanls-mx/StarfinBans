package com.starfinplugins.bans.storage;

import com.starfinplugins.bans.model.Punishment;
import com.starfinplugins.bans.model.PunishmentType;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class MemoryPunishmentStorageTest {
    @Test
    public void reasonChangeAndWarningRemovalPreserveHistory() {
        MemoryPunishmentStorage storage = new MemoryPunishmentStorage();
        UUID player = UUID.randomUUID();
        UUID id = UUID.fromString("12345678-0000-4000-8000-000000000001");
        storage.createPunishment(new Punishment(id, player, "Tester", null, null, "Staff",
            PunishmentType.WARN, "Original", System.currentTimeMillis(), -1, true));
        assertEquals(1, storage.updateReason("12345678", "Corrigido"));
        assertEquals("Corrigido", storage.getHistory(player).get(0).getReason());
        assertEquals(1, storage.getHistoryByName("tester").size());
        storage.expirePunishmentByName("tester", PunishmentType.WARN, null, "Staff");
        assertEquals(1, storage.getHistory(player).size());
        assertFalse(storage.getHistory(player).get(0).isActive());
    }

    @Test
    public void ambiguousPrefixNeverUpdatesEitherPunishment() {
        MemoryPunishmentStorage storage = new MemoryPunishmentStorage();
        UUID player = UUID.randomUUID();
        for (int suffix = 1; suffix <= 2; suffix++)
            storage.createPunishment(new Punishment(UUID.fromString("abcdef12-0000-4000-8000-00000000000" + suffix),
                player, "Tester", null, null, "Staff", PunishmentType.WARN,
                "Original", System.currentTimeMillis(), -1, true));
        assertEquals(-1, storage.updateReason("abcdef12", "Outro"));
        for (Punishment punishment : storage.getHistory(player)) assertEquals("Original", punishment.getReason());
    }
}
