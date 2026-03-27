package abilities;

/**
 * ASSUMPTIONS (Sprint2 patch):
 * - Status exists and is checked by Rules to prevent acting.
 * - We only need SUMMONING_SICKNESS for Sprint 2; Rush later can bypass.
 */
public enum Status {
    SUMMONING_SICKNESS,
    STUNNED
}
