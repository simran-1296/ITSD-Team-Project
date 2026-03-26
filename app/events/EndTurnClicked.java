package events;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import actions.PlayCardAction;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.Pos;
import structures.basic.Card;
import structures.basic.Tile;
import systems.CombatSystem;
import systems.GameEngine;

/**
 * Handles end-turn clicks.
 * Human ends turn -> switch to AI -> AI acts -> switch back to human.
 */
public class EndTurnClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.gameInitalised)
			return;
		if (gameState.isGameOver())
			return;
		if (gameState.isUnitMoving())
			return;

		gameState.switchTurn(out);

		BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
		BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());

		if (gameState.getCurrentTurn() == 2) {
			BasicCommands.addPlayer1Notification(out, "AI's Turn", 2);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			executeBasicAITurn(out, gameState);

			if (!gameState.isGameOver()) {
				gameState.switchTurn(out);

				for (GameUnit u : getUnitsForOwner(gameState, 1)) {
					u.setHasMoved(false);
					u.setHasAttacked(false);
				}

				for (GameUnit u : getUnitsForOwner(gameState, 2)) {
					u.setHasMoved(false);
					u.setHasAttacked(false);
				}

				BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
				BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());
				BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return;
		}

		BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void executeBasicAITurn(ActorRef out, GameState state) {
		if (state.isGameOver())
			return;

		playOneAICard(out, state);

		if (state.isGameOver())
			return;

		actWithAIUnits(out, state);
	}

	/**
	 * AI priority:
	 * 1) Useful spells
	 * 2) Creature summon
	 */
	private void playOneAICard(ActorRef out, GameState state) {
		List<Card> hand = state.getPlayer2Hand();
		int mana = state.getPlayer2().getMana();

		// 1. Try spell cards first
		for (int i = 0; i < hand.size(); i++) {
			Card card = hand.get(i);
			if (card == null)
				continue;
			if (card.getManacost() > mana)
				continue;

			String name = card.getCardname();

			// True Strike / Truestrike
			if ("True Strike".equals(name) || "Truestrike".equals(name)) {
				GameUnit target = findLowestHealthEnemyNonAvatar(state, 2);
				if (target != null) {
					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(target.getTileX(), target.getTileY())));
					syncManaUI(out, state);

					GameUnit after = state.getUnitOnTile(target.getTileX(), target.getTileY());
					if (after == null) {
						BasicCommands.deleteUnit(out, target.getUnit());
					} else {
						BasicCommands.setUnitHealth(out, after.getUnit(), after.getHealth());
					}
					return;
				}
			}

			// Sundrop Elixir
			if ("Sundrop Elixir".equals(name)) {
				GameUnit target = findLowestHealthAllyNonAvatar(state, 2);
				if (target != null && target.getHealth() < target.getMaxHealth()) {
					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(target.getTileX(), target.getTileY())));
					syncManaUI(out, state);
					BasicCommands.setUnitHealth(out, target.getUnit(), target.getHealth());
					return;
				}
			}

			// Beamshock
			if ("Beamshock".equals(name) || "Beam Shock".equals(name)) {
				GameUnit target = findClosestEnemyNonAvatar(state, 2);
				if (target != null) {
					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(target.getTileX(), target.getTileY())));
					syncManaUI(out, state);
					return;
				}
			}

			// Dark Terminus
			if ("Dark Terminus".equals(name)) {
				GameUnit target = findHighestAttackEnemyNonAvatar(state, 2);
				if (target != null) {
					int tx = target.getTileX();
					int ty = target.getTileY();

					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(tx, ty)));
					syncManaUI(out, state);

					BasicCommands.deleteUnit(out, target.getUnit());

					GameUnit after = state.getUnitOnTile(tx, ty);
					if (after != null) {
						Tile tile = state.getTile(tx, ty);
						drawUnitWithStats(out, tile, after);
					}
					return;
				}
			}

			// Horn of the Forsaken
			if ("Horn of the Forsaken".equals(name)) {
				GameUnit avatar = state.getPlayer2Avatar();
				if (avatar != null) {
					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(avatar.getTileX(), avatar.getTileY())));
					syncManaUI(out, state);
					return;
				}
			}

			// Wraithling Swarm
			if ("Wraithling Swarm".equals(name)) {
				GameUnit avatar = state.getPlayer2Avatar();
				if (avatar != null) {
					GameEngine.apply(state, new PlayCardAction(2, i, new Pos(avatar.getTileX(), avatar.getTileY())));
					syncManaUI(out, state);
					drawNewAIAdjacentWraithlings(out, state, avatar);
					return;
				}
			}
		}

		// 2. Try creature cards
		for (int i = 0; i < hand.size(); i++) {
			Card card = hand.get(i);
			if (card == null)
				continue;
			if (card.getManacost() > mana)
				continue;
			if (!card.getIsCreature())
				continue;

			Tile summonTile = findFirstValidSummonTile(state, 2, card);
			if (summonTile == null)
				continue;

			GameEngine.apply(state, new PlayCardAction(2, i, new Pos(summonTile.getTilex(), summonTile.getTiley())));

			GameUnit newUnit = state.getUnitOnTile(summonTile.getTilex(), summonTile.getTiley());
			if (newUnit != null) {
				drawUnitWithStats(out, summonTile, newUnit);
			}

			syncManaUI(out, state);
			return;
		}
	}

	private void actWithAIUnits(ActorRef out, GameState state) {
		List<GameUnit> aiUnits = getUnitsForOwner(state, 2);

		for (GameUnit unit : aiUnits) {
			if (unit == null)
				continue;
			if (unit.isDead())
				continue;
			if (unit.isAvatar())
				continue;
			if (state.isGameOver())
				return;

			// Attack first if already adjacent
			if (!unit.hasAttacked()) {
				GameUnit adjacentEnemy = findAdjacentEnemy(state, unit);
				if (adjacentEnemy != null) {
					CombatSystem.executeAttack(out, state, unit, adjacentEnemy);
					unit.setHasAttacked(true);
					unit.setHasMoved(true);
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
			}

			// Move toward enemy avatar
			if (!unit.hasMoved()) {
				Tile moveTile = findBestMoveTowardAvatar(state, unit, state.getPlayer1Avatar());
				if (moveTile != null) {
					BasicCommands.moveUnitToTile(out, unit.getUnit(), moveTile);
					state.moveUnit(unit, moveTile.getTilex(), moveTile.getTiley());
					unit.setHasMoved(true);
					try {
						Thread.sleep(700);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// After moving, attack again if now adjacent
			if (!unit.hasAttacked()) {
				GameUnit adjacentEnemy = findAdjacentEnemy(state, unit);
				if (adjacentEnemy != null) {
					CombatSystem.executeAttack(out, state, unit, adjacentEnemy);
					unit.setHasAttacked(true);
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private Tile findFirstValidSummonTile(GameState state, int owner, Card card) {
		String name = card.getCardname();

		// Flying / anywhere summon cards
		if ("Young Flamewing".equals(name) || "Ironcliff Guardian".equals(name)) {
			for (int x = 1; x <= 9; x++) {
				for (int y = 1; y <= 5; y++) {
					Tile tile = state.getTile(x, y);
					if (tile == null)
						continue;
					if (state.getUnitOnTile(x, y) != null)
						continue;
					return tile;
				}
			}
			return null;
		}

		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit == null || unit.getOwner() != owner)
					continue;

				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						if (dx == 0 && dy == 0)
							continue;

						int nx = x + dx;
						int ny = y + dy;

						Tile tile = state.getTile(nx, ny);
						if (tile == null)
							continue;
						if (state.getUnitOnTile(nx, ny) != null)
							continue;
						return tile;
					}
				}
			}
		}
		return null;
	}

	private List<GameUnit> getUnitsForOwner(GameState state, int owner) {
		List<GameUnit> units = new ArrayList<>();
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit != null && unit.getOwner() == owner) {
					units.add(unit);
				}
			}
		}
		return units;
	}

	private GameUnit findAdjacentEnemy(GameState state, GameUnit unit) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0)
					continue;

				int nx = unit.getTileX() + dx;
				int ny = unit.getTileY() + dy;

				GameUnit target = state.getUnitOnTile(nx, ny);
				if (target != null && target.getOwner() != unit.getOwner()) {
					return target;
				}
			}
		}
		return null;
	}

	private Tile findBestMoveTowardAvatar(GameState state, GameUnit unit, GameUnit targetAvatar) {
		if (unit == null || targetAvatar == null)
			return null;

		Tile bestTile = null;
		int bestDistance = Integer.MAX_VALUE;

		int[][] dirs = {
				{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
				{ 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
		};

		for (int[] d : dirs) {
			int nx = unit.getTileX() + d[0];
			int ny = unit.getTileY() + d[1];

			Tile tile = state.getTile(nx, ny);
			if (tile == null)
				continue;
			if (state.getUnitOnTile(nx, ny) != null)
				continue;

			int dist = Math.abs(nx - targetAvatar.getTileX()) + Math.abs(ny - targetAvatar.getTileY());
			if (dist < bestDistance) {
				bestDistance = dist;
				bestTile = tile;
			}
		}

		return bestTile;
	}

	private GameUnit findLowestHealthEnemyNonAvatar(GameState state, int aiOwner) {
		GameUnit best = null;
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit == null)
					continue;
				if (unit.getOwner() == aiOwner)
					continue;
				if (unit.isAvatar())
					continue;

				if (best == null || unit.getHealth() < best.getHealth()) {
					best = unit;
				}
			}
		}
		return best;
	}

	private GameUnit findLowestHealthAllyNonAvatar(GameState state, int aiOwner) {
		GameUnit best = null;
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit == null)
					continue;
				if (unit.getOwner() != aiOwner)
					continue;
				if (unit.isAvatar())
					continue;

				if (best == null || unit.getHealth() < best.getHealth()) {
					best = unit;
				}
			}
		}
		return best;
	}

	private GameUnit findClosestEnemyNonAvatar(GameState state, int aiOwner) {
		GameUnit avatar = state.getPlayer2Avatar();
		if (avatar == null)
			return null;

		GameUnit best = null;
		int bestDist = Integer.MAX_VALUE;

		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit == null)
					continue;
				if (unit.getOwner() == aiOwner)
					continue;
				if (unit.isAvatar())
					continue;

				int dist = Math.abs(unit.getTileX() - avatar.getTileX())
						+ Math.abs(unit.getTileY() - avatar.getTileY());

				if (dist < bestDist) {
					bestDist = dist;
					best = unit;
				}
			}
		}
		return best;
	}

	private GameUnit findHighestAttackEnemyNonAvatar(GameState state, int aiOwner) {
		GameUnit best = null;
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				GameUnit unit = state.getUnitOnTile(x, y);
				if (unit == null)
					continue;
				if (unit.getOwner() == aiOwner)
					continue;
				if (unit.isAvatar())
					continue;

				if (best == null || unit.getAttack() > best.getAttack()) {
					best = unit;
				}
			}
		}
		return best;
	}

	private void drawNewAIAdjacentWraithlings(ActorRef out, GameState state, GameUnit avatar) {
		if (avatar == null)
			return;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0)
					continue;

				int nx = avatar.getTileX() + dx;
				int ny = avatar.getTileY() + dy;

				Tile tile = state.getTile(nx, ny);
				GameUnit unit = state.getUnitOnTile(nx, ny);

				if (tile != null && unit != null && "Wraithling".equals(unit.getCardName())) {
					drawUnitWithStats(out, tile, unit);
				}
			}
		}
	}

	private void drawUnitWithStats(ActorRef out, Tile tile, GameUnit unit) {
		if (unit == null || tile == null)
			return;

		BasicCommands.deleteUnit(out, unit.getUnit());
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		BasicCommands.drawUnit(out, unit.getUnit(), tile);
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (!unit.isAvatar()) {
			BasicCommands.setUnitAttack(out, unit.getUnit(), unit.getAttack());
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		BasicCommands.setUnitHealth(out, unit.getUnit(), Math.max(0, unit.getHealth()));
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void syncManaUI(ActorRef out, GameState state) {
		BasicCommands.setPlayer1Mana(out, state.getPlayer1());
		BasicCommands.setPlayer2Mana(out, state.getPlayer2());
	}
}