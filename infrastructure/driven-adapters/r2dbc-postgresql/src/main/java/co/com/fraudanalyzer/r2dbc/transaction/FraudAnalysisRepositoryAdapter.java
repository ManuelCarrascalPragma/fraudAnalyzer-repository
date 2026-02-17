package co.com.fraudanalyzer.r2dbc.transaction;

import co.com.fraudanalyzer.model.transaction.Transaction;
import co.com.fraudanalyzer.model.transaction.gateways.FraudAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class FraudAnalysisRepositoryAdapter implements FraudAnalysisRepository {
    private final FraudAnalysisR2dbcRepository r2dbcRepository;

    @Override
    public Mono<Transaction> save(Transaction transaction, String reason) {
        return r2dbcRepository.findById(transaction.getId())
                .flatMap(existing -> {
                    // Ya existe, actualizar
                    FraudAnalysisEntity updated = FraudAnalysisEntity.builder()
                            .transactionId(transaction.getId())
                            .accountId(transaction.getAccountId())
                            .amount(transaction.getAmount())
                            .currency(transaction.getCurrency())
                            .status(transaction.getStatus())
                            .reason(reason)
                            .analyzedAt(existing.getAnalyzedAt())
                            .isNew(false)
                            .build();
                    return r2dbcRepository.save(updated);
                })
                .switchIfEmpty(
                    // No existe, crear nuevo
                    r2dbcRepository.save(FraudAnalysisEntity.fromDomain(transaction, reason))
                )
                .map(FraudAnalysisEntity::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTransactionId(String transactionId) {
        return r2dbcRepository.existsByTransactionId(transactionId);
    }
}
