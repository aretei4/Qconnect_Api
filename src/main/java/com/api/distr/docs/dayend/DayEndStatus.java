package com.api.distr.docs.dayend;

import java.util.Map;

/**
 * Integer status codes for dayend_approval.status and the dan_approval_log.action column.
 *
 * dayend_approval.status
 *   0 STARTED · 1 PENDING · 2 SK_APPROVED · 3 APPROVED · 4 REJECTED · 5 CLOSED
 *
 * dan_approval_log.action
 *   1 APPROVED · 2 REJECTED
 *
 * DB stores the integer; the API keeps exposing the string names (via name()/code())
 * so no frontend change is needed.
 */
public final class DayEndStatus {

    private DayEndStatus() {}

    // dayend_approval.status
    public static final int STARTED     = 0;
    public static final int PENDING     = 1;
    public static final int SK_APPROVED = 2;
    public static final int APPROVED    = 3;
    public static final int REJECTED    = 4;
    public static final int CLOSED      = 5;

    // dan_approval_log.stage — the DAN lifecycle phase the event belongs to
    public static final int STAGE_STARTED     = 0;
    public static final int STAGE_SUBMITTED   = 1;
    public static final int STAGE_STOREKEEPER = 2;
    public static final int STAGE_ACCOUNTS    = 3;
    public static final int STAGE_CLOSED      = 4;

    // dan_approval_log.action — what happened at that stage
    public static final int ACT_APPROVED = 1;
    public static final int ACT_REJECTED = 2;
    public static final int ACT_CREATED  = 3;
    public static final int ACT_CLOSED   = 4;

    private static final Map<Integer, String> STATUS_NAME = Map.of(
            STARTED, "STARTED", PENDING, "PENDING", SK_APPROVED, "SK_APPROVED",
            APPROVED, "APPROVED", REJECTED, "REJECTED", CLOSED, "CLOSED");

    private static final Map<String, Integer> STATUS_CODE = Map.of(
            "STARTED", STARTED, "PENDING", PENDING, "SK_APPROVED", SK_APPROVED,
            "APPROVED", APPROVED, "REJECTED", REJECTED, "CLOSED", CLOSED);

    private static final Map<Integer, String> STAGE_NAME = Map.of(
            STAGE_STARTED, "STARTED", STAGE_SUBMITTED, "SUBMITTED",
            STAGE_STOREKEEPER, "STOREKEEPER", STAGE_ACCOUNTS, "ACCOUNTS",
            STAGE_CLOSED, "CLOSED");

    private static final Map<String, Integer> STAGE_CODE = Map.of(
            "STARTED", STAGE_STARTED, "SUBMITTED", STAGE_SUBMITTED,
            "STOREKEEPER", STAGE_STOREKEEPER, "ACCOUNTS", STAGE_ACCOUNTS,
            "CLOSED", STAGE_CLOSED);

    private static final Map<Integer, String> ACTION_NAME = Map.of(
            ACT_APPROVED, "APPROVED", ACT_REJECTED, "REJECTED",
            ACT_CREATED,  "CREATED",  ACT_CLOSED,   "CLOSED");

    private static final Map<String, Integer> ACTION_CODE = Map.of(
            "APPROVED", ACT_APPROVED, "REJECTED", ACT_REJECTED,
            "CREATED",  ACT_CREATED,  "CLOSED",   ACT_CLOSED);

    /** status code → name (e.g. 1 → "PENDING"), or null when unknown. */
    public static String statusName(Integer code) {
        return code == null ? null : STATUS_NAME.get(code);
    }

    /** status name → code (e.g. "PENDING" → 1). Throws on an unknown name. */
    public static int statusCode(String name) {
        Integer c = name == null ? null : STATUS_CODE.get(name.trim().toUpperCase());
        if (c == null) throw new IllegalArgumentException("Unknown day-end status: " + name);
        return c;
    }

    /** stage code → name (2 → "STOREKEEPER"), or null when unknown. */
    public static String stageName(Integer code) {
        return code == null ? null : STAGE_NAME.get(code);
    }

    /** stage name → code ("STOREKEEPER" → 2). Throws on an unknown name. */
    public static int stageCode(String name) {
        Integer c = name == null ? null : STAGE_CODE.get(name.trim().toUpperCase());
        if (c == null) throw new IllegalArgumentException("Unknown approval stage: " + name);
        return c;
    }

    /** action code → name (1 → "APPROVED"). */
    public static String actionName(Integer code) {
        return code == null ? null : ACTION_NAME.get(code);
    }

    /** action name → code ("APPROVED" → 1). Throws on an unknown name. */
    public static int actionCode(String name) {
        Integer c = name == null ? null : ACTION_CODE.get(name.trim().toUpperCase());
        if (c == null) throw new IllegalArgumentException("Unknown approval action: " + name);
        return c;
    }
}
