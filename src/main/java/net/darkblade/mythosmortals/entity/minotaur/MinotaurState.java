package net.darkblade.mythosmortals.entity.minotaur;

import net.darkblade.deluxelib.entity.ai.cortex.StateEnum;


public enum MinotaurState implements StateEnum {
    IDLE(0),                // rest / passive patrol
    SPOTTED(1),             // one-shot roar upon detecting the player
    CHASE(2),               // active pursuit (run)
    COMBAT_IDLE(3),         // tactical guard between attacks (cooldown)
    ATTACK_HORIZONTAL_1(4), // combo A (clip COMBO_A): sweep left → right
    ATTACK_HORIZONTAL_2(5), // combo B (clip COMBO_B): backhand right → left, heavier
    ATTACK_VERTICAL(6),     // downward axe slam (shield breaker)
    ATTACK_PUSH(7),         // haft push: creates space and chains into charge
    CHARGE_WINDUP(8),       // scrapes the ground, aims horns
    CHARGE_RUN(9),          // straight-line dash
    CHARGE_HIT(10),         // successful impact: launches the target
    CHARGE_STUN(11),        // wall crash: dazed and vulnerable
    CHARGE_RECOVER(12);     // whiffed dash: pulls up and plants, hitting nothing
    private final int id;

    MinotaurState(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }
}
