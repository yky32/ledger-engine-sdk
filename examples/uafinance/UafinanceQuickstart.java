package examples.uafinance;

import com.altech.ledger.sdk.LedgerAuthException;
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerConflictException;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerNotFoundException;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.OnboardWalletRequest;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.model.WalletOnboardResponse;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Reference snippet for UAfinance integration (copy into your service).
 * Not compiled as part of the SDK module — documentation only.
 *
 * <pre>
 * // Maven
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.altech&lt;/groupId&gt;
 *   &lt;artifactId&gt;ledger-engine-sdk&lt;/artifactId&gt;
 *   &lt;version&gt;0.2.0-SNAPSHOT&lt;/version&gt;
 * &lt;/dependency&gt;
 * // Optional Kafka:
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.apache.kafka&lt;/groupId&gt;
 *   &lt;artifactId&gt;kafka-clients&lt;/artifactId&gt;
 *   &lt;version&gt;3.8.1&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
public final class UafinanceQuickstart {
    private UafinanceQuickstart() {}

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("LEDGER_BASE_URL", "http://localhost:8080");
        String token = System.getenv("LEDGER_TOKEN"); // optional until engine enforces auth

        try (LedgerClient client = LedgerClient.create(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .bearerToken(token)
            .defaultCurrency("LP")
            .defaultExternalType("uafinance")
            .build())) {

            // Phase 1 — 1 customer = 1 LP wallet
            String userId = "UAF-10001";
            WalletOnboardResponse wallet = client.onboardWallet(OnboardWalletRequest.builder()
                .userId(userId)
                .name("Member 10001")
                .externalId(userId)
                .build());
            System.out.println("Wallet ready: " + wallet.getWalletId() + " " + wallet.getCurrency());

            // Phase 2 — POS / order event → engine rule → LP
            TransactionalEvent event = TransactionalEvent.builder()
                .eventId("pos-20260806-001") // also used as Idempotency-Key
                .userId(userId)
                .eventType("PURCHASE")
                .amount(new BigDecimal("150.00"))
                .currency("LP")
                .occurredAt(Instant.now())
                .build();

            IngestionResult result = client.ingestRest(event);
            System.out.println("Ingest: " + result);

            WalletOnboardResponse after = client.getWallet(userId, "LP");
            System.out.println("Balance: " + after.getBalance());

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
