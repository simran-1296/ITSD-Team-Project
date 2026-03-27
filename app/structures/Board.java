package structures;

import java.util.HashMap;
import java.util.Map;

/**
 * 9x5 board; tracks unit positions and provides basic queries.
 * Uses 1-based indexing (x in [1..9], y in [1..5]).
 */
public class Board {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 5;

    private final Map<Pos, Integer> posToUnitId = new HashMap<>();

    public boolean inBounds(Pos p) {
        return p != null && p.x >= 1 && p.x <= WIDTH && p.y >= 1 && p.y <= HEIGHT;
    }

    public Integer getUnitIdAt(Pos p) {
        return posToUnitId.get(p);
    }

    public boolean isOccupied(Pos p) {
        return getUnitIdAt(p) != null;
    }

    public void setUnitAt(Pos p, Integer unitId) {
        if (unitId == null) {
            posToUnitId.remove(p);
        } else {
            posToUnitId.put(p, unitId);
        }
    }
}
