package com.api.distr.docs.dayend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DayEndApprovalService")
class DayEndApprovalServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    DayEndApprovalService service;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private DayEndDto dto(Long deliveryId, String date) {
        DayEndDto d = new DayEndDto();
        d.setDeliveryId(deliveryId);
        d.setDate(date);
        return d;
    }

    private DayEndDto dtoWithPicklists(Long deliveryId, String date, double amount, List<String> picklists) {
        DayEndDto d = dto(deliveryId, date);
        d.setTotalAmount(amount);
        d.setPicklistNos(picklists);
        return d;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // startDayEnd
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startDayEnd")
    class StartDayEndTests {

        @Test
        @DisplayName("resets existing record back to STARTED when row already exists")
        void resetsExistingRecord() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(1);

            service.startDayEnd(dto(5L, "23-05-2026"));

            verify(jdbcTemplate).update(contains("SET status     = 'STARTED'"),
                    eq(5L), eq(LocalDate.of(2026, 5, 23)));
            verify(jdbcTemplate, never()).update(contains("INSERT"), any(), any(), any());
        }

        @Test
        @DisplayName("inserts new STARTED record when no row exists")
        void insertsNewRecord() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(0);

            service.startDayEnd(dto(7L, "01-01-2026"));

            verify(jdbcTemplate).update(contains("INSERT INTO dayend_approval"),
                    eq(7L), eq(LocalDate.of(2026, 1, 1)));
        }

        @Test
        @DisplayName("parses date in dd-MM-yyyy format correctly")
        void parsesDateFormat() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(0);

            // Day 15, Month 03, Year 2026 — must not be swapped
            service.startDayEnd(dto(1L, "15-03-2026"));

            ArgumentCaptor<Object> dateCaptor = ArgumentCaptor.forClass(Object.class);
            verify(jdbcTemplate).update(anyString(), any(), dateCaptor.capture());
            assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.of(2026, 3, 15));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createDayEnd
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createDayEnd")
    class CreateDayEndTests {

        @Test
        @DisplayName("updates status to PENDING and saves picklists when row exists")
        void updatesExistingRowToPending() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(1);

            DayEndDto d = dtoWithPicklists(5L, "23-05-2026", 3500.0,
                    List.of("E587P001", "E587P002", "E587P003"));

            service.createDayEnd(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("SET status = 'PENDING'"), captor.capture());

            Object[] args = captor.getValue();
            assertThat(args[0]).isEqualTo(3500.0);
            assertThat(args[1]).isEqualTo("E587P001,E587P002,E587P003");   // comma-joined
        }

        @Test
        @DisplayName("inserts new PENDING row when no existing record")
        void insertsNewRow() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(0);

            DayEndDto d = dtoWithPicklists(3L, "10-06-2026", 1200.0,
                    List.of("P001", "P002"));

            service.createDayEnd(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO dayend_approval"), captor.capture());

            Object[] args = captor.getValue();
            assertThat(args[0]).isEqualTo(3L);             // delivery_id
            assertThat(args[2]).isEqualTo(1200.0);          // total_amount
            assertThat(args[3]).isEqualTo("P001,P002");     // picklist_nos
        }

        @Test
        @DisplayName("stores null picklist_nos when picklist list is empty")
        void storesNullWhenNoPicklists() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(0);

            DayEndDto d = dtoWithPicklists(2L, "01-05-2026", 0.0, List.of());
            service.createDayEnd(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT"), captor.capture());
            assertThat(captor.getValue()[3]).isNull();      // picklist_nos = NULL
        }

        @Test
        @DisplayName("defaults totalAmount to 0.0 when null")
        void defaultsTotalAmountToZero() {
            when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*)"),
                    eq(Integer.class), any(), any()))
                    .thenReturn(0);

            DayEndDto d = dto(1L, "01-05-2026");
            d.setTotalAmount(null);
            service.createDayEnd(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT"), captor.capture());
            assertThat(captor.getValue()[2]).isEqualTo(0.0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // approveDayEndById
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("approveDayEndById")
    class ApproveTests {

        @Test
        @DisplayName("throws RuntimeException when dayendId is null")
        void throwsWhenDayendIdNull() {
            DayEndDto d = new DayEndDto();   // dayendId = null
            assertThatThrownBy(() -> service.approveDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("dayendId is required");
        }

        @Test
        @DisplayName("updates status to APPROVED for a valid PENDING record")
        void approvesSuccessfully() {
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            DayEndDto d = new DayEndDto();
            d.setDayendId(48L);
            service.approveDayEndById(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("SET status = 'APPROVED'"), captor.capture());
            assertThat(captor.getValue()[0]).isEqualTo(48L);
        }

        @Test
        @DisplayName("throws RuntimeException when no PENDING record found (already approved / wrong id)")
        void throwsWhenNoPendingFound() {
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

            DayEndDto d = new DayEndDto();
            d.setDayendId(999L);

            assertThatThrownBy(() -> service.approveDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No pending DayEnd found to approve");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // rejectDayEndById
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("rejectDayEndById")
    class RejectTests {

        @Test
        @DisplayName("throws RuntimeException when dayendId is null")
        void throwsWhenDayendIdNull() {
            DayEndDto d = new DayEndDto();
            d.setRejectReason("Wrong amount");
            assertThatThrownBy(() -> service.rejectDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("dayendId is required");
        }

        @Test
        @DisplayName("throws RuntimeException when rejectReason is null")
        void throwsWhenReasonNull() {
            DayEndDto d = new DayEndDto();
            d.setDayendId(48L);
            d.setRejectReason(null);
            assertThatThrownBy(() -> service.rejectDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reject reason is required");
        }

        @Test
        @DisplayName("throws RuntimeException when rejectReason is blank")
        void throwsWhenReasonBlank() {
            DayEndDto d = new DayEndDto();
            d.setDayendId(48L);
            d.setRejectReason("   ");
            assertThatThrownBy(() -> service.rejectDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reject reason is required");
        }

        @Test
        @DisplayName("updates status to REJECTED with reason for a valid PENDING record")
        void rejectsSuccessfully() {
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            DayEndDto d = new DayEndDto();
            d.setDayendId(48L);
            d.setRejectReason("Amount mismatch");

            service.rejectDayEndById(d);

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("SET status = 'REJECTED'"), captor.capture());
            assertThat(captor.getValue()[0]).isEqualTo("Amount mismatch");
            assertThat(captor.getValue()[1]).isEqualTo(48L);
        }

        @Test
        @DisplayName("throws RuntimeException when no PENDING record found")
        void throwsWhenNoPendingFound() {
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

            DayEndDto d = new DayEndDto();
            d.setDayendId(99L);
            d.setRejectReason("Some reason");

            assertThatThrownBy(() -> service.rejectDayEndById(d))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No pending DayEnd found to reject");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getDayEndList
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDayEndList")
    class GetListTests {

        @Test
        @DisplayName("queries without deliveryId filter when deliveryId is null")
        void queriesWithoutDeliveryIdFilter() {
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            service.getDayEndList(null,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 23));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(
                    argThat(sql -> !sql.contains("AND d.delivery_id")),
                    captor.capture(),
                    any(RowMapper.class));
            assertThat(captor.getValue()).hasSize(2);   // only fromDate + toDate
        }

        @Test
        @DisplayName("adds delivery_id filter and param when deliveryId is provided")
        void queriesWithDeliveryIdFilter() {
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            service.getDayEndList(5L,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 23));

            ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).query(
                    argThat(sql -> sql.contains("AND d.delivery_id = ?")),
                    paramsCaptor.capture(),
                    any(RowMapper.class));

            Object[] params = paramsCaptor.getValue();
            assertThat(params).hasSize(3);              // fromDate + toDate + deliveryId
            assertThat(params[2]).isEqualTo(5L);
        }

        @Test
        @DisplayName("returns empty list when no records in date range")
        void returnsEmptyList() {
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            List<DayEndResponseDto> result = service.getDayEndList(null,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2));

            assertThat(result).isEmpty();
        }
    }
}
