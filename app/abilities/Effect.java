package abilities;

import akka.actor.ActorRef;
import structures.GameState;
import structures.GameUnit;

/**
 * Interface for any logic executed when a trigger fires.
 * Implementations encode a specific ability effect (e.g. deal 1 damage, draw a card).
 * Member A - Sprint 3.
 */
public interface Effect {

    /**
     * Executes this effect.
     *
     * @param out       WebSocket actor for sending commands to the front-end
     * @param state     current game state
     * @param source    the unit that owns this triggered effect (may be null for global effects)
     */
    void execute(ActorRef out, GameState state, GameUnit source);
}
