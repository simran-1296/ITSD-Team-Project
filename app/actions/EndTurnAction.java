package actions;

/**
 * ASSUMPTIONS:
 * - EndTurnAction is a simple intent created by controllers.
 * - Sprint 2 needs end-turn to trigger mana refresh + draw.
 */
public final class EndTurnAction implements GameAction {
    private final int playerId;

    public EndTurnAction(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() { return playerId; }
}
