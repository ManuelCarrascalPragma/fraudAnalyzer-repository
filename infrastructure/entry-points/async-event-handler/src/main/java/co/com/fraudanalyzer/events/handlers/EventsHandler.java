package co.com.fraudanalyzer.events.handlers;

import co.com.fraudanalyzer.model.transaction.Transaction;
import co.com.fraudanalyzer.usecase.fraudanalysis.FraudAnalysisUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.async.api.HandlerRegistry;
import org.reactivecommons.async.impl.config.annotations.EnableMessageListeners;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Log
@Component
@RequiredArgsConstructor
@EnableMessageListeners
public class EventsHandler {
    
    private final FraudAnalysisUseCase fraudAnalysisUseCase;
    private final KafkaTemplate<String, Object> dlqKafkaTemplate;
    
    @Value("${dlq.topic:shieldflow.transaction.received.dlq}")
    private String dlqTopic;

    @Bean
    @Primary
    public HandlerRegistry eventSubscription() {
        return HandlerRegistry.register()
                .listenEvent(
                        "shieldflow.transaction.received",
                        event -> fraudAnalysisUseCase.analyze(event.getData())
                                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                                        .doBeforeRetry(signal -> 
                                            log.warning("Retry attempt " + (signal.totalRetries() + 1) + 
                                                      " for transaction: " + event.getData().getId())))
                                .onErrorResume(error -> {
                                    log.severe("Max retries exhausted for: " + event.getData().getId());
                                    return sendToDLQ(event.getData(), error);
                                }),
                        Transaction.class
                );
    }
    
    private Mono<Void> sendToDLQ(Transaction transaction, Throwable error) {
        return Mono.fromRunnable(() -> {
            try {
                dlqKafkaTemplate.send(dlqTopic, transaction.getId(), transaction);
                log.info("✅ Message sent to DLQ: " + transaction.getId());
            } catch (Exception e) {
                log.severe("❌ Failed to send to DLQ: " + e.getMessage());
            }
        });
    }
}
