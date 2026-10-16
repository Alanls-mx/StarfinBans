package com.starfinplugins.bans.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

    @Test
    public void parsesCompoundDurations() {
        assertEquals(93_600_000L, TimeUtil.parseDuration("1d2h"));
        assertEquals(5_400_000L, TimeUtil.parseDuration("1h30m"));
    }

    @Test
    public void rejectsPartialOrInvalidDurations() {
        assertEquals(-1L, TimeUtil.parseDuration("abc1d"));
        assertEquals(-1L, TimeUtil.parseDuration("1dgarbage"));
        assertEquals(-1L, TimeUtil.parseDuration("2x"));
        assertEquals(-1L, TimeUtil.parseDuration(""));
        assertEquals(-1L, TimeUtil.parseDuration(null));
    }

    @Test
    public void rejectsOverflow() {
        assertEquals(-1L, TimeUtil.parseDuration("999999999999999999999d"));
    }

    @Test
    public void normalizesMessagesForLegacyMinecraftFonts() {
        assertEquals("&cVoce esta banido!", MessageUtil.normalizeLegacyText("&cVocê está banido!"));
        assertEquals("Server  status", MessageUtil.normalizeLegacyText("Server ✓ status"));
    }
}
