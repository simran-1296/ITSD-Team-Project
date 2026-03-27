package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Indicates that a unit instance has stopped moving.
 *
 * {
 *   messageType = "unitStopped"
 *   id = <unit id>
 * }
 */
public class UnitStopped implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        int unitid = message.get("id").asInt();

        // movement animation finished -> unlock input
        gameState.setUnitMoving(false);
    }
}