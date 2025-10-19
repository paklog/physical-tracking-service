package com.paklog.wes.tracking.adapter.event;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;

    @InjectMocks
    private TrackingEventPublisher publisher;

    @Captor
    private ArgumentCaptor<CloudEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        CompletableFuture<SendResult<String, CloudEvent>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }

    @Test
    void publishLicensePlateCreated_shouldSendCloudEvent() {
        publisher.publishLicensePlateCreated("LP-1", "WH-1", "PALLET", "tester");

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LP-1"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.licenseplate.created", event.getType());
        assertEquals("LP-1", event.getSubject());
        assertNotNull(event.getId());
        assertNotNull(event.getTime());
        assertEquals("application/json", event.getDataContentType());
        assertNotNull(event.getData());
        assertTrue(new String(event.getData().toBytes()).contains("\"licensePlateId\":\"LP-1\""));
    }

    @Test
    void publishLicensePlateMoved_shouldHandleNullFromLocation() {
        publisher.publishLicensePlateMoved("LP-2", null, "LOC-2", "PUTAWAY", "worker");

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LP-2"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.licenseplate.moved", event.getType());
        String payload = new String(event.getData().toBytes());
        assertTrue(payload.contains("\"fromLocationId\":\"\""));
        assertTrue(payload.contains("\"toLocationId\":\"LOC-2\""));
        assertTrue(payload.contains("\"performedBy\":\"worker\""));
    }

    @Test
    void publishItemAdded_shouldIncludeLocationId() {
        publisher.publishItemAdded("LP-3", "SKU-1", 4, "LOC-5");

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LP-3"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.item.added", event.getType());
        String payload = new String(event.getData().toBytes());
        assertTrue(payload.contains("\"sku\":\"SKU-1\""));
        assertTrue(payload.contains("\"quantity\":\"4\""));
        assertTrue(payload.contains("\"locationId\":\"LOC-5\""));
    }

    @Test
    void publishItemRemoved_shouldHandleNullLocation() {
        publisher.publishItemRemoved("LP-4", "SKU-9", 2, null);

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LP-4"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.item.removed", event.getType());
        assertTrue(new String(event.getData().toBytes()).contains("\"locationId\":\"\""));
    }

    @Test
    void publishLocationBlocked_shouldSendEvent() {
        publisher.publishLocationBlocked("LOC-7", "WH-1", "incident");

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LOC-7"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.location.blocked", event.getType());
        assertTrue(new String(event.getData().toBytes()).contains("\"reason\":\"incident\""));
    }

    @Test
    void publishLocationUnblocked_shouldSendEvent() {
        publisher.publishLocationUnblocked("LOC-8", "WH-1");

        verify(kafkaTemplate).send(eq("tracking-events"), eq("LOC-8"), eventCaptor.capture());
        CloudEvent event = eventCaptor.getValue();

        assertEquals("com.paklog.tracking.location.unblocked", event.getType());
        assertTrue(new String(event.getData().toBytes()).contains("\"warehouseId\":\"WH-1\""));
    }

    @Test
    void publishEvent_shouldHandleExceptions() {
        when(kafkaTemplate.send(eq("tracking-events"), eq("LOC-ERR"), any()))
            .thenThrow(new RuntimeException("failure"));

        assertDoesNotThrow(() -> publisher.publishLocationUnblocked("LOC-ERR", "WH-ERR"));
        verify(kafkaTemplate).send(eq("tracking-events"), eq("LOC-ERR"), any());
    }
}
