package co.com.fraudanalyzer.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.api.domain.DomainEventBus;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReactiveEventsGatewayTest {

    @Mock
    private DomainEventBus domainEventBus;

    private ReactiveEventsGateway<String> reactiveEventsGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reactiveEventsGateway = new ReactiveEventsGateway<>(domainEventBus);
        ReflectionTestUtils.setField(reactiveEventsGateway, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(reactiveEventsGateway, "retryDelaySeconds", 1L);
    }

    @Test
    void shouldEmitEventSuccessfully() {
        when(domainEventBus.emit(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(reactiveEventsGateway.emit("test.event", "123", "test-payload"))
                .verifyComplete();

        verify(domainEventBus, times(1)).emit(any(DomainEvent.class));
    }

    @Test
    void shouldCaptureCorrectDomainEvent() {
        when(domainEventBus.emit(any(DomainEvent.class))).thenReturn(Mono.empty());

        reactiveEventsGateway.emit("test.event", "event-123", "test-data").block();

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventBus).emit(eventCaptor.capture());

        DomainEvent capturedEvent = eventCaptor.getValue();
        assertEquals("test.event", capturedEvent.getName());
        assertEquals("event-123", capturedEvent.getEventId());
        assertEquals("test-data", capturedEvent.getData());
    }

    @Test
    void shouldRetryOnFailure() {
        when(domainEventBus.emit(any(DomainEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")))
                .thenReturn(Mono.empty());

        StepVerifier.create(reactiveEventsGateway.emit("test.event", "123", "payload"))
                .verifyComplete();

        verify(domainEventBus, times(2)).emit(any(DomainEvent.class));
    }
}
