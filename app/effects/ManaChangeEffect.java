package effects;

/**
 * ASSUMPTIONS:
 * - Sprint 2 needs mana updates to display after spending.
 */
public final class ManaChangeEffect implements GameEffect {
    private final int playerId;
    private final int newMana;

    public ManaChangeEffect(int playerId, int newMana) {
        this.playerId = playerId;
        this.newMana = newMana;
    }

    public int getPlayerId() { return playerId; }
    public int getNewMana() { return newMana; }
}
