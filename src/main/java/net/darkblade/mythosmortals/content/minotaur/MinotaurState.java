package net.darkblade.mythosmortals.content.minotaur;

import net.darkblade.deluxelib.entity.ai.cortex.StateEnum;

/**
 * Estados de IA del minotauro. Los ids son estables (se syncan al cliente como int),
 * no reordenar sin migrar.
 */
public enum MinotaurState implements StateEnum {
    IDLE(0),                // reposo / patrulla pasiva
    SPOTTED(1),             // rugido one-shot al detectar al jugador
    CHASE(2),               // persecución activa (run)
    COMBAT_IDLE(3),         // guardia táctica entre ataques (cooldown)
    ATTACK_HORIZONTAL_1(4), // combo A (clip COMBO_A): barrido izquierda → derecha
    ATTACK_HORIZONTAL_2(5), // combo B (clip COMBO_B): revés derecha → izquierda, más pesado
    ATTACK_VERTICAL(6),     // hachazo descendente (rompe escudos)
    ATTACK_PUSH(7),         // empuje con el mango: abre hueco y encadena a la embestida
    CHARGE_WINDUP(8),       // rasca el suelo, apunta cuernos
    CHARGE_RUN(9),          // carrera en línea recta
    CHARGE_HIT(10),         // impacto exitoso: lanza al objetivo
    CHARGE_STUN(11);        // choque contra pared: mareado y vulnerable

    private final int id;

    MinotaurState(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }
}
