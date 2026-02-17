package co.com.fraudanalyzer.usecase.fraudanalysis;

import co.com.fraudanalyzer.model.events.config.EventTopics;
import co.com.fraudanalyzer.model.events.gateways.EventGateway;
import co.com.fraudanalyzer.model.transaction.Transaction;
import co.com.fraudanalyzer.model.transaction.gateways.FraudAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

@Log
@RequiredArgsConstructor
public class FraudAnalysisUseCase {

    public static final int LIMIT_AMOUNT = 10000;
    public static final int CHAOS_AMOUNT = 9999;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final EventGateway<Transaction> eventGateway;
    private final EventTopics eventTopics;

    public Mono<Void> analyze(Transaction transaction) {
        return fraudAnalysisRepository.existsByTransactionId(transaction.getId())
                .flatMap(exists -> exists ? Mono.empty() : analyzeAndSave(transaction));
    }

    private Mono<Void> analyzeAndSave(Transaction transaction) {
        return Mono.fromCallable(() -> {
            if (transaction.getAmount() == CHAOS_AMOUNT) {
                log.warning("CHAOS MODE: Simulating DB failure for amount=" + CHAOS_AMOUNT);
                throw new RuntimeException("Simulando fallo de DB");
            }
            
            if (transaction.getAmount() > LIMIT_AMOUNT) {
                return new AnalysisResult(transaction.toBuilder().status("REJECTED").build(), "Amount exceeds limit");
            } else {
                return new AnalysisResult(transaction.toBuilder().status("APPROVED").build(), "Normal transaction");
            }
        })
        .flatMap(result -> fraudAnalysisRepository.save(result.transaction, result.reason)
                .flatMap(saved -> eventGateway.emit(eventTopics.getFraudResult(), saved.getId(), saved)))
        .then();
    }

    private record AnalysisResult(Transaction transaction, String reason) {}
}
