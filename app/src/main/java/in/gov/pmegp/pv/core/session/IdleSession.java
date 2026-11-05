package in.gov.pmegp.pv.core.session;

import java.util.concurrent.TimeUnit;

/**
 * Idle-timeout session clock.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C01-session-and-auth.md
 *
 * <p><b>Honest scope (C01 §1):</b> the API does not authenticate on any endpoint except
 * logout, so there is nothing to keep alive server-side. This only decides whether the
 * app should still be showing a logged-in screen. A user who clears app data bypasses it.
 * It is NOT a security boundary — do not "harden" it with client-side tricks.
 *
 * <p>Pure clock logic, injectable time source, so every timing rule is unit-testable
 * without waiting 25 minutes.
 */
public final class IdleSession {

    /** C01-R01 */
    public static final long TIMEOUT_MS     = TimeUnit.MINUTES.toMillis(25);
    public static final long WARN_BEFORE_MS = TimeUnit.MINUTES.toMillis(2);

    public interface Clock { long nowMs(); }

    public enum Tick { NONE, WARN, EXPIRED }

    private final Clock clock;
    private long lastActiveMs;
    private boolean active;
    private boolean warned;

    public IdleSession(Clock clock) { this.clock = clock; }

    public void start() {
        active = true;
        warned = false;
        touch();
    }

    /**
     * C01-R02 — activity is the officer TOUCHING THE SCREEN, nothing else.
     * A background upload finishing is not presence and must not reset the clock.
     */
    public void touch() {
        if (!active) return;
        lastActiveMs = clock.nowMs();
        warned = false;          // C01-R04: a fresh idle period warns again
    }

    public void end() {
        active = false;
        warned = false;
        lastActiveMs = 0;
    }

    public boolean isActive() { return active; }

    /** C01-R03 — computed on each read, never counted down by its own ticker. */
    public long remainingMs() {
        return active ? TIMEOUT_MS - (clock.nowMs() - lastActiveMs) : 0;
    }

    /**
     * C01-R05 — evaluated on a foreground timer AND whenever the app resumes.
     * A timer does not run while backgrounded; without the resume check an app
     * backgrounded for an hour would still look logged in.
     */
    public Tick check() {
        if (!active) return Tick.NONE;
        long idle = clock.nowMs() - lastActiveMs;
        if (idle >= TIMEOUT_MS) {
            end();
            return Tick.EXPIRED;
        }
        if (!warned && idle >= TIMEOUT_MS - WARN_BEFORE_MS) {
            warned = true;                       // C01-R04: once per idle period
            return Tick.WARN;
        }
        return Tick.NONE;
    }

    /**
     * C01-R09 — resume requires BOTH a parseable timestamp within the window AND a
     * readable login blob. Any failure ends the session rather than restoring a half
     * state, because the dashboard cannot be rebuilt without the login response.
     *
     * <p>C01-R07: the timestamp is stored as epoch millis, not a formatted string —
     * .NET's round-trip DateTime format has no exact Java equivalent.
     */
    public boolean tryResume(long storedLastActiveMs, boolean loginBlobReadable) {
        if (storedLastActiveMs <= 0) return false;
        if (clock.nowMs() - storedLastActiveMs >= TIMEOUT_MS) { end(); return false; }
        if (!loginBlobReadable) { end(); return false; }
        lastActiveMs = storedLastActiveMs;
        active = true;
        warned = false;
        return true;
    }
}
