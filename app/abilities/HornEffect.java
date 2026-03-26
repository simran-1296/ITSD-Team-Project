package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Horn of the Forsaken:
 * On Hit (whenever the equipped avatar deals damage), summon one Wraithling
 * on a random/available adjacent empty tile.
 *
 * Current implementation: first available adjacent tile.
 */
public class HornEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;
        if (!source.isAvatar()) return;
        if (!source.hasArtifact()) return;
        if (!"Horn of the Forsaken".equals(source.getEquippedArtifact())) return;

        int x = source.getTileX();
        int y = source.getTileY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;

                Tile tile = state.getTile(nx, ny);
                if (tile == null) continue;
                if (state.getUnitOnTile(nx, ny) != null) continue;

                try {
                    Unit wraith = BasicObjectBuilders.loadUnit(
                            StaticConfFiles.wraithling,
                            state.getAndIncrementUnitId(),
                            Unit.class
                    );
                    wraith.setPositionByTile(tile);

                    GameUnit newUnit = new GameUnit(wraith, source.getOwner(), 1, 1, false);
                    newUnit.setCardName("Wraithling");
                    newUnit.setHasMoved(true);
                    newUnit.setHasAttacked(true);

                    state.placeUnit(nx, ny, newUnit);

                    BasicCommands.drawUnit(out, wraith, tile);
                    try { Thread.sleep(150); } catch (InterruptedException e) { e.printStackTrace(); }
                    BasicCommands.setUnitAttack(out, wraith, 1);
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                    BasicCommands.setUnitHealth(out, wraith, 1);
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}