package in.gov.pmegp.pv.validation;

import static org.junit.Assert.*;
import org.junit.Test;
import in.gov.pmegp.pv.ui.unitverification.validation.StageGuard;
import in.gov.pmegp.pv.ui.unitverification.validation.StageGuard.Block;
import in.gov.pmegp.pv.ui.unitverification.validation.StageGuard.State;

/** C06 — the guards that made the step indicator stop being a bypass. */
public class StageGuardTest {

    private State allValid() {
        State s = new State();
        s.editable = true; s.stage6Valid = true; s.stage7Valid = true; s.stage8HasDocument = true;
        return s;
    }

    @Test public void c06_r07_rangeAndSameStep() {
        assertFalse(StageGuard.isNavigable(5, 5));
        assertFalse(StageGuard.isNavigable(5, 0));
        assertFalse(StageGuard.isNavigable(5, 11));
        assertTrue(StageGuard.isNavigable(5, 6));
    }

    /** C06-R08 — the whole point: the STEP INDICATOR is guarded, not just the button. */
    @Test public void c06_r08_stage7InvalidBlocksAJumpToStage10() {
        State s = allValid(); s.stage7Valid = false;
        assertEquals(Block.STAGE_7_INVALID, StageGuard.blockedBy(7, 10, s));
    }

    @Test public void c06_r08_stage6InvalidBlocksForward() {
        State s = allValid(); s.stage6Valid = false;
        assertEquals(Block.STAGE_6_INVALID, StageGuard.blockedBy(6, 9, s));
    }

    @Test public void c06_r08_stage8WithoutADocumentBlocksForward() {
        State s = allValid(); s.stage8HasDocument = false;
        assertEquals(Block.STAGE_8_NO_DOCUMENT, StageGuard.blockedBy(8, 9, s));
    }

    /** C06-R08 — backward is always free, even from an invalid stage. */
    @Test public void c06_r08_backwardIsAlwaysAllowed() {
        State s = allValid(); s.stage7Valid = false;
        assertEquals(Block.NONE, StageGuard.blockedBy(7, 6, s));
        assertEquals(Block.NONE, StageGuard.blockedBy(7, 1, s));
    }

    /** C06-R09 — stages 7 and 8 respect read-only; stage 6 does NOT (F-007). */
    @Test public void c06_r09_readOnlyCaseSkipsTheStage7And8Guards() {
        State s = allValid();
        s.editable = false; s.stage7Valid = false; s.stage8HasDocument = false;
        assertEquals(Block.NONE, StageGuard.blockedBy(7, 10, s));
        assertEquals(Block.NONE, StageGuard.blockedBy(8, 9, s));
    }

    @Test public void f007_stage6GuardFiresEvenOnAReadOnlyCase() {
        State s = allValid(); s.editable = false; s.stage6Valid = false;
        assertEquals("F-007: carried verbatim pending a device check",
                Block.STAGE_6_INVALID, StageGuard.blockedBy(6, 7, s));
    }

    /** C06-R12 */
    @Test public void c06_r12_leavingStage10ResetsTheOtpFlow() {
        assertTrue(StageGuard.shouldResetOtp(10, 9));
        assertTrue(StageGuard.shouldResetOtp(10, 1));
        assertFalse(StageGuard.shouldResetOtp(9, 10));
        assertFalse(StageGuard.shouldResetOtp(10, 10));
    }

    @Test public void c06_r08_validStagesDoNotBlock() {
        assertEquals(Block.NONE, StageGuard.blockedBy(6, 7, allValid()));
        assertEquals(Block.NONE, StageGuard.blockedBy(7, 8, allValid()));
        assertEquals(Block.NONE, StageGuard.blockedBy(8, 9, allValid()));
    }
}
