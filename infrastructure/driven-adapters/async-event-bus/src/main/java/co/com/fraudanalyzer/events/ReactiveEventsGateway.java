package co.com.fraudanalyzer.events;

import co.com.fraudanalyzer.model.events.gateways.EventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.api.domain.DomainEventBus;
import org.reactivecommons.async.impl.config.annotations.EnableDomainEventBus;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.logging.Level;

@Log
@RequiredArgsConstructor
@EnableDomainEventBus
public class ReactiveEventsGateway<T> implements EventGateway<T> {

    private final DomainEventBus domainEventBus;

    @Value("${events.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${events.retry.delay-seconds:2}")
    private long retryDelaySeconds;

    @Override
    public Mono<Void> emit(String eventName, String eventId, T payload) {
        log.log(Level.INFO, "Publicando evento: {0} con ID: {1}", new Object[]{eventName, eventId});

        return Mono.from(domainEventBus.emit(new DomainEvent<>(eventName, eventId, payload)))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofSeconds(retryDelaySeconds))
                        .doBeforeRetry(signal -> log.log(Level.WARNING, 
                                "Reintentando publicación del evento {0}. Intento: {1}", 
                                new Object[]{eventName, signal.totalRetries() + 1})))
                .doOnError(error -> log.log(Level.SEVERE, 
                        "Error al publicar evento {0}: {1}", 
                        new Object[]{eventName, error.getMessage()}))
                .then();
    }
}
