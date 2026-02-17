package co.com.fraudanalyzer.r2dbc.transaction;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FraudAnalysisR2dbcRepository extends ReactiveCrudRepository<FraudAnalysisEntity, String> {
    Mono<Boolean> existsByTransactionId(String transactionId);
}
