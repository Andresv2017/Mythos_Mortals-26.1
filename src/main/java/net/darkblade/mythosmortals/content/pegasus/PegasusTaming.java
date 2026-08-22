package net.darkblade.mythosmortals.content.pegasus;

import net.minecraft.core.UUIDUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * The taming ritual's bookkeeping, kept apart from the entity so the numbers that decide whether a
 * pegasus is won or lost can be read, tuned and reasoned about on their own.
 *
 * <p>Holds no world references and never touches the level: the entity feeds it events and acts on
 * what it returns.
 */
public final class PegasusTaming {

    /** Chance gained per apple. The fifth apple is around even odds; the eighth is a near certainty. */
    public static final float CHANCE_PER_APPLE = 0.15F;
    /** Grace period after the feeder runs dry, before the pegasus gives up on them. */
    public static final int ABANDON_TICKS = 100;
    /** How long the rider has to fit the bridle mid-air. */
    public static final int BUCKING_TICKS = 300;
    /** An upright player closer than this sends a wild pegasus into the sky. */
    public static final double FLEE_RADIUS = 12.0;
    /** Wandering further than this from a half-fed pegasus counts as abandoning it. */
    public static final double FEEDER_LEASH_RADIUS = 16.0;

    private PegasusTameState state = PegasusTameState.WILD;
    private int temper;
    private int escapeTimer;
    private int buckingTimer;
    private @Nullable UUID owner;

    /**
     * Who is currently hand-feeding. Deliberately not persisted: a pegasus half-fed by someone who
     * logged out — or whose chunk unloaded — reverts to wild rather than waiting forever.
     */
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

    /**
     * Accepts one apple.
     *
     * @param golden a golden apple skips the roll entirely
     * @return true when the ground phase is won
     */
    public boolean feed(RandomSource random, UUID feederId, boolean golden) {
        this.state = PegasusTameState.FEEDING;
        this.feeder = feederId;
        this.escapeTimer = 0;
        this.temper++;
        return golden || random.nextFloat() < CHANCE_PER_APPLE * this.temper;
    }

    /**
     * Runs the abandonment clock.
     *
     * @param feederStillCommitted whether the feeder is nearby and still carrying apples
     * @return true when the pegasus has waited long enough and should bolt
     */
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

    /** Ground phase won: the rider is about to be taken up. */
    public void beginBonding() {
        this.state = PegasusTameState.BONDED;
        this.escapeTimer = 0;
    }

    public void beginBucking() {
        this.state = PegasusTameState.BUCKING;
        this.buckingTimer = 0;
    }

    /** @return true once the rider has run out of time to fit the bridle */
    public boolean tickBucking() {
        return this.state == PegasusTameState.BUCKING && ++this.buckingTimer >= BUCKING_TICKS;
    }

    public int buckingTicks() {
        return this.buckingTimer;
    }

    /** The bridle went on: the ritual is complete, with no roll involved. */
    public void complete(UUID ownerId) {
        this.state = PegasusTameState.TAMED;
        this.owner = ownerId;
        this.temper = 0;
        this.escapeTimer = 0;
        this.buckingTimer = 0;
        this.feeder = null;
    }

    /**
     * Back to square one. Used when the airborne phase times out — the pegasus stays in the world
     * and can be approached again from scratch.
     */
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

    /** Convenience for callers that want the owner as an {@link Optional}. */
    public Optional<UUID> ownerOptional() {
        return Optional.ofNullable(this.owner);
    }
}
