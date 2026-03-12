package events;

import com.fasterxml.jackson.databind.JsonNode;

import actions.PlayCardAction;
import akka.actor.ActorRef;
import commands.BasicCommands;
import effects.ErrorEffect;
import effects.GameEffect;
import effects.ManaChangeEffect;
import effects.SummonEffect;
import structures.GameState;
import structures.GameUnit;
import structures.Pos;
import structures.basic.Card;
import structures.basic.Tile;
import systems.CombatSystem;
import systems.GameEngine;
import systems.MovementSystem;

import java.util.List;

/**
 * Indicates that the user has clicked a tile on the board.
 *
 * { messageType = "tileClicked", tilex = <x>, tiley = <y> }
 */
public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        if (gameState.isGameOver() || gameState.isUnitMoving()) return;

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        Tile clickedTile = gameState.getTile(tilex, tiley);
        if (clickedTile == null) return;

        // --- Branch 1: A card is currently selected → summon attempt ---
        if (gameState.getSelectedCard() != null) {
            handleCardSelected(out, gameState, tilex, tiley, clickedTile);
            return;
        }

        // --- Branch 2: A unit is currently selected ---
        if (gameState.getSelectedUnit() != null) {
            handleUnitSelected(out, gameState, tilex, tiley, clickedTile);
            return;
        }

        // --- Branch 3: Nothing selected → try to select a friendly unit ---
        handleNoSelection(out, gameState, tilex, tiley);
    }

    // -------------------------------------------------------------------------
    // Card selected: try to summon on the clicked tile
    // -------------------------------------------------------------------------
    private void handleCardSelected(ActorRef out, GameState gameState,
                                    int tilex, int tiley, Tile clickedTile) {
        Card card = gameState.getSelectedCard();
        int handPosition = gameState.getSelectedCardHandPosition();
        int handIndex = handPosition - 1;

        // Check if clicked tile is a valid summon tile (highlighted)
        boolean isValidSummon = gameState.getHighlightedTiles().stream()
                .anyMatch(t -> t.getTilex() == tilex && t.getTiley() == tiley);

        // Clear highlights and card selection first
        OtherClicked.clearHighlights(out, gameState);
        BasicCommands.drawCard(out, card, handPosition, 0);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        gameState.clearSelection();

        if (!isValidSummon) return;

        PlayCardAction action = new PlayCardAction(1, handIndex, new Pos(tilex, tiley));
        List<GameEffect> effects = GameEngine.apply(gameState, action);
        boolean didSummon = false;

        for (GameEffect effect : effects) {
            if (effect instanceof ManaChangeEffect) {
                ManaChangeEffect mana = (ManaChangeEffect) effect;
                if (mana.getPlayerId() == 1) {
                    BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
                } else {
                    BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());
                }
                try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
            } else if (effect instanceof SummonEffect) {
                didSummon = true;
                drawSummonedUnit(out, gameState, ((SummonEffect) effect).getPos());
            } else if (effect instanceof ErrorEffect) {
                BasicCommands.addPlayer1Notification(out, ((ErrorEffect) effect).getMessage(), 2);
                return;
            }
        }

        if (didSummon) {
            redrawPlayer1Hand(out, gameState);
        }
    }

    // -------------------------------------------------------------------------
    // Unit selected: handle move, attack, re-select, or deselect
    // -------------------------------------------------------------------------
    private void handleUnitSelected(ActorRef out, GameState gameState,
                                    int tilex, int tiley, Tile clickedTile) {
        GameUnit selected = gameState.getSelectedUnit();

        // Check if clicked on a movement tile
        boolean isMoveTarget = gameState.getHighlightedTiles().stream()
                .anyMatch(t -> t.getTilex() == tilex && t.getTiley() == tiley);

        // Check if clicked on an attack tile
        boolean isAttackTarget = gameState.getAttackHighlightedTiles().stream()
                .anyMatch(t -> t.getTilex() == tilex && t.getTiley() == tiley);

        GameUnit unitOnTile = gameState.getUnitOnTile(tilex, tiley);

        // --- Move ---
        if (isMoveTarget && !selected.hasMoved()) {
            OtherClicked.clearHighlights(out, gameState);
            gameState.clearSelection();

            // Animate movement
            gameState.moveUnit(selected, tilex, tiley);
            BasicCommands.moveUnitToTile(out, selected.getUnit(), clickedTile);
            selected.setHasMoved(true);

            // Wait for UnitStopped event to unlock, but show attack highlights now
            // We re-select the unit to show new attack tiles
            List<Tile> newAttackTiles = MovementSystem.getAttackableTiles(gameState, selected);
            if (!selected.hasAttacked() && !newAttackTiles.isEmpty()) {
                for (Tile t : newAttackTiles) {
                    BasicCommands.drawTile(out, t, 2);
                    try { Thread.sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                gameState.setAttackHighlightedTiles(newAttackTiles);
                gameState.setSelectedUnit(selected);
            }
            return;
        }

        // --- Attack ---
        if (isAttackTarget && !selected.hasAttacked() && unitOnTile != null
                && unitOnTile.getOwner() != selected.getOwner()) {

            OtherClicked.clearHighlights(out, gameState);
            gameState.clearSelection();

            // If the attacker hasn't moved and isn't adjacent, move adjacent first
            if (!selected.hasMoved() && !isAdjacent(selected.getTileX(), selected.getTileY(), tilex, tiley)) {
                // Find a move tile adjacent to the target
                Tile moveTile = findMoveAdjacentTo(gameState, selected, tilex, tiley);
                if (moveTile != null) {
                    gameState.moveUnit(selected, moveTile.getTilex(), moveTile.getTiley());
                    BasicCommands.moveUnitToTile(out, selected.getUnit(), moveTile);
                    selected.setHasMoved(true);
                    // Brief wait for movement
                    try { Thread.sleep(1500); } catch (InterruptedException e) { e.printStackTrace(); }
                }
            }

            CombatSystem.executeAttack(out, gameState, selected, unitOnTile);
            if (!gameState.isGameOver()) {
                selected.setHasAttacked(true);
            }
            return;
        }

        // --- Re-select same unit: deselect ---
        if (unitOnTile == selected) {
            OtherClicked.clearHighlights(out, gameState);
            gameState.clearSelection();
            return;
        }

        // --- Click on another friendly unit: switch selection ---
        if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentTurn()
                && canUnitAct(unitOnTile)) {
            OtherClicked.clearHighlights(out, gameState);
            gameState.clearSelection();
            selectUnit(out, gameState, unitOnTile);
            return;
        }

        // --- Anything else: deselect ---
        OtherClicked.clearHighlights(out, gameState);
        gameState.clearSelection();
    }

    // -------------------------------------------------------------------------
    // Nothing selected: try to select a friendly unit on the clicked tile
    // -------------------------------------------------------------------------
    private void handleNoSelection(ActorRef out, GameState gameState, int tilex, int tiley) {
        GameUnit unit = gameState.getUnitOnTile(tilex, tiley);
        if (unit == null) return;
        if (unit.getOwner() != gameState.getCurrentTurn()) return;
        if (!canUnitAct(unit)) return;

        selectUnit(out, gameState, unit);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Highlights valid move and attack tiles for a unit and stores selection. */
    private void selectUnit(ActorRef out, GameState gameState, GameUnit unit) {
        gameState.setSelectedUnit(unit);

        // Movement highlights (mode 1 = blue) — only if unit hasn't moved
        if (!unit.hasMoved()) {
            List<Tile> moveTiles = MovementSystem.getValidMoveTiles(gameState, unit);
            for (Tile t : moveTiles) {
                BasicCommands.drawTile(out, t, 1);
                try { Thread.sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            gameState.setHighlightedTiles(moveTiles);
        }

        // Attack highlights (mode 2 = red) — only if unit hasn't attacked
        if (!unit.hasAttacked()) {
            List<Tile> atkTiles = unit.hasMoved()
                    ? MovementSystem.getAttackableTiles(gameState, unit)
                    : MovementSystem.getAllAttackableTiles(gameState, unit);
            for (Tile t : atkTiles) {
                BasicCommands.drawTile(out, t, 2);
                try { Thread.sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            gameState.setAttackHighlightedTiles(atkTiles);
        }
    }

    private boolean canUnitAct(GameUnit unit) {
        return !unit.hasMoved() || !unit.hasAttacked();
    }

    private boolean isAdjacent(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1;
    }

    /**
     * Finds a valid move tile that is adjacent to the target position.
     * Used for move-then-attack when the attacker is not yet adjacent.
     */
    private Tile findMoveAdjacentTo(GameState gameState, GameUnit unit, int targetX, int targetY) {
        List<Tile> moveTiles = MovementSystem.getValidMoveTiles(gameState, unit);
        for (Tile t : moveTiles) {
            if (isAdjacent(t.getTilex(), t.getTiley(), targetX, targetY)) {
                return t;
            }
        }
        return null;
    }

    private void redrawPlayer1Hand(ActorRef out, GameState gameState) {
        List<Card> hand = gameState.getPlayer1Hand();
        for (int i = 0; i < hand.size(); i++) {
            BasicCommands.drawCard(out, hand.get(i), i + 1, 0);
            try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        BasicCommands.deleteCard(out, hand.size() + 1);
        try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void drawSummonedUnit(ActorRef out, GameState gameState, Pos pos) {
        Tile tile = gameState.getTile(pos.x, pos.y);
        GameUnit unit = gameState.getUnitOnTile(pos.x, pos.y);
        if (tile == null || unit == null) return;

        BasicCommands.drawUnit(out, unit.getUnit(), tile);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

        BasicCommands.setUnitAttack(out, unit.getUnit(), unit.getAttack());
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

        BasicCommands.setUnitHealth(out, unit.getUnit(), unit.getHealth());
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
