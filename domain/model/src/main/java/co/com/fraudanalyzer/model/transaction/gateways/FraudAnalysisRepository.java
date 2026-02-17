package co.com.fraudanalyzer.model.transaction.gateways;

import co.com.fraudanalyzer.model.transaction.Transaction;
import reactor.core.publisher.Mono;

public interface FraudAnalysisRepository {
    Mono<Transaction> save(Transaction transaction, String reason);
    Mono<Boolean> existsByTransactionId(String transactionId);
}
