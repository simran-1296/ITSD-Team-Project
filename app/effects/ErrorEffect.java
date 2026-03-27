package effects;

/**
 * ASSUMPTIONS:
 * - Used to represent invalid action feedback without mutating state.
 * - UI may ignore this in your real repo; still useful for tests/logging.
 */
public final class ErrorEffect implements GameEffect {
    private final String message;

    public ErrorEffect(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
