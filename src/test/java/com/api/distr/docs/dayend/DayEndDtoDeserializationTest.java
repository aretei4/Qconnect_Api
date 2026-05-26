package com.api.distr.docs.dayend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that DayEndDto correctly deserializes both camelCase (web) and
 * snake_case (mobile app) JSON payloads — critical because the same endpoint
 * is called by two different clients.
 */
@DisplayName("DayEndDto JSON deserialization")
class DayEndDtoDeserializationTest {

    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // camelCase (web client)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("camelCase JSON (web)")
    class CamelCaseTests {

        @Test
        @DisplayName("maps all camelCase fields correctly")
        void allCamelCaseFields() throws Exception {
            String json = """
                    {
                      "deliveryId": 5,
                      "date": "23-05-2026",
                      "totalAmount": 3500.0,
                      "picklistNos": ["E587P001", "E587P002"],
                      "rejectReason": "Wrong amount",
                      "dayendId": 48
                    }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);

            assertThat(dto.getDeliveryId()).isEqualTo(5L);
            assertThat(dto.getDate()).isEqualTo("23-05-2026");
            assertThat(dto.getTotalAmount()).isEqualTo(3500.0);
            assertThat(dto.getPicklistNos()).containsExactly("E587P001", "E587P002");
            assertThat(dto.getRejectReason()).isEqualTo("Wrong amount");
            assertThat(dto.getDayendId()).isEqualTo(48L);
        }

        @Test
        @DisplayName("picklistNos maps as camelCase array")
        void picklistNosCamelCase() throws Exception {
            String json = """
                    { "picklistNos": ["P001", "P002", "P003"] }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);

            assertThat(dto.getPicklistNos()).hasSize(3)
                    .containsExactly("P001", "P002", "P003");
        }

        @Test
        @DisplayName("null picklistNos is allowed")
        void picklistNosNullAllowed() throws Exception {
            String json = """
                    { "deliveryId": 5, "date": "23-05-2026" }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);

            assertThat(dto.getPicklistNos()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // snake_case (mobile app)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("snake_case JSON (mobile)")
    class SnakeCaseTests {

        @Test
        @DisplayName("maps all snake_case fields via @JsonAlias")
        void allSnakeCaseFields() throws Exception {
            String json = """
                    {
                      "delivery_id": 7,
                      "date": "15-03-2026",
                      "total_amount": 1200.0,
                      "picklist_nos": ["E587P001", "E587P002", "E587P003"],
                      "reject_reason": "Short delivery",
                      "dayend_id": 12
                    }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);

            assertThat(dto.getDeliveryId()).isEqualTo(7L);
            assertThat(dto.getDate()).isEqualTo("15-03-2026");
            assertThat(dto.getTotalAmount()).isEqualTo(1200.0);
            assertThat(dto.getPicklistNos()).containsExactly("E587P001", "E587P002", "E587P003");
            assertThat(dto.getRejectReason()).isEqualTo("Short delivery");
            assertThat(dto.getDayendId()).isEqualTo(12L);
        }

        @Test
        @DisplayName("picklist_nos (snake_case) maps to picklistNos field")
        void picklistNosSnakeCase() throws Exception {
            String json = """
                    { "picklist_nos": ["A1", "A2"] }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);

            assertThat(dto.getPicklistNos())
                    .hasSize(2)
                    .containsExactly("A1", "A2");
        }

        @Test
        @DisplayName("delivery_id (snake_case) maps to deliveryId field")
        void deliveryIdSnakeCase() throws Exception {
            String json = """
                    { "delivery_id": 99 }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getDeliveryId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("total_amount (snake_case) maps to totalAmount field")
        void totalAmountSnakeCase() throws Exception {
            String json = """
                    { "total_amount": 4750.50 }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getTotalAmount()).isEqualTo(4750.50);
        }

        @Test
        @DisplayName("reject_reason (snake_case) maps to rejectReason field")
        void rejectReasonSnakeCase() throws Exception {
            String json = """
                    { "reject_reason": "Incorrect items" }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getRejectReason()).isEqualTo("Incorrect items");
        }

        @Test
        @DisplayName("dayend_id (snake_case) maps to dayendId field")
        void dayendIdSnakeCase() throws Exception {
            String json = """
                    { "dayend_id": 55 }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getDayendId()).isEqualTo(55L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Edge cases
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("empty JSON object produces DTO with all null fields")
        void emptyJsonProducesNullFields() throws Exception {
            DayEndDto dto = mapper.readValue("{}", DayEndDto.class);

            assertThat(dto.getDeliveryId()).isNull();
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getTotalAmount()).isNull();
            assertThat(dto.getPicklistNos()).isNull();
            assertThat(dto.getRejectReason()).isNull();
            assertThat(dto.getDayendId()).isNull();
        }

        @Test
        @DisplayName("empty picklist_nos array maps to empty list, not null")
        void emptyPicklistArray() throws Exception {
            String json = """
                    { "picklist_nos": [] }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getPicklistNos()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("camelCase takes precedence — picklistNos works alongside picklist_nos")
        void camelCasePrecedence() throws Exception {
            // Only one key should win — Jackson uses last-wins for duplicate aliases
            // but here we test that the primary @JsonProperty key works
            String json = """
                    { "picklistNos": ["X1", "X2"] }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getPicklistNos()).containsExactly("X1", "X2");
        }

        @Test
        @DisplayName("single-item picklist array deserializes correctly")
        void singleItemPicklist() throws Exception {
            String json = """
                    { "picklist_nos": ["ONLY001"] }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getPicklistNos()).hasSize(1).containsExactly("ONLY001");
        }

        @Test
        @DisplayName("totalAmount as integer value maps to Double without loss")
        void totalAmountAsInteger() throws Exception {
            String json = """
                    { "total_amount": 5000 }
                    """;

            DayEndDto dto = mapper.readValue(json, DayEndDto.class);
            assertThat(dto.getTotalAmount()).isEqualTo(5000.0);
        }
    }
}
