package systems;

import actions.EndTurnAction;
import effects.DrawCardEffect;
import effects.ErrorEffect;
import effects.GameEffect;
import effects.ManaChangeEffect;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Card;
import structures.basic.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles turn switching, mana refresh, card draw and state reset.
 * TODO Fix 3: implement using real structures.GameState API.
 */
public final class TurnSystem {

    private TurnSystem() {}

    public static List<GameEffect> endTurn(GameState state, EndTurnAction action) {
        List<GameEffect> effects = new ArrayList<>();
        if (state == null || action == null) return effects;

        int endingPlayer = action.getPlayerId();
        if (endingPlayer != 1 && endingPlayer != 2) {
            effects.add(new ErrorEffect("Unknown player."));
            return effects;
        }
        if (!Rules.isCurrentPlayersTurn(state, endingPlayer)) {
            effects.add(new ErrorEffect("Cannot end turn when it is not your turn."));
            return effects;
        }

        // End-of-turn draw for the player whose turn is ending.
        boolean drewCard = drawOneForPlayer(state, endingPlayer);
        if (drewCard) {
            int newHandSize = (endingPlayer == 1)
                ? state.getPlayer1Hand().size()
                : state.getPlayer2Hand().size();
            effects.add(new DrawCardEffect(endingPlayer, newHandSize));
        }

        // Switch active player and round count.
        if (state.getCurrentTurn() == 1) {
            state.setCurrentTurn(2);
        } else {
            state.setCurrentTurn(1);
            state.setTurnNumber(state.getTurnNumber() + 1);
        }

        // Refresh mana for the new active player; unused mana of inactive player is lost.
        Player active = (state.getCurrentTurn() == 1) ? state.getPlayer1() : state.getPlayer2();
        Player inactive = (state.getCurrentTurn() == 1) ? state.getPlayer2() : state.getPlayer1();
        active.refreshMana(state.getTurnNumber());
        inactive.setMana(0);

        effects.add(new ManaChangeEffect(1, state.getPlayer1().getMana()));
        effects.add(new ManaChangeEffect(2, state.getPlayer2().getMana()));

        // New active player's units can act again.
        resetActivePlayerUnits(state);

        // Clear any stale UI selections/highlights stored in state.
        state.clearSelection();
        state.getHighlightedTiles().clear();
        state.getAttackHighlightedTiles().clear();

        return effects;
    }

    private static boolean drawOneForPlayer(GameState state, int playerId) {
        List<Card> deck = (playerId == 1) ? state.getPlayer1Deck() : state.getPlayer2Deck();
        List<Card> hand = (playerId == 1) ? state.getPlayer1Hand() : state.getPlayer2Hand();

        if (deck == null || hand == null) return false;
        if (deck.isEmpty() || hand.size() >= 6) return false;

        hand.add(deck.remove(0));
        return true;
    }

    private static void resetActivePlayerUnits(GameState state) {
        int activePlayerId = state.getCurrentTurn();
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                GameUnit unit = state.getUnitOnTile(x, y);
                if (unit != null && unit.getOwner() == activePlayerId) {
                    unit.resetTurnFlags();
                }
            }
        }
    }
}
