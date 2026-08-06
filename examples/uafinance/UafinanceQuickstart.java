package examples.uafinance;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerAuthException;
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerConflictException;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerNotFoundException;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ProgressListener;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.OnboardWalletRequest;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.model.WalletOnboardResponse;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Reference snippet for UAfinance integration (copy into your service).
 * Not compiled as part of the SDK module — documentation only.
 *
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.altech&lt;/groupId&gt;
 *   &lt;artifactId&gt;ledger-engine-sdk&lt;/artifactId&gt;
 *   &lt;version&gt;1.1.0&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
public final class UafinanceQuickstart {
    private UafinanceQuickstart() {}

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("LEDGER_BASE_URL", "http://localhost:8080");
        String token = System.getenv("LEDGER_TOKEN");

        try (LedgerClient client = LedgerClient.create(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .bearerToken(token)
            .defaultCurrency("LP")
            .defaultExternalType("uafinance")
            .build())) {

            String userId = "UAF-10001";

            // Phase 1 — resource API
            WalletOnboardResponse wallet = client.wallets().onboard(OnboardWalletRequest.builder()
                .userId(userId)
                .name("Member 10001")
                .externalId(userId)
                .build());
            System.out.println("Wallet ready: " + wallet.getWalletId());

            // Phase 2 — single event
            IngestionResult result = client.events().submit(TransactionalEvent.builder()
                .eventId("pos-20260806-001")
                .userId(userId)
                .eventType("PURCHASE")
                .amount(new BigDecimal("150.00"))
                .currency("LP")
                .occurredAt(Instant.now())
                .build());
            System.out.println("Ingest: " + result);

            // Optional: stream a large NDJSON file (70K-style)
            String batchFile = System.getenv("LEDGER_BATCH_FILE");
            if (batchFile != null && !batchFile.isBlank()) {
                BatchResult<IngestionResult> batch = client.files().process(
                    Path.of(batchFile),
                    DeliveryChannel.REST,
                    BatchOptions.builder()
                        .continueOnError(true)
                        .progress(ProgressListener.loggingEvery(500))
                        .build());
                System.out.println("File batch: " + batch);
            }

            System.out.println("Balance: " + client.wallets().get(userId, "LP").getBalance());

        } catch (LedgerValidationException ex) {
            System.err.println("Bad request: " + ex.getCode() + " fields=" + ex.getFieldErrors());
        } catch (LedgerNotFoundException ex) {
            System.err.println("Missing wallet/user: " + ex.getMessage());
        } catch (LedgerConflictException ex) {
            System.err.println("Conflict (duplicate?): " + ex.getCode());
        } catch (LedgerAuthException ex) {
            System.err.println("Auth failed — check LEDGER_TOKEN: " + ex.getMessage());
        } catch (LedgerException ex) {
            System.err.println("Ledger error http=" + ex.getHttpStatus()
                + " requestId=" + ex.getRequestId() + " : " + ex.getMessage());
        }
    }
}
