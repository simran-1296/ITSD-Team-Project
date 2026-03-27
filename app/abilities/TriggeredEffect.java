package abilities;

import structures.GameUnit;

/**
 * Pairs a TriggerType with an Effect and the unit that owns the trigger.
 * Registered with EffectResolver to fire when the named event occurs.
 * Member A - Sprint 3.
 */
public class TriggeredEffect {

    private final TriggerType trigger;
    private final Effect effect;
    private final GameUnit owner;

    public TriggeredEffect(TriggerType trigger, Effect effect, GameUnit owner) {
        this.trigger = trigger;
        this.effect  = effect;
        this.owner   = owner;
    }

    public TriggerType getTrigger() { return trigger; }
    public Effect       getEffect()  { return effect;  }
    public GameUnit     getOwner()   { return owner;   }
}
