package net.darkblade.mythosmortals.entity.pegasus;

public enum PegasusTameState {

    WILD,

    FEEDING,

    BONDED,

    BUCKING,

    TAMED;

    private static final PegasusTameState[] VALUES = values();

    public static PegasusTameState byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WILD;
    }

    public boolean isTaming() {
        return this == FEEDING || this == BONDED || this == BUCKING;
    }

    public boolean isAirborneRitual() {
        return this == BONDED || this == BUCKING;
    }

    public boolean acceptsApples() {
        return this == WILD || this == FEEDING;
    }
}
