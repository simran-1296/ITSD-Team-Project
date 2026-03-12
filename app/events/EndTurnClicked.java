package events;

import com.fasterxml.jackson.databind.JsonNode;

import actions.EndTurnAction;
import akka.actor.ActorRef;
import structures.GameState;
import commands.BasicCommands;
import effects.DrawCardEffect;
import effects.ErrorEffect;
import effects.GameEffect;
import systems.GameEngine;

import java.util.List;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = “endTurnClicked”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (gameState.isGameOver() || gameState.isUnitMoving()) return;

		EndTurnAction action = new EndTurnAction(gameState.getCurrentTurn());
		List<GameEffect> effects = GameEngine.apply(gameState, action);

		for (GameEffect effect : effects) {
			if (effect instanceof DrawCardEffect) {
				DrawCardEffect draw = (DrawCardEffect) effect;
				if (draw.getPlayerId() == 1 && draw.getNewHandSize() > 0) {
					BasicCommands.drawCard(
						out,
						gameState.getPlayer1Hand().get(draw.getNewHandSize() - 1),
						draw.getNewHandSize(),
						0
					);
				}
			}
			if (effect instanceof ErrorEffect) {
				BasicCommands.addPlayer1Notification(out, ((ErrorEffect) effect).getMessage(), 2);
				return;
			}
		}

		// Mana is always synced after a successful end-turn state transition.
		BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
		BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());

		if (gameState.getCurrentTurn() == 1) {
			BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
		} else {
			BasicCommands.addPlayer1Notification(out, "AI's Turn", 2);
		}
	}

}
