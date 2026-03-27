package actions;

import structures.Pos;

/**
 * Represents "player plays one card from hand to a target board position".
 */
public final class PlayCardAction implements GameAction {
    private final int playerId;
    private final int handIndex;
    private final Pos targetPos;

    public PlayCardAction(int playerId, int handIndex, Pos targetPos) {
        this.playerId = playerId;
        this.handIndex = handIndex;
        this.targetPos = targetPos;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getHandIndex() {
        return handIndex;
    }

    public Pos getTargetPos() {
        return targetPos;
    }
}