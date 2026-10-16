package br.com.starfinplugins.bans.bungee.service;

import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.model.PunishmentType;
import br.com.starfinplugins.bans.bungee.storage.MemoryPunishmentStorageBungee;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class OnlinePunishmentServiceTest {
    @Test
    public void chatSnapshotReadsMemoryOnlyAndReflectsNextRefresh() {
        UUID player = UUID.randomUUID();
        CountingStorage storage = new CountingStorage();
        OnlinePunishmentService service = new OnlinePunishmentService(null, storage);
        assertNull(service.snapshot(player));
        service.prime(player, "Tester", "127.0.0.1");
        assertNotNull(service.snapshot(player));
        assertNull(service.snapshot(player).getMute());

        storage.createPunishment(new Punishment(UUID.randomUUID(), player, "Tester", null,
                null, "Staff", PunishmentType.MUTE_PERMANENT, "Spam",
                System.currentTimeMillis(), -1L, true));
        int readsBeforeChat = storage.reads;
        assertNull(service.snapshot(player).getMute());
        assertEquals(readsBeforeChat, storage.reads);

        service.prime(player, "Tester", "127.0.0.1");
        assertEquals("Spam", service.snapshot(player).getMute().getReason());
        service.forget(player);
        assertNull(service.snapshot(player));
    }

    private static final class CountingStorage extends MemoryPunishmentStorageBungee {
        private int reads;

        @Override public Punishment getActiveMute(UUID player) {
            reads++;
            return super.getActiveMute(player);
        }

        @Override public Punishment getActiveMuteByName(String name) {
            reads++;
            return super.getActiveMuteByName(name);
        }

        @Override public Punishment getActiveIpMute(String ip) {
            reads++;
            return super.getActiveIpMute(ip);
        }
    }
}
