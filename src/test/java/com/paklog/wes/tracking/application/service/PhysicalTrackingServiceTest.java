package com.paklog.wes.tracking.application.service;

import com.paklog.wes.tracking.adapter.event.TrackingEventPublisher;
import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.repository.LicensePlateRepository;
import com.paklog.wes.tracking.domain.repository.LocationStateRepository;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PhysicalTrackingServiceTest {

    @Mock
    private LicensePlateRepository licensePlateRepository;
    @Mock
    private LocationStateRepository locationStateRepository;
    @Mock
    private TrackingEventPublisher eventPublisher;

    private PhysicalTrackingService service;

    @BeforeEach
    void setUp() {
        service = new PhysicalTrackingService(licensePlateRepository, locationStateRepository, eventPublisher);

        lenient().when(licensePlateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(locationStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createLicensePlate_shouldPersistAndPublishEvent() {
        LicensePlate result = service.createLicensePlate(
            "LP-100", "WH-1", LicensePlateType.PALLET, "CONT-1", "tester"
        );

        verify(licensePlateRepository).save(any(LicensePlate.class));
        verify(eventPublisher).publishLicensePlateCreated("LP-100", "WH-1", "PALLET", "tester");
        assertEquals("LP-100", result.getLicensePlateId());
        assertEquals(LicensePlateStatus.CREATED, result.getStatus());
    }

    @Test
    void moveLicensePlate_shouldUpdateLocationStatesAndPublish() {
        LicensePlate plate = LicensePlate.create(
            "LP-200", "WH-1", LicensePlateType.TOTE, "CONT-2", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 5, new BigDecimal("10"), new BigDecimal("2"), "EA");
        plate.moveTo("LOC-1", MovementType.PUTAWAY, "worker", "initial");

        LocationState fromState = LocationState.create(
            "LOC-1", "WH-1", null, 100, new BigDecimal("1000"), new BigDecimal("100")
        );
        fromState.addLicensePlate("LP-200", plate.getTotalQuantity(), plate.getTotalWeight(), plate.getTotalVolume());

        when(licensePlateRepository.findById("LP-200")).thenReturn(Optional.of(plate));
        when(locationStateRepository.findById("LOC-1")).thenReturn(Optional.of(fromState));
        when(locationStateRepository.findById("LOC-2")).thenReturn(Optional.empty());

        LicensePlate updated = service.moveLicensePlate(
            "LP-200", "LOC-2", MovementType.RELOCATION, "worker-2", "move"
        );

        assertEquals("LOC-2", updated.getCurrentLocationId());
        assertEquals(LicensePlateStatus.AT_LOCATION, updated.getStatus());

        ArgumentCaptor<LocationState> stateCaptor = ArgumentCaptor.forClass(LocationState.class);
        verify(locationStateRepository, times(2)).save(stateCaptor.capture());
        List<LocationState> savedStates = stateCaptor.getAllValues();

        LocationState savedFrom = savedStates.get(0);
        LocationState savedTo = savedStates.get(1);

        assertTrue(savedFrom.isEmpty());
        assertEquals("LOC-2", savedTo.getLocationId());
        assertEquals(plate.getTotalQuantity(), savedTo.getCurrentQuantity());

        verify(eventPublisher).publishLicensePlateMoved(
            "LP-200", "LOC-1", "LOC-2", MovementType.RELOCATION.name(), "worker-2"
        );
    }

    @Test
    void addItemToLicensePlate_shouldUpdateLocationStateAndPublish() {
        LicensePlate plate = LicensePlate.create(
            "LP-300", "WH-1", LicensePlateType.PALLET, "CONT-3", "tester"
        );
        plate.addItem("SKU-BASE", "LOT-0", 1, BigDecimal.ZERO, BigDecimal.ZERO, "EA");
        plate.moveTo("LOC-3", MovementType.PUTAWAY, "worker", "initial");

        LocationState locationState = LocationState.create(
            "LOC-3", "WH-1", null, 100, new BigDecimal("1000"), new BigDecimal("100")
        );
        locationState.addLicensePlate("LP-300", 0, BigDecimal.ZERO, BigDecimal.ZERO);

        when(licensePlateRepository.findById("LP-300")).thenReturn(Optional.of(plate));
        when(locationStateRepository.findById("LOC-3")).thenReturn(Optional.of(locationState));

        LicensePlate updated = service.addItemToLicensePlate(
            "LP-300", "SKU-1", "LOT-1", 3, new BigDecimal("6.5"), new BigDecimal("1.5"), "EA"
        );

        assertEquals(4, updated.getTotalQuantity());
        assertEquals(new BigDecimal("6.5"), updated.getTotalWeight());
        assertEquals(new BigDecimal("1.5"), updated.getTotalVolume());

        ArgumentCaptor<LocationState> stateCaptor = ArgumentCaptor.forClass(LocationState.class);
        verify(locationStateRepository).save(stateCaptor.capture());
        LocationState savedState = stateCaptor.getValue();
        assertEquals(4, savedState.getCurrentQuantity());
        assertTrue(savedState.getLicensePlateIds().contains("LP-300"));

        verify(eventPublisher).publishItemAdded("LP-300", "SKU-1", 3, "LOC-3");
        verify(licensePlateRepository).save(updated);
    }

    @Test
    void removeItemFromLicensePlate_shouldHandleEmptyResultAndPublish() {
        LicensePlate plate = LicensePlate.create(
            "LP-400", "WH-1", LicensePlateType.TOTE, "CONT-4", "tester"
        );
        plate.addItem("SKU-9", "LOT-9", 2, new BigDecimal("4"), new BigDecimal("1"), "EA");
        plate.moveTo("LOC-4", MovementType.PUTAWAY, "worker", "initial");

        LocationState locationState = LocationState.create(
            "LOC-4", "WH-1", null, 100, new BigDecimal("1000"), new BigDecimal("100")
        );
        locationState.addLicensePlate("LP-400", 0, BigDecimal.ZERO, BigDecimal.ZERO);

        when(licensePlateRepository.findById("LP-400")).thenReturn(Optional.of(plate));
        when(locationStateRepository.findById("LOC-4")).thenReturn(Optional.of(locationState));

        LicensePlate updated = service.removeItemFromLicensePlate(
            "LP-400", "SKU-9", "LOT-9", 2
        );

        assertEquals(0, updated.getTotalQuantity());
        assertEquals(LicensePlateStatus.CONSUMED, updated.getStatus());

        ArgumentCaptor<LocationState> stateCaptor = ArgumentCaptor.forClass(LocationState.class);
        verify(locationStateRepository).save(stateCaptor.capture());
        LocationState savedState = stateCaptor.getValue();
        assertFalse(savedState.getLicensePlateIds().contains("LP-400"));
        assertEquals(0, savedState.getCurrentQuantity());

        verify(eventPublisher).publishItemRemoved("LP-400", "SKU-9", 2, "LOC-4");
        verify(licensePlateRepository).save(updated);
    }

    @Test
    void blockLocation_shouldPersistAndPublish() {
        LocationState state = LocationState.create(
            "LOC-5", "WH-2", "ZONE-X", 10, new BigDecimal("100"), new BigDecimal("50")
        );

        when(locationStateRepository.findById("LOC-5")).thenReturn(Optional.of(state));

        LocationState blocked = service.blockLocation("LOC-5", "maintenance");

        assertTrue(blocked.getIsBlocked());
        assertEquals("maintenance", blocked.getBlockReason());
        verify(eventPublisher).publishLocationBlocked("LOC-5", "WH-2", "maintenance");
        verify(locationStateRepository).save(blocked);
    }

    @Test
    void unblockLocation_shouldPersistAndPublish() {
        LocationState state = LocationState.create(
            "LOC-6", "WH-3", "ZONE-Y", 10, new BigDecimal("100"), new BigDecimal("50")
        );
        state.block("incident");

        when(locationStateRepository.findById("LOC-6")).thenReturn(Optional.of(state));

        LocationState unblocked = service.unblockLocation("LOC-6");

        assertFalse(unblocked.getIsBlocked());
        assertNull(unblocked.getBlockReason());
        verify(eventPublisher).publishLocationUnblocked("LOC-6", "WH-3");
        verify(locationStateRepository).save(unblocked);
    }

    @Test
    void getLicensePlate_shouldReturnFromRepository() {
        LicensePlate plate = LicensePlate.create(
            "LP-500", "WH-4", LicensePlateType.PALLET, "CONT-5", "tester"
        );
        when(licensePlateRepository.findById("LP-500")).thenReturn(Optional.of(plate));

        Optional<LicensePlate> result = service.getLicensePlate("LP-500");

        assertTrue(result.isPresent());
        assertEquals("LP-500", result.get().getLicensePlateId());
        verify(licensePlateRepository).findById("LP-500");
    }

    @Test
    void getLocationStateAndStates_shouldDelegateToRepositories() {
        LocationState state = LocationState.create(
            "LOC-7", "WH-5", "ZONE-Z", 10, new BigDecimal("100"), new BigDecimal("50")
        );
        when(locationStateRepository.findById("LOC-7")).thenReturn(Optional.of(state));
        when(locationStateRepository.findByWarehouseId("WH-5")).thenReturn(List.of(state));

        LocationState single = service.getLocationState("LOC-7");
        List<LocationState> states = service.getLocationStates("WH-5");

        assertEquals("LOC-7", single.getLocationId());
        assertEquals(1, states.size());
        verify(locationStateRepository).findById("LOC-7");
        verify(locationStateRepository).findByWarehouseId("WH-5");
    }
}
