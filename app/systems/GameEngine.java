package systems;

import actions.EndTurnAction;
import actions.GameAction;
import actions.PlayCardAction;
import effects.GameEffect;
import structures.GameState;

import java.util.Collections;
import java.util.List;

/**
 * GameEngine routes actions to systems.
 * TODO Fix 3: wire to real event handlers.
 */
public final class GameEngine {

    private GameEngine() {}

    public static List<GameEffect> apply(GameState state, GameAction action) {
        if (state == null || action == null) return Collections.emptyList();

        if (action instanceof PlayCardAction) {
            return CardSystem.playCard(state, (PlayCardAction) action);
        }

        if (action instanceof EndTurnAction) {
            return TurnSystem.endTurn(state, (EndTurnAction) action);
        }

        return Collections.emptyList();
    }
}
