package in.gov.pmegp.pv.ui.unitverification.validation;

/**
 * Whether the officer may move from one stage to another.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C06-navigation-and-back.md (C06-R07..R12)
 *
 * <p>Lives here — not in a fragment — so the step indicator and the SAVE &amp; NEXT button
 * run the IDENTICAL code path (C06-R14). Disabling the button alone left the step
 * indicator as an open route past an invalid stage, which is how Stage 10 ended up
 * reporting "Stage 7 details are not filled".
 */
public final class StageGuard {

    public static final int TOTAL_STEPS = 10;

    public enum Block { NONE, STAGE_6_INVALID, STAGE_7_INVALID, STAGE_8_NO_DOCUMENT }

    public static final class State {
        public boolean editable;
        public boolean stage6Valid;
        public boolean stage7Valid;
        public boolean stage8HasDocument;
    }

    private StageGuard() { }

    /** C06-R07 — only 1..TOTAL_STEPS, and only when the target differs. */
    public static boolean isNavigable(int from, int to) {
        return to != from && to >= 1 && to <= TOTAL_STEPS;
    }

    /**
     * C06-R08 — forward guards on 6, 7 and 8. Backward is ALWAYS allowed.
     *
     * <p>Note the asymmetry carried verbatim from the MAUI app: stages 7 and 8 test
     * {@code editable} first, stage 6 does NOT (F-007). Either an oversight or it relies
     * on a read-only stage validating clean. Reproduced as-is pending a device check —
     * do not "tidy" it without confirming.
     */
    public static Block blockedBy(int from, int to, State s) {
        if (!isNavigable(from, to)) return Block.NONE;
        if (to <= from) return Block.NONE;                       // backward is free

        if (from == 6 && !s.stage6Valid) return Block.STAGE_6_INVALID;
        if (from == 7 && s.editable && !s.stage7Valid) return Block.STAGE_7_INVALID;
        if (from == 8 && s.editable && !s.stage8HasDocument) return Block.STAGE_8_NO_DOCUMENT;
        return Block.NONE;
    }

    /**
     * C06-R12 — leaving stage 10 for any other stage resets the OTP cycle. A code
     * generated or validated in one visit must not still stand on the next; Final Submit
     * is the only thing it was for, and it pops the page on success, so reaching here
     * means submit did not happen.
     */
    public static boolean shouldResetOtp(int from, int to) {
        return from == 10 && to != 10;
    }
}
