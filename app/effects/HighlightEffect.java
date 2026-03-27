package effects;

import structures.Pos;

import java.util.Collections;
import java.util.List;

/**
 * ASSUMPTIONS:
 * - HighlightEffect supports modes for move/attack/summon (doc mentions modes).
 * - Sprint 2 only uses SUMMON tiles highlighting.
 */
public final class HighlightEffect implements GameEffect {
    public enum Mode {
        MOVE, ATTACK, SUMMON
    }

    private final List<Pos> positions;
    private final Mode mode;

    public HighlightEffect(List<Pos> positions, Mode mode) {
        this.positions = (positions == null) ? List.of() : List.copyOf(positions);
        this.mode = mode;
    }

    public List<Pos> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public Mode getMode() {
        return mode;
    }
}
