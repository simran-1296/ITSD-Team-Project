package systems;

import actions.PlayCardAction;
import effects.ErrorEffect;
import effects.GameEffect;
import effects.ManaChangeEffect;
import effects.SummonEffect;
import structures.GameState;
import structures.GameUnit;
import structures.Pos;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Unit;
import utils.BasicObjectBuilders;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles mana deduction, summoning and spell execution.
 * TODO Fix 3: implement using real structures.GameState API and structures.basic.Card.
 */
public final class CardSystem {

    private CardSystem() {}

    public static List<GameEffect> playCard(GameState state, PlayCardAction action) {
        List<GameEffect> effects = new ArrayList<>();
        if (state == null || action == null) return effects;

        int playerId = action.getPlayerId();
        if (playerId != 1 && playerId != 2) {
            effects.add(new ErrorEffect("Unknown player."));
            return effects;
        }
        int handIndex = action.getHandIndex();
        Pos targetPos = action.getTargetPos();

        if (!Rules.canPlayCreatureAt(state, playerId, handIndex, targetPos)) {
            effects.add(new ErrorEffect("Invalid creature play."));
            return effects;
        }

        List<Card> hand = (playerId == 1) ? state.getPlayer1Hand() : state.getPlayer2Hand();
        Player player = (playerId == 1) ? state.getPlayer1() : state.getPlayer2();
        Card card = hand.get(handIndex);

        int unitId = state.getAndIncrementUnitId();
        Unit unitBase = BasicObjectBuilders.loadUnit(card.getUnitConfig(), unitId, Unit.class);
        if (unitBase == null) {
            effects.add(new ErrorEffect("Failed to load unit config for summoned creature."));
            return effects;
        }

        // Mutations are applied only after all validations/loaders succeed.
        player.setMana(player.getMana() - card.getManacost());
        hand.remove(handIndex);

        unitBase.setPositionByTile(state.getTile(targetPos.x, targetPos.y));

        int attack = (card.getBigCard() != null) ? card.getBigCard().getAttack() : 1;
        int health = (card.getBigCard() != null) ? card.getBigCard().getHealth() : 1;

        GameUnit summoned = new GameUnit(unitBase, playerId, attack, health, false);
        summoned.setHasMoved(true);   // Summoning sickness for Sprint 2.
        summoned.setHasAttacked(true);
        summoned.setCardName(card.getCardname());

        state.placeUnit(targetPos.x, targetPos.y, summoned);

        effects.add(new ManaChangeEffect(playerId, player.getMana()));
        effects.add(new SummonEffect(unitId, targetPos));
        return effects;
    }
}
