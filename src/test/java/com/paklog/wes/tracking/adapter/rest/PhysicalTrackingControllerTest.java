package com.paklog.wes.tracking.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.tracking.adapter.rest.dto.AddItemRequest;
import com.paklog.wes.tracking.adapter.rest.dto.CreateLicensePlateRequest;
import com.paklog.wes.tracking.adapter.rest.dto.MoveLicensePlateRequest;
import com.paklog.wes.tracking.application.service.PhysicalTrackingService;
import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PhysicalTrackingController.class)
class PhysicalTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhysicalTrackingService trackingService;

    private LicensePlate sampleLicensePlate;
    private LocationState sampleLocationState;

    @BeforeEach
    void setUp() {
        sampleLicensePlate = LicensePlate.create(
            "LP-123", "WH-1", LicensePlateType.PALLET, "CONT-1", "creator"
        );
        sampleLicensePlate.addItem(
            "SKU-1", "LOT-1", 5, new BigDecimal("10.5"), new BigDecimal("3.2"), "EA"
        );
        sampleLicensePlate.moveTo("LOC-1", MovementType.PUTAWAY, "worker-1", "initial putaway");

        sampleLocationState = LocationState.create(
            "LOC-1", "WH-1", "ZONE-A", 20, new BigDecimal("200"), new BigDecimal("100")
        );
        sampleLocationState.addLicensePlate("LP-123", 5, new BigDecimal("10.5"), new BigDecimal("3.2"));
    }

    @Test
    void createLicensePlate_shouldReturnCreatedResponse() throws Exception {
        when(trackingService.createLicensePlate(anyString(), anyString(), any(), anyString(), anyString()))
            .thenReturn(sampleLicensePlate);

        CreateLicensePlateRequest request = new CreateLicensePlateRequest(
            "LP-123", "WH-1", LicensePlateType.PALLET, "CONT-1"
        );

        mockMvc.perform(post("/api/v1/tracking/license-plates")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.licensePlateId", is("LP-123")))
            .andExpect(jsonPath("$.items", hasSize(1)));

        verify(trackingService).createLicensePlate("LP-123", "WH-1", LicensePlateType.PALLET, "CONT-1", "tester");
    }

    @Test
    void getLicensePlate_shouldReturnResultWhenPresent() throws Exception {
        when(trackingService.getLicensePlate("LP-123")).thenReturn(Optional.of(sampleLicensePlate));

        mockMvc.perform(get("/api/v1/tracking/license-plates/LP-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licensePlateId", is("LP-123")))
            .andExpect(jsonPath("$.totalQuantity", is(5)));
    }

    @Test
    void getLicensePlate_shouldReturnNotFoundWhenMissing() throws Exception {
        when(trackingService.getLicensePlate("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tracking/license-plates/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listLicensePlates_shouldReturnResultsForLocation() throws Exception {
        when(trackingService.getLicensePlatesAtLocation("LOC-1")).thenReturn(List.of(sampleLicensePlate));

        mockMvc.perform(get("/api/v1/tracking/license-plates")
                .param("locationId", "LOC-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].licensePlateId", is("LP-123")));
    }

    @Test
    void listLicensePlates_shouldReturnEmptyWhenNoLocationProvided() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/license-plates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void moveLicensePlate_shouldReturnUpdatedLicensePlate() throws Exception {
        when(trackingService.moveLicensePlate(eq("LP-123"), eq("LOC-2"), eq(MovementType.RELOCATION), eq("tester"), eq("reason")))
            .thenReturn(sampleLicensePlate);

        MoveLicensePlateRequest request = new MoveLicensePlateRequest("LOC-2", MovementType.RELOCATION, "reason");

        mockMvc.perform(post("/api/v1/tracking/license-plates/LP-123/move")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licensePlateId", is("LP-123")));
    }

    @Test
    void addItem_shouldReturnUpdatedLicensePlate() throws Exception {
        when(trackingService.addItemToLicensePlate(eq("LP-123"), anyString(), any(), anyInt(), any(), any(), anyString()))
            .thenReturn(sampleLicensePlate);

        AddItemRequest request = new AddItemRequest(
            "SKU-NEW", "LOT-NEW", 3, new BigDecimal("5.0"), new BigDecimal("1.0"), "EA"
        );

        mockMvc.perform(post("/api/v1/tracking/license-plates/LP-123/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licensePlateId", is("LP-123")));
    }

    @Test
    void removeItem_shouldReturnUpdatedLicensePlate() throws Exception {
        when(trackingService.removeItemFromLicensePlate("LP-123", "SKU-1", "LOT-1", 2))
            .thenReturn(sampleLicensePlate);

        mockMvc.perform(delete("/api/v1/tracking/license-plates/LP-123/items")
                .param("sku", "SKU-1")
                .param("lotNumber", "LOT-1")
                .param("quantity", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licensePlateId", is("LP-123")));
    }

    @Test
    void getLocationState_shouldReturnResponse() throws Exception {
        when(trackingService.getLocationState("LOC-1")).thenReturn(sampleLocationState);

        mockMvc.perform(get("/api/v1/tracking/locations/LOC-1/state"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId", is("LOC-1")))
            .andExpect(jsonPath("$.licensePlateIds", hasSize(1)));
    }

    @Test
    void getLocationState_shouldReturnNotFoundForMissingState() throws Exception {
        when(trackingService.getLocationState("LOC-2")).thenReturn(null);

        mockMvc.perform(get("/api/v1/tracking/locations/LOC-2/state"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listLocationStates_shouldReturnWarehouseStates() throws Exception {
        when(trackingService.getLocationStates("WH-1")).thenReturn(List.of(sampleLocationState));

        mockMvc.perform(get("/api/v1/tracking/locations")
                .param("warehouseId", "WH-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].locationId", is("LOC-1")));
    }

    @Test
    void blockAndUnblockLocation_shouldReturnUpdatedState() throws Exception {
        when(trackingService.blockLocation("LOC-1", "maintenance")).thenReturn(sampleLocationState);
        when(trackingService.unblockLocation("LOC-1")).thenReturn(sampleLocationState);

        mockMvc.perform(post("/api/v1/tracking/locations/LOC-1/block")
                .param("reason", "maintenance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId", is("LOC-1")));

        mockMvc.perform(post("/api/v1/tracking/locations/LOC-1/unblock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId", is("LOC-1")));
    }
}
