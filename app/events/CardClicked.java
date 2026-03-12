package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import systems.Rules;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Indicates that the user has clicked a card in their hand.
 *
 * { messageType = "cardClicked", position = <1-indexed hand position> }
 */
public class CardClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        if (gameState.isGameOver() || gameState.isUnitMoving()) return;

        // Only player 1 (human) interacts via UI
        if (gameState.getCurrentTurn() != 1) return;

        int handPosition = message.get("position").asInt();  // 1-indexed
        int handIndex = handPosition - 1;

        List<Card> hand = gameState.getPlayer1Hand();
        if (handIndex < 0 || handIndex >= hand.size()) return;

        Card card = hand.get(handIndex);

        // Check affordability
        int mana = gameState.getPlayer1().getMana();
        if (card.getManacost() > mana) {
            // Cannot afford — ignore click
            return;
        }

        // Clicking the same card again deselects it
        if (gameState.getSelectedCard() != null
                && gameState.getSelectedCardHandPosition() == handPosition) {
            OtherClicked.clearHighlights(out, gameState);
            gameState.clearSelection();
            // Redraw card as unselected
            BasicCommands.drawCard(out, card, handPosition, 0);
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
            return;
        }

        // Clear any previous selection and highlights
        if (gameState.getSelectedCard() != null) {
            BasicCommands.drawCard(out, gameState.getSelectedCard(),
                    gameState.getSelectedCardHandPosition(), 0);
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        OtherClicked.clearHighlights(out, gameState);
        gameState.clearSelection();

        // Select the card
        gameState.setSelectedCard(card);
        gameState.setSelectedCardHandPosition(handPosition);

        // Highlight the card as selected (mode 1)
        BasicCommands.drawCard(out, card, handPosition, 1);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

        if (card.getIsCreature()) {
            // Highlight valid summon tiles (mode 1 = blue)
            List<Tile> summonTiles = Rules.getValidSummonTiles(gameState, 1).stream()
                    .map(pos -> gameState.getTile(pos.x, pos.y))
                    .filter(tile -> tile != null)
                    .collect(Collectors.toList());
            for (Tile tile : summonTiles) {
                BasicCommands.drawTile(out, tile, 1);
                try { Thread.sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            gameState.setHighlightedTiles(summonTiles);
        }
        // Spell cards: no summon tile highlights (spell targets handled via tile clicks if needed)
    }
}
