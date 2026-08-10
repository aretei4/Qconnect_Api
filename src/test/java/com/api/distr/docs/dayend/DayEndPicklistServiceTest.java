package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PicklistUpdateRequest;
import com.api.distr.docs.sales.dto.SalesEntryDto;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DayEndPicklistService")
class DayEndPicklistServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    com.api.distr.docs.sales.repo.DeliveryRepository deliveryRepository;

    @InjectMocks
    DayEndPicklistService service;

    // ── Helpers ────────────────────────────────────────────────────────────────

    private PicklistUpdateRequest buildRequest(boolean delivered, double amount,
                                               String mode, String reason) {
        PicklistUpdateRequest req = new PicklistUpdateRequest();
        req.setDelivered(delivered);
        req.setPaymentAmount(amount);
        req.setPaymentMode(mode);
        req.setReason(reason);
        return req;
    }

    // ── getPicklistsByDayendId ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getPicklistsByDayendId")
    class GetPicklistsTests {

        @Test
        @DisplayName("returns mapped list for a valid dayendId")
        void returnsListForDayendId() {
            SalesEntryDto dto = new SalesEntryDto();
            dto.setDireId(101L);
            dto.setCustomerNo("C001");
            dto.setDelivered(true);
            dto.setPaymentAmount(500.0);
            dto.setPaymentMode("CASH:500.0");
            dto.setAssignStatus(2);

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of(dto));

            List<SalesEntryDto> result = service.getPicklistsByDayendId(48L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDireId()).isEqualTo(101L);
            assertThat(result.get(0).getPaymentAmount()).isEqualTo(500.0);
            assertThat(result.get(0).getAssignStatus()).isEqualTo(2);

            verify(jdbcTemplate).query(anyString(), eq(new Object[]{48L}), any(RowMapper.class));
        }

        @Test
        @DisplayName("returns empty list when no delivery_status rows exist")
        void returnsEmptyListWhenNoRows() {
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(List.of());

            List<SalesEntryDto> result = service.getPicklistsByDayendId(99L);

            assertThat(result).isEmpty();
        }
    }

    // ── updatePicklistPayment — validation ─────────────────────────────────────

    @Nested
    @DisplayName("updatePicklistPayment — validation")
    class UpdateValidationTests {

        @Test
        @DisplayName("throws IllegalArgumentException when direId is null")
        void throwsWhenDireIdNull() {
            PicklistUpdateRequest req = buildRequest(true, 100.0, "CASH:100.0", null);
            assertThatThrownBy(() -> service.updatePicklistPayment(null, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when direId is zero or negative")
        void throwsWhenDireIdZero() {
            PicklistUpdateRequest req = buildRequest(true, 100.0, "CASH:100.0", null);
            assertThatThrownBy(() -> service.updatePicklistPayment(0L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when paymentAmount is negative")
        void throwsWhenNegativeAmount() {
            PicklistUpdateRequest req = buildRequest(true, -1.0, "CASH:-1.0", null);
            assertThatThrownBy(() -> service.updatePicklistPayment(101L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("zero payment amount is valid (non-delivery case)")
        void zeroAmountIsValid() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                    .thenReturn(1);

            assertThatNoException().isThrownBy(() ->
                    service.updatePicklistPayment(101L,
                            buildRequest(false, 0.0, null, "Customer not available")));
        }
    }

    // ── updatePicklistPayment — UPDATE branch ──────────────────────────────────

    @Nested
    @DisplayName("updatePicklistPayment — UPDATE existing row")
    class UpdateExistingRowTests {

        @BeforeEach
        void rowExists() {
            when(jdbcTemplate.queryForObject(
                    contains("SELECT COUNT(*)"), eq(Integer.class), any()))
                    .thenReturn(1);
        }

        @Test
        @DisplayName("calls UPDATE when delivery_status row exists")
        void callsUpdate() {
            PicklistUpdateRequest req = buildRequest(true, 600.0, "CASH:600.0", null);

            service.updatePicklistPayment(101L, req);

            // one UPDATE for delivery_status, one for delivery_assignments
            verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
            verify(jdbcTemplate, never()).update(contains("INSERT"), any(Object[].class));
        }

        @Test
        @DisplayName("UPDATE uses correct parameters in correct order")
        void updateParameterOrder() {
            PicklistUpdateRequest req = buildRequest(true, 750.0, "UPI:750.0", null);
            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);

            service.updatePicklistPayment(109L, req);

            verify(jdbcTemplate, atLeastOnce()).update(contains("UPDATE delivery_status"), captor.capture());
            Object[] args = captor.getValue();
            assertThat(args[0]).isEqualTo(true);          // delivered
            assertThat(args[1]).isNull();                 // reason
            assertThat(args[2]).isEqualTo(109L);          // dire_id (WHERE)

            // Payment now goes to payment_details, not delivery_status
            verify(deliveryRepository).upsertPaymentDetails(eq(109L), anyList());
        }

        @Test
        @DisplayName("syncs delivery_assignments status=2 when delivered=true")
        void syncsStatusDelivered() {
            service.updatePicklistPayment(101L, buildRequest(true, 500.0, "CASH:500.0", null));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate, atLeastOnce())
                    .update(contains("UPDATE delivery_assignments"), captor.capture());
            assertThat(captor.getValue()[0]).isEqualTo(2);   // DELIVERED
        }

        @Test
        @DisplayName("syncs delivery_assignments status=1 when delivered=false")
        void syncsStatusFailed() {
            service.updatePicklistPayment(101L,
                    buildRequest(false, 0.0, null, "No one home"));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate, atLeastOnce())
                    .update(contains("UPDATE delivery_assignments"), captor.capture());
            assertThat(captor.getValue()[0]).isEqualTo(1);   // FAILED
        }
    }

    // ── updatePicklistPayment — INSERT branch ──────────────────────────────────

    @Nested
    @DisplayName("updatePicklistPayment — INSERT new row")
    class InsertNewRowTests {

        @BeforeEach
        void rowMissing() {
            when(jdbcTemplate.queryForObject(
                    contains("SELECT COUNT(*)"), eq(Integer.class), any()))
                    .thenReturn(0);
        }

        @Test
        @DisplayName("calls INSERT when delivery_status row does not exist")
        void callsInsert() {
            when(jdbcTemplate.queryForObject(
                    contains("delivery_boy_id"), eq(Long.class), any()))
                    .thenReturn(5L);

            service.updatePicklistPayment(102L,
                    buildRequest(true, 400.0, "CASH:400.0", null));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO delivery_status"), captor.capture());
            Object[] args = captor.getValue();
            assertThat(args[0]).isEqualTo(102L);          // dire_id
            assertThat(args[1]).isEqualTo(5L);            // delivery_id
            assertThat(args[2]).isEqualTo(true);          // delivered
            assertThat(args[3]).isNull();                 // reason

            verify(deliveryRepository).upsertPaymentDetails(eq(102L), anyList());
        }

        @Test
        @DisplayName("inserts with null delivery_id when lookup fails")
        void insertsNullDeliveryIdOnCastFailure() {
            when(jdbcTemplate.queryForObject(
                    contains("delivery_boy_id"), eq(Long.class), any()))
                    .thenThrow(new RuntimeException("invalid cast"));

            assertThatNoException().isThrownBy(() ->
                    service.updatePicklistPayment(103L,
                            buildRequest(true, 200.0, "UPI:200.0", null)));

            ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbcTemplate).update(contains("INSERT INTO delivery_status"), captor.capture());
            assertThat(captor.getValue()[1]).isNull();    // delivery_id = null
        }

        @Test
        @DisplayName("still syncs delivery_assignments after INSERT")
        void syncStatusAfterInsert() {
            when(jdbcTemplate.queryForObject(
                    contains("delivery_boy_id"), eq(Long.class), any()))
                    .thenReturn(7L);

            service.updatePicklistPayment(104L,
                    buildRequest(false, 0.0, null, "Refused delivery"));

            verify(jdbcTemplate).update(contains("UPDATE delivery_assignments"), any(Object[].class));
        }
    }

    // ── deletePicklist ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deletePicklist")
    class DeletePicklistTests {

        @Test
        @DisplayName("deletes delivery_status first, then delivery_assignments")
        void deleteOrderAndReturnValue() {
            when(jdbcTemplate.update(contains("DELETE FROM delivery_status"), (Object) any()))
                    .thenReturn(0);
            when(jdbcTemplate.update(contains("DELETE FROM delivery_assignments"), (Object) any()))
                    .thenReturn(1);

            int rows = service.deletePicklist(101L);

            assertThat(rows).isEqualTo(1);
            var inOrder = inOrder(jdbcTemplate);
            inOrder.verify(jdbcTemplate).update(contains("DELETE FROM delivery_status"), eq(101L));
            inOrder.verify(jdbcTemplate).update(contains("DELETE FROM delivery_assignments"), eq(101L));
        }

        @Test
        @DisplayName("returns 0 when not found in delivery_assignments")
        void returnsZeroWhenNotFound() {
            when(jdbcTemplate.update(anyString(), (Object) any()))
                    .thenReturn(0);

            assertThat(service.deletePicklist(999L)).isEqualTo(0);
        }
    }
}
