package effects;

/**
 * ASSUMPTIONS:
 * - UI can respond to a draw by refreshing hand display.
 */
public final class DrawCardEffect implements GameEffect {
    private final int playerId;
    private final int newHandSize;

    public DrawCardEffect(int playerId, int newHandSize) {
        this.playerId = playerId;
        this.newHandSize = newHandSize;
    }

    public int getPlayerId() { return playerId; }
    public int getNewHandSize() { return newHandSize; }
}
