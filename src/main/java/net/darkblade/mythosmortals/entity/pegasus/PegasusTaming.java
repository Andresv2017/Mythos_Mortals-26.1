package net.darkblade.mythosmortals.entity.pegasus;

import net.minecraft.core.UUIDUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class PegasusTaming {

    public static final float CHANCE_PER_APPLE = 0.15F;
    public static final int ABANDON_TICKS = 100;
    public static final int BUCKING_TICKS = 300;
    public static final double FLEE_RADIUS = 12.0;
    public static final double FEEDER_LEASH_RADIUS = 16.0;

    private PegasusTameState state = PegasusTameState.WILD;
    private int temper;
    private int escapeTimer;
    private int buckingTimer;
    private @Nullable UUID owner;

    private transient @Nullable UUID feeder;

    public PegasusTameState state() {
        return this.state;
    }

    public int temper() {
        return this.temper;
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public @Nullable UUID feeder() {
        return this.feeder;
    }

    public boolean isOwnedBy(UUID candidate) {
        return this.owner != null && this.owner.equals(candidate);
    }

    public boolean feed(RandomSource random, UUID feederId, boolean golden) {
        this.state = PegasusTameState.FEEDING;
        this.feeder = feederId;
        this.escapeTimer = 0;
        this.temper++;
        return golden || random.nextFloat() < CHANCE_PER_APPLE * this.temper;
    }

    public boolean tickFeeding(boolean feederStillCommitted) {
        if (this.state != PegasusTameState.FEEDING) {
            return false;
        }
        if (feederStillCommitted) {
            this.escapeTimer = 0;
            return false;
        }
        return ++this.escapeTimer >= ABANDON_TICKS;
    }

    public void beginBonding() {
        this.state = PegasusTameState.BONDED;
        this.escapeTimer = 0;
    }

    public void beginBucking() {
        this.state = PegasusTameState.BUCKING;
        this.buckingTimer = 0;
    }

    public boolean tickBucking() {
        return this.state == PegasusTameState.BUCKING && ++this.buckingTimer >= BUCKING_TICKS;
    }

    public int buckingTicks() {
        return this.buckingTimer;
    }

    public void complete(UUID ownerId) {
        this.state = PegasusTameState.TAMED;
        this.owner = ownerId;
        this.temper = 0;
        this.escapeTimer = 0;
        this.buckingTimer = 0;
        this.feeder = null;
    }

    public void reset() {
        this.state = PegasusTameState.WILD;
        this.owner = null;
        this.temper = 0;
        this.escapeTimer = 0;
        this.buckingTimer = 0;
        this.feeder = null;
    }

    public void save(ValueOutput output) {
        output.putInt("TameState", this.state.ordinal());
        output.putInt("Temper", this.temper);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.owner);
    }

    public void load(ValueInput input) {
        this.state = PegasusTameState.byOrdinal(input.getIntOr("TameState", PegasusTameState.WILD.ordinal()));
        this.temper = input.getIntOr("Temper", 0);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.feeder = null;
        this.escapeTimer = 0;
        this.buckingTimer = 0;
        // A ritual interrupted by a save/reload is not resumable: the feeder reference is gone and
        // the rider is no longer aboard, so anything mid-ritual reverts to wild with temper kept.
        if (this.state.isTaming()) {
            this.state = PegasusTameState.WILD;
        }
    }

    public Optional<UUID> ownerOptional() {
        return Optional.ofNullable(this.owner);
    }
}
