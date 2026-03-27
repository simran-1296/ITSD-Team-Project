package abilities;

/**
 * Enumerates the game events that can fire triggered effects.
 * Member A - Sprint 3.
 */
public enum TriggerType {
    ON_SUMMON,        // A unit is summoned to the board
    ON_UNIT_DIED,     // Any unit dies
    ON_TURN_START,    // The active player's turn begins
    ON_TURN_END,      // The active player's turn ends
    ON_DAMAGE_DEALT,  // A unit deals damage to another unit
    ON_DAMAGE_TAKEN   // A unit receives damage
}
