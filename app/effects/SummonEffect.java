package effects;

import structures.Pos;

/**
 * ASSUMPTIONS:
 * - Sprint 2 summon should produce a SummonEffect for UI animation.
 * - We include unitId + position; UI can look up stats from GameState.
 */
public final class SummonEffect implements GameEffect {
    private final int unitId;
    private final Pos pos;

    public SummonEffect(int unitId, Pos pos) {
        this.unitId = unitId;
        this.pos = pos;
    }

    public int getUnitId() { return unitId; }
    public Pos getPos() { return pos; }
}
