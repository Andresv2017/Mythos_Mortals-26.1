package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.deluxelib.anim.AnimSource;
import net.darkblade.deluxelib.anim.Animatable;
import net.darkblade.deluxelib.anim.Loop;
import net.darkblade.deluxelib.anim.MobAnimator;
import net.darkblade.deluxelib.anim.StandardAnimation;
import net.darkblade.deluxelib.camera.ScreenShake;
import net.darkblade.deluxelib.entity.AbstractFlyingEntity;
import net.darkblade.deluxelib.vfx.ParticleFx;
import net.darkblade.mythosmortals.content.pegasus.menu.PegasusInventoryMenu;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The pegasus: a skittish, non-combatant flying mount won through a two-stage taming ritual.
 *
 * <p><b>Ground phase.</b> Wild, it bolts into the sky and out of the world if an upright player gets
 * within {@link PegasusTaming#FLEE_RADIUS} blocks. Approached crouching, it accepts apples, each one
 * improving the odds of bonding. Run out of apples mid-ritual and it gives up on you and leaves.
 *
 * <p><b>Air phase.</b> The moment it bonds it takes the feeder up itself and bucks. Fitting
 * {@code athena_bridle} during that window tames it outright; letting the window close throws the
 * rider off and returns the pegasus to wild — still in the area, ready to be tried again.
 *
 * <p>Tamed, it wears the bridle (flight), a {@code pegasus_saddle} (mounting) and horse armour, and
 * flies freely in three dimensions with a wind dash. See {@link PegasusFlightController}.
 */
public class PegasusEntity extends AbstractFlyingEntity implements Animatable<PegasusEntity> {

    private static final EntityDataAccessor<Integer> DATA_TAME_STATE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BRIDLE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DASH_COOLDOWN =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    /**
     * Horizontal blocks travelled per tick, measured and synced by us.
     *
     * <p>{@code isGroundMoving()} and {@code isFlyingMoving()} in the base class derive their answer
     * from {@code getDeltaMovement()} on the server — but a mount driven by its rider is moved by
     * position packets from that client, so the server's delta stays near zero and every gait
     * animation would sit in idle forever. Measuring how far the entity actually got is true
     * regardless of which side did the moving.
     */
    private static final EntityDataAccessor<Float> DATA_TRAVEL_SPEED =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.FLOAT);

    /** Horizontal distance covered before a fleeing pegasus is considered gone for good. */
    private static final double ESCAPE_DISTANCE = 96.0;
    /** Forward speed of the escape. It leaves toward the horizon, not straight up. */
    private static final double ESCAPE_FORWARD_SPEED = 0.90;
    /** Climb blended into the escape, so it recedes into the sky rather than along the ground. */
    private static final double ESCAPE_CLIMB_SPEED = 0.30;
    /** Safety net: never let an escape run forever if something blocks the path. */
    private static final int ESCAPE_MAX_TICKS = 400;
    /** Ticks per bucking stroke. Long enough that each one crosses real ground. */
    private static final int BUCK_STROKE_MIN = 25;
    private static final int BUCK_STROKE_MAX = 45;
    /** Thrust along the current stroke, and the speed it saturates at. */
    private static final double BUCK_SURGE = 0.28;
    private static final double BUCK_MAX_SPEED = 1.4;
    /** Altitude band the buck stays inside, so it neither ploughs the ground nor leaves the world. */
    private static final double BUCK_MIN_ALTITUDE = 12.0;
    private static final double BUCK_MAX_ALTITUDE = 45.0;
    /**
     * Hard ceiling on the whole air phase. Whatever else happens — a stalled climb, a phase that
     * fails to advance — the rider is bridled or on the floor within this many ticks of leaving
     * the ground. Nothing about the ritual is allowed to strand someone in the sky.
     */
    private static final int AIR_PHASE_MAX_TICKS = 500;
    /** How long the newly bridled pegasus hangs in the air catching its breath. */
    private static final int SETTLE_HOVER_TICKS = 100;
    /** Powered descent rate for the ride down. Five times the landing flare's own creep. */
    private static final double SETTLE_DESCENT_SPEED = 0.35;
    /** Safety net on the way down, in case the descent cannot find ground. */
    private static final int SETTLE_DESCENT_MAX_TICKS = 400;
    /** Minimum height the ritual climbs to, so the throw-off is a real fall. */
    private static final double BUCK_ALTITUDE = 14.0;
    private static final double BONDED_CLIMB_SPEED = 0.35;
    /** Length of the authored take_off animation, in ticks. */
    private static final int TAKEOFF_TICKS = 16;

    /**
     * Ceiling on the visual climb/dive tilt while someone is aboard.
     *
     * <p>The library targets up to 40°, which reads well on a bird crossing the sky and is awful
     * from the saddle: rearing that far fills the screen with pegasus and hides where you are going.
     */
    private static final float RIDDEN_MAX_PITCH = 18.0F;

    /**
     * Velocity {@link #steerTowards} aims for on a wander leg.
     *
     * <p>Not the speed it actually flies at: {@code travelInAir} multiplies the velocity by 0.91
     * every tick, so the steering and the drag settle at an equilibrium well below this. At 1.0 the
     * pegasus cruises at about 0.4 blocks a tick — eight a second. The previous 0.45 settled at
     * 0.13, slower than a walking player.
     */
    private static final double GLIDE_SPEED = 1.0;
    /** How hard it corrects toward the leg. Low is heavy and gliding; 0.3 and up darts about. */
    private static final double GLIDE_ACCEL = 0.07;
    /** Close enough to call the leg finished. */
    private static final double GLIDE_ARRIVE_RADIUS = 3.0;

    /** Gait thresholds, in horizontal blocks per tick. Read by {@link PegasusFlightDebug}. */
    static final float WALK_SPEED = 0.015F;
    static final float FLY_MOVE_SPEED = 0.08F;

    private final MobAnimator<PegasusEntity> animator = new MobAnimator<>(this);
    private final PegasusFlightDebug flightDebug = new PegasusFlightDebug(this);
    private final PegasusTaming taming = new PegasusTaming();
    private final PegasusEquipment equipment = new PegasusEquipment(this);

    /** Set while climbing away from the player for good; ends in {@code discard()}. */
    private boolean escaping;
    private Vec3 escapeOrigin = Vec3.ZERO;
    private Vec3 escapeHeading = Vec3.ZERO;
    private int escapeTicks;

    private int buckRetargetTimer;
    private Vec3 buckHeading = Vec3.ZERO;
    private double buckVertical;
    /** Absolute tick by which the air phase must be over, one way or the other. */
    private int airPhaseDeadline;

    /**
     * What the pegasus does with itself in the moments after the bridle goes on: it hovers, then
     * carries the rider down and lets them off. Purely a server-side script, and one the rider can
     * cut short at any point by fitting the saddle — see {@link #tickSettling()}.
     */
    private enum Settling { NONE, HOVER, DESCEND }

    private Settling settling = Settling.NONE;
    private int settlingTicks;

    private double lastTickX;
    private double lastTickZ;
    private boolean hasLastTickPosition;

    /** Where {@link GlideWanderGoal} is currently flying to. Read by {@link PegasusFlightDebug}. */
    @Nullable Vec3 glideTarget;
    /** Counts out the takeoff phase when the goal that normally would is standing down. */
    private int phaseTicks;

    public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.70)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // -----------------------------------------------------------------------
    // Synched data & persistence
    // -----------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAME_STATE, PegasusTameState.WILD.ordinal());
        builder.define(DATA_HAS_BRIDLE, false);
        builder.define(DATA_DASH_COOLDOWN, 0);
        builder.define(DATA_TRAVEL_SPEED, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.taming.save(output);
        output.store("Bridle", ItemStack.OPTIONAL_CODEC, this.equipment.getBridle());
    }

    @Override
    public void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        this.taming.load(input);
        this.equipment.setItem(PegasusEquipment.BRIDLE_SLOT,
                input.read("Bridle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.syncState();
    }

    /** Mirrors server-only state onto the accessors the client reads for rendering and control. */
    private void syncState() {
        this.entityData.set(DATA_TAME_STATE, this.taming.state().ordinal());
        this.entityData.set(DATA_HAS_BRIDLE, this.equipment.hasBridle());
    }

    /** Called by {@link PegasusEquipment} whenever the bridle slot changes. */
    void onEquipmentChanged() {
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_HAS_BRIDLE, this.equipment.hasBridle());
        }
    }

    public PegasusTameState tameState() {
        return PegasusTameState.byOrdinal(this.entityData.get(DATA_TAME_STATE));
    }

    public boolean isTamed() {
        return this.tameState() == PegasusTameState.TAMED;
    }

    public boolean hasBridle() {
        return this.entityData.get(DATA_HAS_BRIDLE);
    }

    public boolean isSaddled() {
        return !this.getItemBySlot(EquipmentSlot.SADDLE).isEmpty();
    }

    public int armorTier() {
        return PegasusEquipment.armorTier(this.getItemBySlot(EquipmentSlot.BODY));
    }

    public int dashCooldown() {
        return this.entityData.get(DATA_DASH_COOLDOWN);
    }

    public PegasusEquipment equipment() {
        return this.equipment;
    }

    /** Server-side authority for permissions. Never consult it from rendering or control code. */
    public boolean isOwnedBy(Player player) {
        return this.taming.isOwnedBy(player.getUUID());
    }

    // -----------------------------------------------------------------------
    // Flight tuning
    // -----------------------------------------------------------------------

    @Override
    protected double getMinFlightAltitude() { return 5.0; }

    @Override
    protected double getMaxFlightAltitude() { return 12.0; }

    /**
     * Legs the flight budget can actually finish. The move control cruises at roughly 0.14 blocks a
     * tick, so at the previous 45 a target sat 240 ticks away against a 193-tick stint: the pegasus
     * never once arrived, and instead curved through one endless arc until the timer sent it down.
     * Halved and then some, a leg lands at 50-100 ticks and the fly / hover / fly rhythm can breathe.
     */
    @Override
    protected double getWanderHorizontalRadius() { return 30.0; }

    /**
     * Body yaw is capped well below the library's 8°/tick default. At this size a quick turn reads
     * as the model snapping around its own axis; a shallow rate makes it carve the turn instead,
     * and the banking roll has time to sell it.
     */
    @Override
    protected float getFlightYawTurnSpeed() { return 5.0F; }

    /** Same idea for a ridden turn, but loose enough that steering still feels answerable. */
    private static final float RIDDEN_YAW_TURN_SPEED = 7.0F;

    /** Twenty to thirty seconds on the wing — long enough to be worth watching. */
    @Override
    protected int computeMaxFlightTicks() { return 400 + this.random.nextInt(200); }

    /**
     * A beat between legs, not a stop. The library's 30-80 ticks ate half of a short flight sitting
     * perfectly still in mid-air.
     */
    @Override
    protected int computeFlightHoverTicks() { return 20 + this.random.nextInt(20); }

    /** And it stays down a long while between them. */
    @Override
    protected int computeGroundRestTicks() { return 600 + this.random.nextInt(600); }

    /**
     * A tamed pegasus never takes off on its own — left standing, it stays where its owner parked
     * it. Only the rider's own take-off command, or the taming ritual, puts it in the air.
     */
    @Override
    protected boolean shouldStayGrounded() { return this.isTamed(); }

    /**
     * Sustained climb rate through the take-off. The height comes from holding this for the length
     * of the animation, never from a kick — see {@link #beginTakeoff()}.
     */
    @Override
    protected double getTakeoffLiftSpeed() { return 0.15; }

    /**
     * Leaves the ground at a wingbeat rather than a launch.
     *
     * <p>The library opens a take-off by setting the vertical velocity to three times the lift rate
     * — 0.6 blocks a tick even at a modest setting, which on something horse-sized reads as being
     * fired out of the ground. Nothing else about the take-off needs that: the goal sustains the
     * lift for as long as the animation runs, and the climb reads better spread across those ticks.
     */
    @Override
    protected void beginTakeoff() {
        super.beginTakeoff();
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, this.getTakeoffLiftSpeed(), motion.z);
    }

    /**
     * The flare and the approach are tuned against each other: the authored landing animation runs
     * 1.25 s (25 ticks), and 2.2 blocks at 0.10 a tick is 22 — so the gesture finishes as the hooves
     * touch. The library's 4-block, 0.07-a-tick default takes 57 ticks, which left the animation
     * over and done with while the pegasus was still floating down.
     */
    @Override
    protected double getLandingDescentSpeed() { return 0.10; }

    @Override
    protected double getLandingApproachAltitude() { return 2.2; }

    @Override
    protected boolean applyTiltDuringTakeoff() { return false; }

    // -----------------------------------------------------------------------
    // Goals
    // -----------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BoltFromPlayerGoal());

        // Every autonomous goal stands down while someone is aboard: during the ritual and under a
        // rider the pegasus is driven directly, and a wander target would fight that.
        this.goalSelector.addGoal(2, new TakeoffGoal() {
            @Override
            public boolean canUse() {
                return !isBusyWithRider() && super.canUse();
            }

            /**
             * The base completes the takeoff on the tick after it starts, which cuts the climb off
             * before it has left the ground and leaves the authored animation playing over level
             * flight. Hold it until the clip is done, the way the arpy does.
             */
            @Override
            protected boolean shouldCompleteTakeoff() {
                return !animator.isPlaying("take_off");
            }
        });
        this.goalSelector.addGoal(3, new GlideWanderGoal());
        this.goalSelector.addGoal(4, new LandingGoal() {
            @Override
            public boolean canUse() {
                return !isBusyWithRider() && super.canUse();
            }
        });

        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override
            public boolean canUse() {
                return !isBusyWithRider() && super.canUse();
            }
        };
        stroll.setInterval(30);
        this.goalSelector.addGoal(5, stroll);
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /**
     * Flies a wander leg by steering the velocity directly rather than by pathfinding to it.
     *
     * <p>The library's own wander goal hands the leg to {@code FlyingPathNavigation}, and the move
     * control then aims the body at the current path <em>node</em>. A creature this size overshoots
     * those nodes and ends up orbiting them: the flight log showed the yaw swinging through 175°
     * one way and 165° back while the distance to the actual destination barely moved. That is the
     * "stair-step bouncing" {@link #steerTowards} exists to avoid — it points the body along the
     * velocity it really has, so the heading converges by construction and the turns come out as
     * arcs.
     *
     * <p>The descent at the end of a stint is left to the base goal, which already handles the
     * powered glide down and the handover to the landing.
     */
    private class GlideWanderGoal extends FlightWanderGoal {

        private int hoverTicks;

        @Override
        public boolean canUse() {
            return !isBusyWithRider() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !isBusyWithRider() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            glideTarget = null;
            this.hoverTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (isTakingOff()) {
                return;
            }
            if (seekingGround) {
                super.tick();
                return;
            }
            if (flightDurationTimer >= getMaxFlightTicks()) {
                seekingGround = true;
                glideTarget = null;
                getNavigation().stop();
                return;
            }

            if (this.hoverTicks > 0) {
                this.hoverTicks--;
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x * 0.7, motion.y * 0.7, motion.z * 0.7);
                return;
            }

            if (glideTarget == null) {
                glideTarget = findFlightWanderTarget();
                if (glideTarget == null) {
                    this.hoverTicks = computeFlightHoverTicks();
                }
                return;
            }

            if (position().distanceTo(glideTarget) < GLIDE_ARRIVE_RADIUS) {
                glideTarget = null;
                this.hoverTicks = computeFlightHoverTicks();
                return;
            }

            steerTowards(glideTarget, GLIDE_SPEED, GLIDE_ACCEL);
        }
    }

    /** True whenever something other than the goal system owns the movement. */
    private boolean isBusyWithRider() {
        return this.isVehicle() || this.escaping || this.taming.state().isTaming();
    }

    /**
     * A wild pegasus hears you coming. Crouching keeps you under its notice; walking up to it does
     * not, and it leaves for good.
     */
    private class BoltFromPlayerGoal extends Goal {

        private @Nullable Player intruder;

        BoltFromPlayerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (escaping || tameState() != PegasusTameState.WILD) {
                return false;
            }
            this.intruder = level().getNearestPlayer(PegasusEntity.this, PegasusTaming.FLEE_RADIUS);
            // Creative players are ruled out alongside spectators: they are building or testing, not
            // hunting, and a mob that vanishes the moment you summon it cannot be worked on.
            return this.intruder != null
                    && !this.intruder.isCrouching()
                    && !this.intruder.isSpectator()
                    && !this.intruder.getAbilities().instabuild;
        }

        @Override
        public void start() {
            beginEscape(this.intruder);
        }
    }

    // -----------------------------------------------------------------------
    // Ritual & escape ticking
    // -----------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();

        // Both sides: the client recomputes the flight tilt locally, so clamping only on the server
        // would leave the rider looking at the steep version anyway.
        if (this.isVehicle()) {
            this.flightPitch = Mth.clamp(this.flightPitch, -RIDDEN_MAX_PITCH, RIDDEN_MAX_PITCH);
            this.prevFlightPitch = Mth.clamp(this.prevFlightPitch, -RIDDEN_MAX_PITCH, RIDDEN_MAX_PITCH);
        }

        if (this.level().isClientSide()) {
            return;
        }

        this.updateTravelSpeed();
        this.flightDebug.tick();

        // Nobody is asking for speed unless a rider holds sprint, it is bolting, or it is bucking.
        if (!this.isVehicle() && !this.escaping && this.isSprinting()) {
            this.setSprinting(false);
        }

        int cooldown = this.entityData.get(DATA_DASH_COOLDOWN);
        if (cooldown > 0) {
            this.entityData.set(DATA_DASH_COOLDOWN, cooldown - 1);
        }

        // TakeoffGoal and LandingGoal are what normally end those two phases, and both stand down
        // while a rider is aboard or the pegasus is bolting. Without this the entity would hang in
        // "taking off" or "landing" forever the first time either happens under a rider.
        if (this.isBusyWithRider()) {
            if (this.isTakingOff()) {
                this.sustainTakeoff();
            } else if (this.isLanding()) {
                this.sustainLanding();
            }
        }

        // Flying into the ground counts as landing. Without this the rider could only end a flight
        // by holding shift on the way down, and otherwise carried on "flying" while scraping the
        // floor — grounded animations and walking never came back until they remounted.
        if (this.isFlying() && !this.isTakingOff() && !this.isLanding() && this.isVehicle()
                && !this.taming.state().isAirborneRitual()
                && (this.onGround() || this.verticalCollision)) {
            this.beginLanding();
        }

        if (this.escaping) {
            this.tickEscape();
            return;
        }

        if (this.taming.state().isAirborneRitual() && this.tickCount >= this.airPhaseDeadline) {
            this.throwRider();
            return;
        }

        switch (this.taming.state()) {
            case FEEDING -> this.tickFeeding();
            case BONDED -> this.tickBonded();
            case BUCKING -> this.tickBucking();
            default -> { }
        }

        if (this.settling != Settling.NONE) {
            this.tickSettling();
        }
    }

    /**
     * Records how far the pegasus actually moved last tick. {@code super.aiStep()} has already run
     * the travel step by the time this is called, so the difference covers a whole tick of motion.
     */
    private void updateTravelSpeed() {
        double dx = this.getX() - this.lastTickX;
        double dz = this.getZ() - this.lastTickZ;
        this.lastTickX = this.getX();
        this.lastTickZ = this.getZ();
        if (!this.hasLastTickPosition) {
            this.hasLastTickPosition = true;
            return;
        }

        float instant = (float) Math.sqrt(dx * dx + dz * dz);
        float previous = this.entityData.get(DATA_TRAVEL_SPEED);
        // Quick to pick up, slower to fall away, so a gait never flickers on a single stuttered tick.
        float smoothed = previous + (instant - previous) * (instant > previous ? 0.5F : 0.2F);
        if (Math.abs(smoothed - previous) > 1.0E-4F) {
            this.entityData.set(DATA_TRAVEL_SPEED, smoothed);
        }
    }

    /** Horizontal speed in blocks per tick, agreed on by both sides. */
    public float travelSpeed() {
        return this.entityData.get(DATA_TRAVEL_SPEED);
    }

    // Narrow views onto protected base state, for PegasusFlightDebug only.
    int flightDurationForDebug() { return this.flightDurationTimer; }

    int groundRestForDebug() { return this.groundRestTimer; }

    double groundHeightForDebug() { return this.groundHeight(); }

    /**
     * Closes the take-off once the authored animation has had its 0.75 s, and supplies the lift
     * itself only when nobody is flying the pegasus — during the ritual, or while it bolts. Under a
     * rider the climb is theirs, so this just times the animation out.
     */
    private void sustainTakeoff() {
        if (!(this.getControllingPassenger() instanceof Player)) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < this.getTakeoffLiftSpeed()) {
                this.setDeltaMovement(motion.x * 0.6, this.getTakeoffLiftSpeed(), motion.z * 0.6);
                this.hurtMarked = true;
            }
        }
        if (++this.phaseTicks >= TAKEOFF_TICKS) {
            this.phaseTicks = 0;
            this.completeTakeoff();
        }
    }

    /** Descend, then close the landing on ground contact. */
    private void sustainLanding() {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.9, -this.getLandingDescentSpeed(), motion.z * 0.9);
        this.hurtMarked = true;
        if (this.onGround() || this.verticalCollision || this.isInWater()) {
            this.completeLanding();
        }
    }

    /**
     * A climbing departure toward the horizon: it holds the heading it picked when startled and
     * gains height as it goes, so it shrinks into the distance instead of rocketing out of sight.
     * Flying entities take no fall damage, so nothing catches it on the way.
     */
    private void tickEscape() {
        this.setDeltaMovement(
                this.escapeHeading.x * ESCAPE_FORWARD_SPEED,
                ESCAPE_CLIMB_SPEED,
                this.escapeHeading.z * ESCAPE_FORWARD_SPEED);
        this.hurtMarked = true;

        // Face where it is going; the renderer's banking tilt follows from the movement itself.
        this.setYRot((float) (Mth.atan2(this.escapeHeading.z, this.escapeHeading.x) * (180.0 / Math.PI)) - 90.0F);
        this.yBodyRot = this.yHeadRot = this.getYRot();

        double travelled = this.position().subtract(this.escapeOrigin).horizontalDistance();
        if (travelled >= ESCAPE_DISTANCE || ++this.escapeTicks >= ESCAPE_MAX_TICKS) {
            this.discard();
        }
    }

    private void beginEscape(@Nullable Player intruder) {
        this.escaping = true;
        this.escapeOrigin = this.position();
        this.escapeHeading = pickEscapeHeading(intruder);
        this.escapeTicks = 0;
        this.phaseTicks = 0;
        this.getNavigation().stop();
        if (!this.isFlying()) {
            this.beginTakeoff();
        }
        this.setSprinting(true);
        this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.2F);
        this.animator.play(this.animator.getByName("tame_fail"));
    }

    /** Directly away from whoever startled it; a random bearing when nothing in particular did. */
    private Vec3 pickEscapeHeading(@Nullable Player intruder) {
        Vec3 away = intruder == null ? Vec3.ZERO : this.position().subtract(intruder.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        if (flat.lengthSqr() < 1.0E-4) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            flat = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        return flat.normalize();
    }

    private void tickFeeding() {
        boolean committed = false;
        UUID feederId = this.taming.feeder();
        if (feederId != null) {
            Player feeder = this.level().getPlayerByUUID(feederId);
            committed = feeder != null
                    && feeder.isAlive()
                    && this.distanceTo(feeder) <= PegasusTaming.FEEDER_LEASH_RADIUS
                    && hasApple(feeder);
        }
        if (this.taming.tickFeeding(committed)) {
            this.beginEscape(feederId == null ? null : this.level().getPlayerByUUID(feederId));
        }
    }

    /**
     * Bridging phase: the take-off plays out, then the pegasus climbs until being thrown off would
     * actually be a fall worth fearing. Only then does the bucking proper start.
     */
    private void tickBonded() {
        if (!(this.getFirstPassenger() instanceof Player)) {
            this.throwRider();
            return;
        }
        if (this.isTakingOff() || !this.isFlying()) {
            return;
        }
        // Climb to a height worth fearing — but hand over to the buck on schedule regardless, so a
        // ceiling or an awkward heightmap can never swallow the fifteen seconds that matter.
        boolean climbTimeLeft = this.tickCount < this.airPhaseDeadline - PegasusTaming.BUCKING_TICKS;
        if (this.getY() - this.groundHeight() < BUCK_ALTITUDE && climbTimeLeft) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.9, BONDED_CLIMB_SPEED, motion.z * 0.9);
            this.hurtMarked = true;
            return;
        }
        this.taming.beginBucking();
        this.buckRetargetTimer = 0;
        this.buckHeading = Vec3.ZERO;
        this.syncState();
    }

    private double groundHeight() {
        return this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition()).getY();
    }

    private void tickBucking() {
        // Belt and braces for the dismount lock: however the rider came off — died, logged out, was
        // teleported away — the ritual ends rather than carrying on riderless.
        if (!(this.getFirstPassenger() instanceof Player)) {
            this.throwRider();
            return;
        }
        this.driveBucking();

        if (this.tickCount % 10 == 0 && this.getFirstPassenger() instanceof ServerPlayer rider) {
            ScreenShake.forPlayer(rider)
                    .duration(10).fadeOut(6).frequency(16.0F).amplitude(0.30F)
                    .seed(this.getId() + this.tickCount)
                    .fire();
        }

        if (this.taming.tickBucking()) {
            this.throwRider();
        }
    }

    /**
     * The buck: long, committed surges that swerve hard at the end of each one, with the nose
     * pitching up and plunging between them. Chasing a nearby point instead produced a twitchy
     * hover — the pegasus reached the target in a few ticks and then jittered around it.
     */
    private void driveBucking() {
        this.setSprinting(true);
        if (--this.buckRetargetTimer <= 0) {
            this.buckRetargetTimer = BUCK_STROKE_MIN + this.random.nextInt(BUCK_STROKE_MAX - BUCK_STROKE_MIN);
            this.buckHeading = swerve(this.buckHeading);
            // Alternating rear-up and plunge is what sells it as an animal trying to shed you.
            this.buckVertical = (this.random.nextBoolean() ? 1.0 : -1.0) * (0.55 + this.random.nextDouble() * 0.35);
            this.playSound(SoundEvents.HORSE_ANGRY, 0.9F, 1.0F + this.random.nextFloat() * 0.3F);
        }

        double altitude = this.getY() - this.groundHeight();
        double vertical = this.buckVertical;
        if (altitude < BUCK_MIN_ALTITUDE) {
            vertical = Math.abs(vertical);
        } else if (altitude > BUCK_MAX_ALTITUDE) {
            vertical = -Math.abs(vertical);
        }

        Vec3 motion = this.getDeltaMovement().scale(0.88)
                .add(this.buckHeading.scale(BUCK_SURGE))
                .add(0.0, vertical * 0.25, 0.0);
        if (motion.length() > BUCK_MAX_SPEED) {
            motion = motion.normalize().scale(BUCK_MAX_SPEED);
        }
        this.setDeltaMovement(motion);
        this.hurtMarked = true;

        this.setYRot((float) (Mth.atan2(this.buckHeading.z, this.buckHeading.x) * (180.0 / Math.PI)) - 90.0F);
        this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    /** A new heading 90° to 210° off the old one — a wrench, not a drift. */
    private Vec3 swerve(Vec3 current) {
        Vec3 base = current.lengthSqr() < 1.0E-4
                ? new Vec3(this.random.nextDouble() * 2.0 - 1.0, 0.0, this.random.nextDouble() * 2.0 - 1.0)
                : current;
        if (base.lengthSqr() < 1.0E-4) {
            base = new Vec3(1.0, 0.0, 0.0);
        }
        base = base.normalize();
        double turn = Math.toRadians(90.0 + this.random.nextDouble() * 120.0) * (this.random.nextBoolean() ? 1.0 : -1.0);
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        return new Vec3(base.x * cos - base.z * sin, 0.0, base.x * sin + base.z * cos).normalize();
    }

    /**
     * The calm after the bridle: it hovers a moment, then flies the rider down and sets them on
     * their feet.
     *
     * <p>Fitting the saddle at any point during this cancels the whole thing. That is the intended
     * shortcut, not an escape hatch — a saddled pegasus already answers to its rider through
     * {@link #getControllingPassenger()}, so the script simply gets out of the way.
     */
    private void tickSettling() {
        if (this.isSaddled()) {
            this.handOverControls();
            return;
        }
        if (!(this.getFirstPassenger() instanceof Player)) {
            this.settling = Settling.NONE;
            return;
        }

        switch (this.settling) {
            case HOVER -> {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x * 0.80, motion.y * 0.60, motion.z * 0.80);
                this.hurtMarked = true;
                if (++this.settlingTicks >= SETTLE_HOVER_TICKS) {
                    this.settlingTicks = 0;
                    this.settling = Settling.DESCEND;
                }
            }
            case DESCEND -> {
                // Fly down first, and only call the landing once the ground is close enough for the
                // flare to mean something. Starting it up at hover altitude left the pegasus holding
                // a one-second touchdown animation through a twenty-second glide.
                if (this.isFlying() && !this.isLanding()) {
                    if (this.getY() - this.groundHeight() > this.getLandingApproachAltitude()) {
                        Vec3 motion = this.getDeltaMovement();
                        this.setDeltaMovement(motion.x * 0.85, -SETTLE_DESCENT_SPEED, motion.z * 0.85);
                        this.hurtMarked = true;
                    } else {
                        this.beginLanding();
                    }
                }
                if (!this.isFlying() || ++this.settlingTicks >= SETTLE_DESCENT_MAX_TICKS) {
                    this.settling = Settling.NONE;
                    this.settlingTicks = 0;
                    this.ejectPassengers();
                }
            }
            default -> this.settling = Settling.NONE;
        }
    }

    /** The rider fitted the saddle mid-descent: abort the script and give them the aircraft. */
    private void handOverControls() {
        this.settling = Settling.NONE;
        this.settlingTicks = 0;
        if (this.isLanding() && this.isFlying()) {
            // Drop out of the scripted approach; the rider owns the descent from here.
            this.setLanding(false);
        }
    }

    /**
     * Puts the saddle on from the back of the pegasus. Same reasoning as the bridle: a rider cannot
     * reliably click the animal they are sitting on, so {@link PegasusRiderEvents} routes it here.
     */
    void fitSaddle(Player player, ItemStack saddle) {
        this.setItemSlot(EquipmentSlot.SADDLE, saddle.split(1));
        this.playSound(SoundEvents.HORSE_SADDLE.value(), 0.9F, 1.0F);
        this.handOverControls();
    }

    /** The window closed. Down goes the rider, and the pegasus is wild again — but still here. */
    private void throwRider() {
        // Order matters: the ritual holds the rider in the saddle (see PegasusRiderEvents), so the
        // state has to be released before the eject, or our own dismount gets cancelled.
        this.taming.reset();
        this.syncState();
        this.ejectPassengers();
        this.playSound(SoundEvents.HORSE_ANGRY, 1.2F, 0.8F);
        this.setSprinting(false);
        this.buckHeading = Vec3.ZERO;
        this.buckRetargetTimer = 0;
        // Left airborne and wild: the base landing goal takes it down from here.
        if (this.isFlying()) {
            this.beginLanding();
        }
    }

    private static boolean hasApple(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isApple(player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isApple(ItemStack stack) {
        return stack.is(Items.APPLE) || isGoldenApple(stack);
    }

    private static boolean isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    // -----------------------------------------------------------------------
    // Interaction
    // -----------------------------------------------------------------------

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        PegasusTameState state = this.tameState();

        // Fitting the bridle mid-buck is the whole point of the air phase: it always works.
        if (state == PegasusTameState.BUCKING
                && held.is(MythosMortalsItems.ATHENA_BRIDLE.get())
                && player == this.getFirstPassenger()) {
            if (!this.level().isClientSide()) {
                this.fitBridle(player, held);
            }
            return InteractionResult.SUCCESS;
        }

        // Once airborne the ritual owns the pegasus; nothing but the bridle above gets through.
        if (state.isAirborneRitual()) {
            return InteractionResult.PASS;
        }

        if (state == PegasusTameState.TAMED) {
            return this.interactTamed(player, hand, held);
        }

        // Wild or already being fed: a crouching player holding an apple keeps the ritual going.
        if (state.acceptsApples() && player.isCrouching() && isApple(held)) {
            if (!this.level().isClientSide()) {
                this.feed(player, held);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult interactTamed(Player player, InteractionHand hand, ItemStack held) {
        if (!this.isOwnedBy(player)) {
            if (!this.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("pegasus.mythosmortals.not_your_mount"));
            }
            return InteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (!this.level().isClientSide()) {
                this.openInventory(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(MythosMortalsItems.ATHENA_BRIDLE.get()) && !this.hasBridle()) {
            if (!this.level().isClientSide()) {
                this.equipment.setItem(PegasusEquipment.BRIDLE_SLOT, held.split(1));
                this.playSound(SoundEvents.HORSE_SADDLE.value(), 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.SADDLE, EquipmentSlot.BODY}) {
            if (this.isEquippableInSlot(held, slot) && this.getItemBySlot(slot).isEmpty()) {
                if (!this.level().isClientSide()) {
                    this.setItemSlot(slot, held.split(1));
                    this.playSound(SoundEvents.HORSE_SADDLE.value(), 0.8F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (this.isSaddled() && !this.isVehicle()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void feed(Player player, ItemStack held) {
        boolean golden = isGoldenApple(held);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
        boolean bonded = this.taming.feed(this.random, player.getUUID(), golden);
        this.syncState();

        if (this.level() instanceof ServerLevel server) {
            ParticleFx.burst(server, bonded ? ParticleTypes.HEART : ParticleTypes.SMOKE,
                    this.position().add(0.0, this.getBbHeight() * 0.8, 0.0), bonded ? 7 : 3, 0.5, 0.02);
        }

        if (bonded) {
            this.beginAirPhase(player);
        }
    }

    /** The pegasus takes the feeder up itself — no saddle needed, and no say in the matter. */
    private void beginAirPhase(Player player) {
        this.taming.beginBonding();
        this.phaseTicks = 0;
        this.airPhaseDeadline = this.tickCount + AIR_PHASE_MAX_TICKS;
        this.syncState();
        player.startRiding(this, true, true);
        this.getNavigation().stop();
        if (!this.isFlying()) {
            this.beginTakeoff();
        }
        this.playSound(SoundEvents.HORSE_BREATHE, 1.0F, 1.0F);
    }

    /**
     * Fits the bridle and ends the ritual. Called both from {@code mobInteract} and from
     * {@link PegasusRiderEvents}, since a rider cannot reliably click the mount beneath them.
     * Server-side only, and always succeeds — that certainty is the point of the bridle.
     */
    void fitBridle(Player player, ItemStack bridle) {
        this.equipment.setItem(PegasusEquipment.BRIDLE_SLOT, bridle.split(1));
        this.taming.complete(player.getUUID());
        this.setPersistenceRequired();
        this.settling = Settling.HOVER;
        this.settlingTicks = 0;
        this.syncState();
        this.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel server) {
            ParticleFx.burst(server, ParticleTypes.HEART,
                    this.position().add(0.0, this.getBbHeight() * 0.8, 0.0), 12, 0.7, 0.03);
        }
    }

    private void openInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new PegasusInventoryMenu(containerId, inventory, this.equipment, this),
                            this.getDisplayName()),
                    buf -> buf.writeVarInt(this.getId()));
        }
    }

    // -----------------------------------------------------------------------
    // Riding & flight control
    // -----------------------------------------------------------------------

    /**
     * Deliberately null during the ritual: the rider is aboard but has no say until the bridle is
     * on. Afterwards it takes a saddle to steer and a bridle to leave the ground.
     *
     * <p>Every term here reads synced state, exactly as vanilla mounts do. Ownership is checked when
     * mounting, not here — the owner UUID lives only on the server, so consulting it would make this
     * method answer differently on each side and the client would never take the controls.
     */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (this.isTamed() && this.isSaddled() && this.getFirstPassenger() instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        // In the air the body swings into the turn rather than snapping onto the rider's look. The
        // flight direction still comes straight from where they are looking, so steering stays
        // immediate — it is only the two-metre animal that takes a moment to come round, which is
        // what the banking roll needs in order to read. On the ground the body yaw *is* the steering,
        // so there it follows exactly.
        float yaw = this.isFlying()
                ? Mth.approachDegrees(this.getYRot(), player.getYRot(), RIDDEN_YAW_TURN_SPEED)
                : player.getYRot();
        this.setRot(yaw, player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        if (this.level().isClientSide()) {
            return;
        }
        var input = PegasusFlightController.riderInput(player);
        // Synced through the vanilla shared flags, which is what the second-gear animations read.
        this.setSprinting(input.sprint() && input.forward());

        if (!this.isFlying() && !this.isTakingOff() && input.jump() && this.hasBridle()) {
            this.beginTakeoff();
        } else if (this.isFlying() && !this.isLanding() && input.shift() && this.isNearGround()) {
            this.beginLanding();
        }
    }

    private boolean isNearGround() {
        return this.getY() - this.groundHeight() <= this.getLandingApproachAltitude();
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        // Deliberately not excluding the take-off: the rider flies through it. Locking control out
        // for the length of the animation and handing it back all at once is what made lifting off
        // read as a jolt rather than a climb.
        if (this.isFlying() && !this.isLanding()
                && this.getControllingPassenger() instanceof Player rider) {
            PegasusFlightController.travel(this, rider);
            this.calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        var input = PegasusFlightController.riderInput(player);
        float strafe = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
        float forward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 0.25F : 0.0F);
        return new Vec3(strafe * 0.5F, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /**
     * Fires the Wind Surge, if the rider has one available.
     *
     * @return whether the dash actually went off
     */
    public boolean tryDash(Player rider) {
        if (!this.isTamed() || !this.hasBridle() || !this.isFlying()
                || this.getControllingPassenger() != rider
                || this.dashCooldown() > 0) {
            return false;
        }
        PegasusFlightController.applyDash(this, rider);
        this.entityData.set(DATA_DASH_COOLDOWN, PegasusFlightController.DASH_COOLDOWN_TICKS);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.NEUTRAL, 1.0F, 1.2F);
        if (this.level() instanceof ServerLevel server) {
            ParticleFx.burst(server, ParticleTypes.CLOUD, this.position(), 16, 1.0, 0.08);
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTamed() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Being struck is one more reason to leave. It never fights back.
        if (hurt && !this.escaping && this.tameState() == PegasusTameState.WILD && this.isAlive()) {
            this.beginEscape(source.getEntity() instanceof Player attacker ? attacker : null);
        }
        return hurt;
    }

    /**
     * Tack is gear the player put on deliberately, so all of it comes back — the vanilla per-slot
     * drop chance would eat the saddle and armour most of the time.
     */
    @Override
    protected void dropEquipment(@NotNull ServerLevel level) {
        super.dropEquipment(level);
        this.dropSlot(level, EquipmentSlot.SADDLE);
        this.dropSlot(level, EquipmentSlot.BODY);

        ItemStack bridle = this.equipment.getBridle();
        if (!bridle.isEmpty()) {
            this.spawnAtLocation(level, bridle);
            this.equipment.setItem(PegasusEquipment.BRIDLE_SLOT, ItemStack.EMPTY);
        }
    }

    private void dropSlot(ServerLevel level, EquipmentSlot slot) {
        ItemStack stack = this.getItemBySlot(slot);
        if (!stack.isEmpty()) {
            this.spawnAtLocation(level, stack.copy());
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    /** Mountain-dwelling and open-sky: never spawns in a cave or low in a valley. */
    public static boolean checkPegasusSpawnRules(EntityType<PegasusEntity> type, ServerLevelAccessor level,
                                                 EntitySpawnReason spawnReason, net.minecraft.core.BlockPos pos,
                                                 net.minecraft.util.RandomSource random) {
        return pos.getY() >= 100 && level.canSeeSky(pos);
    }

    // -----------------------------------------------------------------------
    // Animation
    // -----------------------------------------------------------------------

    @Override
    public @NotNull MobAnimator<PegasusEntity> animator() {
        return this.animator;
    }

    @Override
    public void registerAnimations() {
        StandardAnimation idle = new StandardAnimation("idle",
                new AnimSource(() -> PegasusAnimation.IDLE), Loop.REPEATING, 0, 3, 2.0F);
        StandardAnimation walk = new StandardAnimation("walk",
                new AnimSource(() -> PegasusAnimation.WALK), Loop.REPEATING, 0, 2, 1.0F);
        StandardAnimation sprint = new StandardAnimation("sprint",
                new AnimSource(() -> PegasusAnimation.SPRINT), Loop.REPEATING, 0, 1, 0.5F);
        StandardAnimation takeOff = new StandardAnimation("take_off",
                new AnimSource(() -> PegasusAnimation.TAKE_OFF), Loop.PLAY_ONCE, 0, 0, 0.753F);
        StandardAnimation flyIdle = new StandardAnimation("fly_idle",
                new AnimSource(() -> PegasusAnimation.FLY_IDLE), Loop.REPEATING, 0, 3, 0.875F);
        StandardAnimation fly = new StandardAnimation("fly",
                new AnimSource(() -> PegasusAnimation.FLY), Loop.REPEATING, 0, 2, 0.875F);
        StandardAnimation flySprint = new StandardAnimation("fly_sprint",
                new AnimSource(() -> PegasusAnimation.FLY_SPRINT), Loop.REPEATING, 0, 1, 0.5F);
        StandardAnimation landing = new StandardAnimation("landing",
                new AnimSource(() -> PegasusAnimation.LANDING), Loop.PLAY_ONCE, 0, 0, 1.25F);
        StandardAnimation tameFail = new StandardAnimation("tame_fail",
                new AnimSource(() -> PegasusAnimation.TAME_FAIL), Loop.PLAY_ONCE, 0, 0, 1.375F);
        StandardAnimation death = new StandardAnimation("death",
                new AnimSource(() -> PegasusAnimation.DEATH), Loop.PLAY_ONCE, 0, 0, 0.6667F);
        StandardAnimation fall = new StandardAnimation("fall",
                new AnimSource(() -> PegasusAnimation.FALL), Loop.REPEATING, 0, 0, 0.5F);
        StandardAnimation hitGround = new StandardAnimation("hit_ground",
                new AnimSource(() -> PegasusAnimation.HIT_GROUND), Loop.PLAY_ONCE, 0, 0, 0.5833F);

        idle.blendInMs(300).blendOutMs(150);
        walk.blendInMs(150).blendOutMs(150);
        sprint.blendInMs(150).blendOutMs(150);
        // Flap→glide is where a short crossfade freezes a wing mid-stroke; give it room.
        flyIdle.blendInMs(350).blendOutMs(250);
        fly.blendInMs(250).blendOutMs(250);
        flySprint.blendInMs(250).blendOutMs(250);
        takeOff.blendInMs(100).blendOutMs(300);
        landing.blendInMs(150).blendOutMs(250);
        tameFail.blendInMs(120).blendOutMs(200);
        // The fall→impact cut is the impact; a long crossfade softens it away.
        hitGround.blendInMs(100);

        // Both gait families are picked from the measured travel speed, and the second gear is the
        // rider's own sprint key rather than a speed threshold — so the change of animation lines up
        // with the change of control instead of trailing behind it.
        idle.setPlayCondition(anim -> !this.isFlying() && this.travelSpeed() < WALK_SPEED);
        walk.setPlayCondition(anim ->
                !this.isFlying() && this.travelSpeed() >= WALK_SPEED && !this.isSprinting());
        sprint.setPlayCondition(anim ->
                !this.isFlying() && this.travelSpeed() >= WALK_SPEED && this.isSprinting());

        flyIdle.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && this.travelSpeed() < FLY_MOVE_SPEED);
        fly.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && !this.isLanding()
                        && this.travelSpeed() >= FLY_MOVE_SPEED && !this.isSprinting());
        flySprint.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && !this.isLanding()
                        && this.travelSpeed() >= FLY_MOVE_SPEED && this.isSprinting());

        this.animator.register(idle, walk, sprint, takeOff, flyIdle, fly, flySprint, landing, tameFail);
        this.animator.registerDeath(death);
        this.animator.registerFallingDeath(fall, hitGround);
    }

    @Override
    protected void onTakeoffBegin() {
        this.animator.play(this.animator.getByName("take_off"));
    }

    @Override
    protected void onLandingBegin() {
        this.animator.play(this.animator.getByName("landing"));
    }

    @Override
    protected void onLandingComplete() {
        this.animator.play(this.animator.getByName("idle"));
    }
}
