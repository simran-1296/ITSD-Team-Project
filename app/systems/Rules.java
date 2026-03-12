package systems;

import structures.GameState;
import structures.Pos;
import structures.basic.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains legality checks and valid-target query methods.
 * TODO Fix 3: implement using real structures.GameState API.
 */
public final class Rules {

    private Rules() {}

    public static boolean isCurrentPlayersTurn(GameState state, int playerId) {
        return state != null && state.getCurrentTurn() == playerId;
    }

    public static List<Pos> getValidSummonTiles(GameState state, int playerId) {
        List<Pos> valid = new ArrayList<>();
        if (state == null || (playerId != 1 && playerId != 2)) return valid;

        MovementSystem.getValidSummonTiles(state, playerId)
            .forEach(tile -> valid.add(new Pos(tile.getTilex(), tile.getTiley())));
        return valid;
    }

    public static boolean canPlayCreatureAt(GameState state, int playerId, int handIndex, Pos target) {
        if (state == null || target == null) return false;
        if (playerId != 1 && playerId != 2) return false;
        if (state.getPlayer1() == null || state.getPlayer2() == null) return false;
        if (!isCurrentPlayersTurn(state, playerId)) return false;

        if (state.getTile(target.x, target.y) == null) return false;
        if (state.getUnitOnTile(target.x, target.y) != null) return false;

        List<Card> hand = (playerId == 1) ? state.getPlayer1Hand() : state.getPlayer2Hand();
        if (hand == null || handIndex < 0 || handIndex >= hand.size()) return false;

        Card card = hand.get(handIndex);
        if (card == null || !card.getIsCreature()) return false;

        int mana = (playerId == 1) ? state.getPlayer1().getMana() : state.getPlayer2().getMana();
        if (mana < card.getManacost()) return false;

        List<Pos> summonTiles = getValidSummonTiles(state, playerId);
        for (Pos p : summonTiles) {
            if (p.x == target.x && p.y == target.y) return true;
        }
        return false;
    }

    public static boolean canUnitActThisTurn(structures.Unit u) {
        if (u == null) return false;
        return !u.hasStatus(abilities.Status.SUMMONING_SICKNESS)
            && !u.hasStatus(abilities.Status.STUNNED);
    }
}
