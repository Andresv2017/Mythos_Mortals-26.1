package net.darkblade.mythosmortals.content.pegasus;

/**
 * The pegasus taming ritual, as a state machine.
 *
 * <p>Serialized by ordinal into both NBT and synched data, so entries must only ever be appended.
 */
public enum PegasusTameState {

    /** Untamed and skittish: bolts if a player approaches without sneaking. */
    WILD,

    /** Being hand-fed apples. Bolts if the feeder runs out of them. */
    FEEDING,

    /** The ground phase just succeeded; the rider is being pulled on for the take-off. */
    BONDED,

    /** Airborne and bucking. The rider has a limited window to fit the bridle. */
    BUCKING,

    /** Fully tamed and owned. */
    TAMED;

    private static final PegasusTameState[] VALUES = values();

    public static PegasusTameState byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WILD;
    }

    /** Whether the ritual is underway — the pegasus neither behaves as wild nor obeys an owner. */
    public boolean isTaming() {
        return this == FEEDING || this == BONDED || this == BUCKING;
    }
}
