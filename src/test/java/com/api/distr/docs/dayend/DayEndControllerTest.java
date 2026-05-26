package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PicklistUpdateRequest;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.api.distr.docs.security.UserJwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DayEndController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@DisplayName("DayEndController")
class DayEndControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // ── Security beans — must be mocked so JwtAuthFilter can be constructed ──
    // UserJwtService uses @Value("${onlyoffice.jwt.secret}") which is not in
    // the test context. @MockBean replaces it with a Mockito mock (no @Value
    // injection needed). JwtAuthFilter then calls chain.doFilter normally since
    // isTokenValid() returns false by default, and anyRequest().permitAll() lets
    // all test requests through.
    @MockBean
    UserJwtService userJwtService;

    @MockBean
    DayEndService service;

    @MockBean
    DayEndApprovalService approvalService;

    @MockBean
    DayEndPicklistService picklistService;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private SalesEntryDto samplePicklist(String picklistNo) {
        SalesEntryDto dto = new SalesEntryDto();
        dto.setPicklistNo(picklistNo);
        dto.setCustomerNo("C001");
        dto.setCustDesc("Test Customer");
        dto.setNetValue("1000.0");
        dto.setDelivered(true);
        dto.setPaymentAmount(1000.0);
        dto.setPaymentMode("CASH:1000.0");
        dto.setAssignStatus(2);
        return dto;
    }

    private DayEndResponseDto sampleResponse(long dayendId, String status) {
        DayEndResponseDto r = new DayEndResponseDto();
        r.setDayendId(dayendId);
        r.setDeliveryId(5L);
        r.setDeliveryBoyName("Ravi Kumar");
        r.setDeliveryDate(LocalDate.of(2026, 5, 23));
        r.setStatus(status);
        r.setTotalAmount(3500.0);
        r.setPicklistNos(List.of("E587P001", "E587P002"));
        r.setRequestDate(LocalDateTime.of(2026, 5, 23, 10, 0));
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/dayend/start
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /start")
    class StartEndpointTests {

        @Test
        @DisplayName("200 OK and delegates to approvalService.startDayEnd")
        void startReturns200() throws Exception {
            DayEndDto dto = new DayEndDto();
            dto.setDeliveryId(5L);
            dto.setDate("23-05-2026");

            mvc.perform(post("/api/dayend/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("DayEnd Started"));

            verify(approvalService).startDayEnd(any(DayEndDto.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/dayend/create
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /create")
    class CreateEndpointTests {

        @Test
        @DisplayName("200 OK with success=true on valid request")
        void createReturns200() throws Exception {
            DayEndDto dto = new DayEndDto();
            dto.setDeliveryId(5L);
            dto.setDate("23-05-2026");
            dto.setTotalAmount(3500.0);
            dto.setPicklistNos(List.of("E587P001", "E587P002"));

            mvc.perform(post("/api/dayend/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Day End submitted successfully"));

            verify(approvalService).createDayEnd(any(DayEndDto.class));
        }

        @Test
        @DisplayName("500 with success=false when service throws exception")
        void createReturns500OnError() throws Exception {
            doThrow(new RuntimeException("DB error")).when(approvalService).createDayEnd(any());

            DayEndDto dto = new DayEndDto();
            dto.setDeliveryId(5L);
            dto.setDate("23-05-2026");

            mvc.perform(post("/api/dayend/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("DB error"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/dayend/approve
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /approve")
    class ApproveEndpointTests {

        @Test
        @DisplayName("200 OK when approval succeeds")
        void approveReturns200() throws Exception {
            DayEndDto dto = new DayEndDto();
            dto.setDayendId(48L);

            mvc.perform(post("/api/dayend/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("DayEnd Approved"));

            verify(approvalService).approveDayEndById(any(DayEndDto.class));
        }

        @Test
        @DisplayName("500 when service throws (no PENDING record)")
        void approveReturns500OnError() throws Exception {
            doThrow(new RuntimeException("No pending DayEnd found to approve"))
                    .when(approvalService).approveDayEndById(any());

            DayEndDto dto = new DayEndDto();
            dto.setDayendId(999L);

            mvc.perform(post("/api/dayend/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/dayend/reject
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /reject")
    class RejectEndpointTests {

        @Test
        @DisplayName("200 OK when rejection succeeds")
        void rejectReturns200() throws Exception {
            DayEndDto dto = new DayEndDto();
            dto.setDayendId(48L);
            dto.setRejectReason("Amount mismatch");

            mvc.perform(post("/api/dayend/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("DayEnd Rejected"));

            verify(approvalService).rejectDayEndById(any(DayEndDto.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/dayend/dayEndSummery
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /dayEndSummery")
    class GetListEndpointTests {

        @Test
        @DisplayName("200 OK with list of records (no filters)")
        void listReturnsAllRecords() throws Exception {
            when(approvalService.getDayEndList(isNull(), any(), any()))
                    .thenReturn(List.of(
                            sampleResponse(1L, "PENDING"),
                            sampleResponse(2L, "APPROVED")
                    ));

            mvc.perform(get("/api/dayend/dayEndSummery"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].dayendId").value(1))
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[1].status").value("APPROVED"));
        }

        @Test
        @DisplayName("filters by deliveryId when provided")
        void listFiltersByDeliveryId() throws Exception {
            when(approvalService.getDayEndList(eq(5L), any(), any()))
                    .thenReturn(List.of(sampleResponse(1L, "PENDING")));

            mvc.perform(get("/api/dayend/dayEndSummery").param("deliveryId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(approvalService).getDayEndList(eq(5L), any(), any());
        }

        @Test
        @DisplayName("200 OK with empty list when no records found")
        void listReturnsEmptyList() throws Exception {
            when(approvalService.getDayEndList(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mvc.perform(get("/api/dayend/dayEndSummery"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("applies custom date range when fromDate and toDate provided")
        void listAppliesDateRange() throws Exception {
            when(approvalService.getDayEndList(any(), any(), any()))
                    .thenReturn(List.of());

            mvc.perform(get("/api/dayend/dayEndSummery")
                            .param("fromDate", "01-05-2026")
                            .param("toDate", "23-05-2026"))
                    .andExpect(status().isOk());

            verify(approvalService).getDayEndList(
                    isNull(),
                    eq(LocalDate.of(2026, 5, 1)),
                    eq(LocalDate.of(2026, 5, 23)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/dayend/picklists/{dayendId}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /picklists/{dayendId}")
    class GetPicklistsEndpointTests {

        @Test
        @DisplayName("200 OK with picklist data for valid dayendId")
        void returnsPicklistsForDayendId() throws Exception {
            when(picklistService.getPicklistsByDayendId(48L))
                    .thenReturn(List.of(
                            samplePicklist("E587P001"),
                            samplePicklist("E587P002")
                    ));

            mvc.perform(get("/api/dayend/picklists/48"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].picklistNo").value("E587P001"))
                    .andExpect(jsonPath("$[0].delivered").value(true))
                    .andExpect(jsonPath("$[0].paymentAmount").value(1000.0))
                    .andExpect(jsonPath("$[0].paymentMode").value("CASH:1000.0"))
                    .andExpect(jsonPath("$[0].assignStatus").value(2));
        }

        @Test
        @DisplayName("200 OK with empty list when no picklists")
        void returnsEmptyList() throws Exception {
            when(picklistService.getPicklistsByDayendId(99L))
                    .thenReturn(Collections.emptyList());

            mvc.perform(get("/api/dayend/picklists/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("500 when service throws exception")
        void returns500OnServiceError() throws Exception {
            when(picklistService.getPicklistsByDayendId(anyLong()))
                    .thenThrow(new RuntimeException("DB connection failed"));

            mvc.perform(get("/api/dayend/picklists/48"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Failed to load picklists")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /api/dayend/picklist/{picklistNo}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /picklist/{picklistNo}")
    class UpdatePicklistEndpointTests {

        private PicklistUpdateRequest validRequest() {
            PicklistUpdateRequest req = new PicklistUpdateRequest();
            req.setDelivered(true);
            req.setPaymentAmount(850.0);
            req.setPaymentMode("CASH:850.0");
            req.setReason(null);
            return req;
        }

        @Test
        @DisplayName("200 OK when update succeeds")
        void updateReturns200() throws Exception {
            mvc.perform(put("/api/dayend/picklist/E587P001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Picklist updated successfully"));

            verify(picklistService).updatePicklistPayment(eq("E587P001"), any(PicklistUpdateRequest.class));
        }

        @Test
        @DisplayName("400 when service throws IllegalArgumentException (negative amount)")
        void updateReturns400OnValidationError() throws Exception {
            doThrow(new IllegalArgumentException("Payment amount cannot be negative"))
                    .when(picklistService).updatePicklistPayment(anyString(), any());

            PicklistUpdateRequest req = new PicklistUpdateRequest();
            req.setPaymentAmount(-100.0);

            mvc.perform(put("/api/dayend/picklist/E587P001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Payment amount cannot be negative"));
        }

        @Test
        @DisplayName("500 when service throws generic exception")
        void updateReturns500OnGenericError() throws Exception {
            doThrow(new RuntimeException("Unexpected DB error"))
                    .when(picklistService).updatePicklistPayment(anyString(), any());

            mvc.perform(put("/api/dayend/picklist/E587P001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Update failed")));
        }

        @Test
        @DisplayName("passes picklistNo from URL path to service correctly")
        void passesPicklistNoFromPath() throws Exception {
            mvc.perform(put("/api/dayend/picklist/XYZ9999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk());

            verify(picklistService).updatePicklistPayment(eq("XYZ9999"), any());
        }

        @Test
        @DisplayName("200 when update for not-delivered picklist with reason")
        void updateNotDelivered() throws Exception {
            PicklistUpdateRequest req = new PicklistUpdateRequest();
            req.setDelivered(false);
            req.setPaymentAmount(0.0);
            req.setReason("Customer not available");

            mvc.perform(put("/api/dayend/picklist/E587P002")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(picklistService).updatePicklistPayment(eq("E587P002"), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/dayend/picklist/{picklistNo}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /picklist/{picklistNo}")
    class DeletePicklistEndpointTests {

        @Test
        @DisplayName("200 OK with deleted row count on success")
        void deleteReturns200() throws Exception {
            when(picklistService.deletePicklist("E587P001")).thenReturn(1);

            mvc.perform(delete("/api/dayend/picklist/E587P001"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1 record(s) deleted"));

            verify(picklistService).deletePicklist("E587P001");
        }

        @Test
        @DisplayName("200 with 0 records deleted when picklist not found")
        void deleteReturns0WhenNotFound() throws Exception {
            when(picklistService.deletePicklist("UNKNOWN")).thenReturn(0);

            mvc.perform(delete("/api/dayend/picklist/UNKNOWN"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0 record(s) deleted"));
        }

        @Test
        @DisplayName("500 when service throws exception")
        void deleteReturns500OnError() throws Exception {
            when(picklistService.deletePicklist(anyString()))
                    .thenThrow(new RuntimeException("FK constraint violation"));

            mvc.perform(delete("/api/dayend/picklist/E587P001"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Delete failed")));
        }
    }
}
