package com.altech.ledger.sdk.batch;

import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchResultTest {

    @Test
    void aggregatesAndThrowSummary() {
        BatchResult<String> batch = new BatchResult<>(List.of(
            ItemResult.success(0, "a", "ok"),
            ItemResult.failure(1, "b", new LedgerValidationException("bad")),
            ItemResult.skipped(2, "c", null)
        ));
        assertEquals(1, batch.successCount());
        assertEquals(1, batch.failureCount());
        assertEquals(1, batch.skippedCount());
        assertTrue(batch.hasFailures());
        assertEquals(List.of("ok"), batch.successes());
        assertThrows(LedgerException.class, batch::throwSummaryIfAnyFailed);
        assertThrows(LedgerValidationException.class, batch::throwIfAnyFailed);
    }
}
