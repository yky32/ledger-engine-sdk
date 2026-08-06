package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ItemResult;
import com.altech.ledger.sdk.model.BatchOnboardWalletResponse;
import com.altech.ledger.sdk.model.OnboardWalletRequest;
import com.altech.ledger.sdk.model.WalletOnboardResponse;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Resource API — Phase 1 wallets (UAfinance: 1 customer = 1 LP wallet).
 * <pre>
 * client.wallets().onboard(...);
 * client.wallets().get(userId, "LP");
 * </pre>
 */
public final class WalletApi {
    private final RestLedgerClient rest;
    private final Executor asyncExecutor;

    public WalletApi(RestLedgerClient rest, Executor asyncExecutor) {
        this.rest = Objects.requireNonNull(rest, "rest");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
    }

    public WalletOnboardResponse onboard(OnboardWalletRequest request) {
        return rest.onboardWallet(request);
    }

    public CompletableFuture<WalletOnboardResponse> onboardAsync(OnboardWalletRequest request) {
        return CompletableFuture.supplyAsync(() -> onboard(request), asyncExecutor);
    }

    /**
     * Engine batch endpoint (max 1000). Fail-fast; use {@link #onboardEach} for per-item results.
     */
    public BatchOnboardWalletResponse onboardBatch(List<OnboardWalletRequest> wallets) {
        return rest.onboardWalletsBatch(wallets);
    }

    /**
     * Onboard many wallets with chunking + optional continue-on-error.
     * <ul>
     *   <li>fail-fast + no per-item need: uses engine {@code /wallets/batch} in chunks</li>
     *   <li>continueOnError: one REST call per wallet for precise {@link ItemResult}s</li>
     * </ul>
     */
    public BatchResult<WalletOnboardResponse> onboardEach(List<OnboardWalletRequest> wallets,
                                                          BatchOptions options) {
        Objects.requireNonNull(wallets, "wallets");
        BatchOptions opts = options == null ? BatchOptions.defaults() : options;
        if (wallets.isEmpty()) {
            return BatchResult.empty();
        }

        if (opts.isContinueOnError()) {
            return onboardOneByOne(wallets, opts);
        }

        // fail-fast: use engine batch in chunks for throughput
        List<ItemResult<WalletOnboardResponse>> items = new ArrayList<>();
        int chunk = opts.getEngineBatchSize();
        int index = 0;
        for (int from = 0; from < wallets.size(); from += chunk) {
            int to = Math.min(from + chunk, wallets.size());
            List<OnboardWalletRequest> slice = wallets.subList(from, to);
            BatchOnboardWalletResponse resp = rest.onboardWalletsBatch(slice);
            // Map created + alreadyExists into item results (best-effort ids)
            for (WalletOnboardResponse w : resp.getCreatedWallets()) {
                ItemResult<WalletOnboardResponse> ir =
                    ItemResult.success(index, w.getOwnerId(), w);
                items.add(ir);
                opts.getProgress().onItem(index, wallets.size(), ir);
                index++;
            }
            for (String existing : resp.getAlreadyExistingUserIds()) {
                ItemResult<WalletOnboardResponse> ir =
                    ItemResult.skipped(index, existing, null);
                items.add(ir);
                opts.getProgress().onItem(index, wallets.size(), ir);
                index++;
            }
            // If engine returns fewer rows than requested, pad remaining as success placeholders
            while (index < to) {
                OnboardWalletRequest req = wallets.get(index);
                ItemResult<WalletOnboardResponse> ir =
                    ItemResult.success(index, req.getUserId(), null);
                items.add(ir);
                opts.getProgress().onItem(index, wallets.size(), ir);
                index++;
            }
        }
        BatchResult<WalletOnboardResponse> batch = new BatchResult<>(items);
        opts.getProgress().onComplete(batch);
        return batch;
    }

    private BatchResult<WalletOnboardResponse> onboardOneByOne(List<OnboardWalletRequest> wallets,
                                                               BatchOptions opts) {
        List<ItemResult<WalletOnboardResponse>> items = new ArrayList<>(wallets.size());
        for (int i = 0; i < wallets.size(); i++) {
            OnboardWalletRequest req = wallets.get(i);
            String id = req.getUserId();
            ItemResult<WalletOnboardResponse> ir;
            try {
                WalletOnboardResponse resp = rest.onboardWallet(req);
                ir = ItemResult.success(i, id, resp);
            } catch (LedgerException ex) {
                ir = ItemResult.failure(i, id, ex);
                if (!opts.isContinueOnError()) {
                    items.add(ir);
                    opts.getProgress().onItem(i, wallets.size(), ir);
                    BatchResult<WalletOnboardResponse> partial = new BatchResult<>(items);
                    opts.getProgress().onComplete(partial);
                    throw ex;
                }
            }
            items.add(ir);
            opts.getProgress().onItem(i, wallets.size(), ir);
        }
        BatchResult<WalletOnboardResponse> batch = new BatchResult<>(items);
        opts.getProgress().onComplete(batch);
        return batch;
    }

    public WalletOnboardResponse get(String ownerId, String currency) {
        return rest.getWallet(ownerId, currency);
    }

    public CompletableFuture<WalletOnboardResponse> getAsync(String ownerId, String currency) {
        return CompletableFuture.supplyAsync(() -> get(ownerId, currency), asyncExecutor);
    }

    public List<WalletOnboardResponse> list(String ownerId) {
        return rest.listWallets(ownerId);
    }
}
