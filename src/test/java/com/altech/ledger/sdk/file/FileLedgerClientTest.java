package com.altech.ledger.sdk.file;

import com.altech.ledger.sdk.model.TransactionalEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileLedgerClientTest {

    @TempDir
    Path dir;

    @Test
    void readsNdjsonAndArray() throws Exception {
        Path ndjson = dir.resolve("events.ndjson");
        Files.writeString(ndjson, """
            {"eventId":"e1","userId":"U1","eventType":"PURCHASE","amount":10,"currency":"LP"}
            {"eventId":"e2","userId":"U2","eventType":"PURCHASE","amount":20,"currency":"LP"}
            """);
        List<TransactionalEvent> a = FileLedgerClient.parse(ndjson);
        assertEquals(2, a.size());
        assertEquals("e1", a.get(0).getEventId());

        Path arr = dir.resolve("events.json");
        Files.writeString(arr, """
            [
              {"eventId":"e3","userId":"U3","eventType":"SIGNUP","amount":0,"currency":"LP"}
            ]
            """);
        List<TransactionalEvent> b = FileLedgerClient.parse(arr);
        assertEquals(1, b.size());
        assertEquals("SIGNUP", b.get(0).getEventType());
    }
}
