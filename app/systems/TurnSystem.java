package systems;

import actions.EndTurnAction;
import effects.GameEffect;
import structures.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * TurnSystem is not used in current implementation.
 */
public final class TurnSystem {

    private TurnSystem() {
    }

    public static List<GameEffect> endTurn(GameState state, EndTurnAction action) {
        return new ArrayList<>();
    }
}