package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanApprovalRequest;
import com.api.distr.docs.dayend.DayEndStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DanApprovalService")
class DanApprovalServiceTest {

    @Mock
    DanApprovalRepository repo;

    @InjectMocks
    DanApprovalService service;

    private DanApprovalRequest req(String stage, String action, String by, String remarks) {
        DanApprovalRequest r = new DanApprovalRequest();
        r.setStage(stage);
        r.setAction(action);
        r.setApprovedBy(by);
        r.setRemarks(remarks);
        return r;
    }

    @BeforeEach
    void danExists() {
        when(repo.danExists(anyLong())).thenReturn(true);
        when(repo.getTrail(anyLong())).thenReturn(List.of());
        when(repo.getDanStatus(anyLong())).thenReturn("PENDING");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects an unknown stage")
        void unknownStage() {
            assertThatThrownBy(() -> service.record(45L, req("WAREHOUSE", "APPROVED", "ravi", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stage must be one of");
        }

        @Test
        @DisplayName("rejects an unknown action")
        void unknownAction() {
            assertThatThrownBy(() -> service.record(45L, req("STOREKEEPER", "MAYBE", "ravi", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("action must be");
        }

        @Test
        @DisplayName("requires remarks when rejecting")
        void rejectNeedsRemarks() {
            assertThatThrownBy(() -> service.record(45L, req("STOREKEEPER", "REJECTED", "ravi", "  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("remarks are required");
        }

        @Test
        @DisplayName("throws when the DAN does not exist")
        void missingDan() {
            when(repo.danExists(99L)).thenReturn(false);
            assertThatThrownBy(() -> service.record(99L, req("STOREKEEPER", "APPROVED", "ravi", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DAN not found");
        }
    }

    // ── Stage order ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stage order")
    class StageOrder {

        @Test
        @DisplayName("accounts cannot approve before the storekeeper")
        void accountsBlockedFirst() {
            when(repo.getLatestApprovals(45L)).thenReturn(Map.of());

            assertThatThrownBy(() -> service.record(45L, req("ACCOUNTS", "APPROVED", "meera", null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Storekeeper approval is required");

            verify(repo, never()).insert(anyLong(), anyString(), anyString(), anyString(), any(), any());
        }

        @Test
        @DisplayName("storekeeper approval alone does not close the DAN")
        void storekeeperOnly() {
            when(repo.getLatestApprovals(45L))
                    .thenReturn(Map.of("STOREKEEPER", Map.of("by", "ravi", "at", "")));

            var status = service.record(45L, req("STOREKEEPER", "APPROVED", "ravi", "stock ok"));

            verify(repo).insert(45L, "STOREKEEPER", "APPROVED", "ravi", null, "stock ok");
            verify(repo, never()).markApproved(anyLong());
            assertThat(status.getPendingStage()).isEqualTo("ACCOUNTS");
            assertThat(status.isFullyApproved()).isFalse();
        }

        @Test
        @DisplayName("both desks approving marks the DAN APPROVED")
        void bothApproved() {
            when(repo.getLatestApprovals(45L)).thenReturn(Map.of(
                    "STOREKEEPER", Map.of("by", "ravi",  "at", ""),
                    "ACCOUNTS",    Map.of("by", "meera", "at", "")));

            var status = service.record(45L, req("ACCOUNTS", "APPROVED", "meera", "tallied"));

            verify(repo).markApproved(45L);
            assertThat(status.isFullyApproved()).isTrue();
            assertThat(status.getPendingStage()).isNull();
            assertThat(status.getStorekeeperBy()).isEqualTo("ravi");
            assertThat(status.getAccountsBy()).isEqualTo("meera");
        }
    }

    // ── Rejection ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rejection records the event and marks the DAN REJECTED")
    void rejection() {
        when(repo.getLatestApprovals(45L)).thenReturn(Map.of());

        service.record(45L, req("STOREKEEPER", "REJECTED", "ravi", "short by 2 cases"));

        verify(repo).insert(45L, "STOREKEEPER", "REJECTED", "ravi", null, "short by 2 cases");
        verify(repo).markRejected(45L, "short by 2 cases");
        verify(repo, never()).markApproved(anyLong());
    }

    @Test
    @DisplayName("action defaults to APPROVED when omitted")
    void defaultsToApproved() {
        when(repo.getLatestApprovals(45L)).thenReturn(Map.of());
        service.record(45L, req("STOREKEEPER", null, "ravi", null));
        verify(repo).insert(45L, "STOREKEEPER", "APPROVED", "ravi", null, null);
    }

    @Test
    @DisplayName("falls back to 'system' when no user is supplied or authenticated")
    void systemFallback() {
        when(repo.getLatestApprovals(45L)).thenReturn(Map.of());
        service.record(45L, req("STOREKEEPER", "APPROVED", null, null));
        verify(repo).insert(eq(45L), eq("STOREKEEPER"), eq("APPROVED"), eq("system"), any(), any());
    }

    // ── Stage / action code mapping ───────────────────────────────────────────

    @Nested
    @DisplayName("stage & action codes")
    class CodeMapping {

        @Test
        @DisplayName("every lifecycle stage maps name ↔ code")
        void stageRoundTrip() {
            assertThat(DayEndStatus.stageCode("STARTED")).isEqualTo(0);
            assertThat(DayEndStatus.stageCode("SUBMITTED")).isEqualTo(1);
            assertThat(DayEndStatus.stageCode("STOREKEEPER")).isEqualTo(2);
            assertThat(DayEndStatus.stageCode("ACCOUNTS")).isEqualTo(3);
            assertThat(DayEndStatus.stageCode("CLOSED")).isEqualTo(4);

            for (String s : java.util.List.of("STARTED","SUBMITTED","STOREKEEPER","ACCOUNTS","CLOSED"))
                assertThat(DayEndStatus.stageName(DayEndStatus.stageCode(s))).isEqualTo(s);
        }

        @Test
        @DisplayName("every action maps name ↔ code")
        void actionRoundTrip() {
            assertThat(DayEndStatus.actionCode("APPROVED")).isEqualTo(1);
            assertThat(DayEndStatus.actionCode("REJECTED")).isEqualTo(2);
            assertThat(DayEndStatus.actionCode("CREATED")).isEqualTo(3);
            assertThat(DayEndStatus.actionCode("CLOSED")).isEqualTo(4);

            for (String a : java.util.List.of("APPROVED","REJECTED","CREATED","CLOSED"))
                assertThat(DayEndStatus.actionName(DayEndStatus.actionCode(a))).isEqualTo(a);
        }

        @Test
        @DisplayName("unknown stage / action is rejected")
        void unknownRejected() {
            assertThatThrownBy(() -> DayEndStatus.stageCode("WAREHOUSE"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DayEndStatus.actionCode("MAYBE"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
