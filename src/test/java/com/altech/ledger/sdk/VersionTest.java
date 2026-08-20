package com.altech.ledger.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    @Test
    void parseAndCompare() {
        assertTrue(Version.parse("1.2.0").isAtLeast(Version.parse("1.2.0")));
        assertTrue(Version.parse("1.2.1").isAtLeast(Version.parse("1.2.0")));
        assertFalse(Version.parse("1.1.9").isAtLeast(Version.parse("1.2.0")));
        assertEquals("1.2.0", Version.parse("v1.2.0-SNAPSHOT").toString());
    }

    @Test
    void sdkVersionsConstant() {
        assertEquals("1.2.0", SdkVersions.VERSION);
    }
}
