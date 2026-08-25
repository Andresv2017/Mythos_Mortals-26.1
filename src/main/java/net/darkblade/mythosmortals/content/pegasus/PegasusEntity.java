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

public class PegasusEntity extends AbstractFlyingEntity implements Animatable<PegasusEntity> {

    private static final EntityDataAccessor<Integer> DATA_TAME_STATE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BRIDLE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DASH_COOLDOWN =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TRAVEL_SPEED =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.FLOAT);

    private static final double ESCAPE_DISTANCE = 96.0;
    private static final double ESCAPE_FORWARD_SPEED = 0.90;
    private static final double ESCAPE_CLIMB_SPEED = 0.30;
    private static final int ESCAPE_MAX_TICKS = 400;
    private static final int BUCK_STROKE_MIN = 25;
    private static final int BUCK_STROKE_MAX = 45;
    private static final double BUCK_SURGE = 0.28;
    private static final double BUCK_MAX_SPEED = 1.4;
    private static final double BUCK_MIN_ALTITUDE = 12.0;
    private static final double BUCK_MAX_ALTITUDE = 45.0;
    private static final int AIR_PHASE_MAX_TICKS = 500;
    private static final int SETTLE_HOVER_TICKS = 100;
    private static final double SETTLE_DESCENT_SPEED = 0.35;
    private static final int SETTLE_DESCENT_MAX_TICKS = 400;
    private static final double BUCK_ALTITUDE = 14.0;
    private static final double BONDED_CLIMB_SPEED = 0.35;
    private static final int TAKEOFF_TICKS = 16;

    private static final float RIDDEN_MAX_PITCH = 18.0F;

    private static final double GLIDE_SPEED = 1.0;
    private static final double GLIDE_ACCEL = 0.07;
    private static final double GLIDE_ARRIVE_RADIUS = 3.0;

    static final float WALK_SPEED = 0.015F;
    static final float FLY_MOVE_SPEED = 0.08F;

    private final MobAnimator<PegasusEntity> animator = new MobAnimator<>(this);
    private final PegasusFlightDebug flightDebug = new PegasusFlightDebug(this);
    private final PegasusTaming taming = new PegasusTaming();
    private final PegasusEquipment equipment = new PegasusEquipment(this);

    private boolean escaping;
    private Vec3 escapeOrigin = Vec3.ZERO;
    private Vec3 escapeHeading = Vec3.ZERO;
    private int escapeTicks;

    private int buckRetargetTimer;
    private Vec3 buckHeading = Vec3.ZERO;
    private double buckVertical;
    private int airPhaseDeadline;

    private enum Settling { NONE, HOVER, DESCEND }

    private Settling settling = Settling.NONE;
    private int settlingTicks;

    private double lastTickX;
    private double lastTickZ;
    private boolean hasLastTickPosition;

    @Nullable Vec3 glideTarget;
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

    private void syncState() {
        this.entityData.set(DATA_TAME_STATE, this.taming.state().ordinal());
        this.entityData.set(DATA_HAS_BRIDLE, this.equipment.hasBridle());
    }

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

    @Override
    protected double getWanderHorizontalRadius() { return 30.0; }

    @Override
    protected float getFlightYawTurnSpeed() { return 5.0F; }

    private static final float RIDDEN_YAW_TURN_SPEED = 7.0F;

    @Override
    protected int computeMaxFlightTicks() { return 400 + this.random.nextInt(200); }

    @Override
    protected int computeFlightHoverTicks() { return 20 + this.random.nextInt(20); }

    @Override
    protected int computeGroundRestTicks() { return 600 + this.random.nextInt(600); }

    @Override
    protected boolean shouldStayGrounded() { return this.isTamed(); }

    @Override
    protected double getTakeoffLiftSpeed() { return 0.15; }

    @Override
    protected void beginTakeoff() {
        super.beginTakeoff();
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, this.getTakeoffLiftSpeed(), motion.z);
    }

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

    private boolean isBusyWithRider() {
        return this.isVehicle() || this.escaping || this.taming.state().isTaming();
    }

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

    public float travelSpeed() {
        return this.entityData.get(DATA_TRAVEL_SPEED);
    }

    // Narrow views onto protected base state, for PegasusFlightDebug only.
    int flightDurationForDebug() { return this.flightDurationTimer; }

    int groundRestForDebug() { return this.groundRestTimer; }

    double groundHeightForDebug() { return this.groundHeight(); }

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

    private void sustainLanding() {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.9, -this.getLandingDescentSpeed(), motion.z * 0.9);
        this.hurtMarked = true;
        if (this.onGround() || this.verticalCollision || this.isInWater()) {
            this.completeLanding();
        }
    }

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

    private void handOverControls() {
        this.settling = Settling.NONE;
        this.settlingTicks = 0;
        if (this.isLanding() && this.isFlying()) {
            // Drop out of the scripted approach; the rider owns the descent from here.
            this.setLanding(false);
        }
    }

    void fitSaddle(Player player, ItemStack saddle) {
        this.setItemSlot(EquipmentSlot.SADDLE, saddle.split(1));
        this.playSound(SoundEvents.HORSE_SADDLE.value(), 0.9F, 1.0F);
        this.handOverControls();
    }

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
