package systems;

import structures.GameState;
import structures.GameUnit;
import structures.Pos;
import abilities.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains legality checks and valid-target query methods.
 * TODO Fix 3: implement using real structures.GameState API.
 */
public final class Rules {

    private Rules() {
    }

    public static boolean isCurrentPlayersTurn(GameState state, int playerId) {
        return state.getCurrentTurn() == playerId;
    }

    public static List<Pos> getValidSummonTiles(GameState state, int playerId) {
        // TODO Fix 3: implement adjacency-based summon tile logic.
        return new ArrayList<>();
    }

    public static boolean canPlayCreatureAt(GameState state, int playerId, int handIndex, Pos target) {
        // TODO Fix 3: implement full legality check.
        return false;
    }

    public static boolean canUnitActThisTurn(GameUnit u) {
        if (u == null)
            return false;
        return !u.hasStatus(Status.SUMMONING_SICKNESS)
                && !u.hasStatus(Status.STUNNED);
    }
}
