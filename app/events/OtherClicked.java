package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * somewhere that is not on a card tile or the end-turn button.
 *
 * {
 *   messageType = "otherClicked"
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class OtherClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (gameState.isGameOver() || gameState.isUnitMoving()) return;

		clearHighlights(out, gameState);
		gameState.clearSelection();
	}

	static void clearHighlights(ActorRef out, GameState gameState) {
		for (Tile tile : gameState.getHighlightedTiles()) {
			BasicCommands.drawTile(out, tile, 0);
		}
		gameState.getHighlightedTiles().clear();

		for (Tile tile : gameState.getAttackHighlightedTiles()) {
			BasicCommands.drawTile(out, tile, 0);
		}
		gameState.getAttackHighlightedTiles().clear();
	}
}
