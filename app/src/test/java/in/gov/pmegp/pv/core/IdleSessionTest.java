package in.gov.pmegp.pv.core;

import static org.junit.Assert.*;
import org.junit.Test;
import in.gov.pmegp.pv.core.session.IdleSession;
import in.gov.pmegp.pv.core.session.IdleSession.Tick;

/** C01 — the timing rules, without waiting 25 minutes. */
public class IdleSessionTest {

    private static final class FakeClock implements IdleSession.Clock {
        long now = 1_000_000L;
        public long nowMs() { return now; }
        void advanceMinutes(long m) { now += m * 60_000L; }
    }

    @Test public void c01_r01_timeoutAndWarnValues() {
        assertEquals(25 * 60_000L, IdleSession.TIMEOUT_MS);
        assertEquals( 2 * 60_000L, IdleSession.WARN_BEFORE_MS);
    }

    @Test public void c01_r05_warnsAtTwentyThreeMinutes() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c); s.start();
        c.advanceMinutes(22); assertEquals(Tick.NONE, s.check());
        c.advanceMinutes(1);  assertEquals(Tick.WARN, s.check());
    }

    @Test public void c01_r04_warnsOnlyOncePerIdlePeriod() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c); s.start();
        c.advanceMinutes(23);
        assertEquals(Tick.WARN, s.check());
        assertEquals(Tick.NONE, s.check());
    }

    @Test public void c01_r02_touchResetsTheClockAndRearmsTheWarning() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c); s.start();
        c.advanceMinutes(23);
        assertEquals(Tick.WARN, s.check());
        s.touch();                                   // officer taps
        c.advanceMinutes(22); assertEquals(Tick.NONE, s.check());
        c.advanceMinutes(1);  assertEquals(Tick.WARN, s.check());   // warns again
    }

    @Test public void c01_r05_expiresAtTwentyFiveMinutes() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c); s.start();
        c.advanceMinutes(25);
        assertEquals(Tick.EXPIRED, s.check());
        assertFalse(s.isActive());
    }

    /** C01-R03 — remaining is computed, so a tap elsewhere shows up immediately. */
    @Test public void c01_r03_remainingIsComputedNotCountedDown() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c); s.start();
        c.advanceMinutes(10);
        assertEquals(15 * 60_000L, s.remainingMs());
        s.touch();
        assertEquals(25 * 60_000L, s.remainingMs());
    }

    @Test public void c01_r02_touchOnAnInactiveSessionDoesNothing() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c);
        s.touch();
        assertFalse(s.isActive());
        assertEquals(Tick.NONE, s.check());
    }

    @Test public void c01_r09_resumeWithinTheWindowSucceeds() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c);
        long stored = c.now; c.advanceMinutes(10);
        assertTrue(s.tryResume(stored, true));
        assertTrue(s.isActive());
        assertEquals(15 * 60_000L, s.remainingMs());
    }

    @Test public void c01_r10_resumePastTheWindowFailsAndClears() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c);
        long stored = c.now; c.advanceMinutes(26);
        assertFalse(s.tryResume(stored, true));
        assertFalse(s.isActive());
    }

    /** C01-R09 — an unreadable login blob must end the session, not half-restore it. */
    @Test public void c01_r09_resumeWithoutALoginBlobFails() {
        FakeClock c = new FakeClock();
        IdleSession s = new IdleSession(c);
        long stored = c.now; c.advanceMinutes(5);
        assertFalse(s.tryResume(stored, false));
        assertFalse(s.isActive());
    }

    @Test public void c01_r09_resumeWithNoStoredTimestampFails() {
        assertFalse(new IdleSession(new FakeClock()).tryResume(0, true));
    }
}
