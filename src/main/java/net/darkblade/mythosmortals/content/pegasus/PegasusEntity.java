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

    /** Height gained before a fleeing pegasus is considered gone for good. */
    private static final double ESCAPE_ALTITUDE = 40.0;
    private static final double ESCAPE_CLIMB_SPEED = 0.55;
    /** How often the bucking flight picks a new erratic destination. */
    private static final int BUCK_RETARGET_MIN = 20;
    private static final int BUCK_RETARGET_MAX = 30;
    private static final double BUCK_RADIUS = 12.0;
    /** Minimum height the ritual climbs to, so the throw-off is a real fall. */
    private static final double BUCK_ALTITUDE = 14.0;
    private static final double BONDED_CLIMB_SPEED = 0.35;
    /** Length of the authored take_off animation, in ticks. */
    private static final int TAKEOFF_TICKS = 16;

    private final MobAnimator<PegasusEntity> animator = new MobAnimator<>(this);
    private final PegasusTaming taming = new PegasusTaming();
    private final PegasusEquipment equipment = new PegasusEquipment(this);

    /** Set while climbing away from the player for good; ends in {@code discard()}. */
    private boolean escaping;
    private double escapeStartY;

    private int buckRetargetTimer;
    private Vec3 buckTarget = Vec3.ZERO;
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

    public boolean isOwnedBy(Player player) {
        return this.taming.isOwnedBy(player.getUUID());
    }

    // -----------------------------------------------------------------------
    // Flight tuning
    // -----------------------------------------------------------------------

    @Override
    protected double getMinFlightAltitude() { return 12.0; }

    @Override
    protected double getMaxFlightAltitude() { return 45.0; }

    @Override
    protected double getWanderHorizontalRadius() { return 28.0; }

    @Override
    protected int computeMaxFlightTicks() { return 300 + this.random.nextInt(240); }

    @Override
    protected double getTakeoffLiftSpeed() { return 0.14; }

    @Override
    protected double getLandingDescentSpeed() { return 0.07; }

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
        });
        this.goalSelector.addGoal(3, new FlightWanderGoal() {
            @Override
            public boolean canUse() {
                return !isBusyWithRider() && super.canUse();
            }
        });
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

    /** True whenever something other than the goal system owns the movement. */
    private boolean isBusyWithRider() {
        return this.isVehicle() || this.escaping || this.taming.state().isTaming();
    }

    /**
     * A wild pegasus hears you coming. Crouching keeps you under its notice; walking up to it does
     * not, and it leaves for good.
     */
    private class BoltFromPlayerGoal extends Goal {

        BoltFromPlayerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (escaping || tameState() != PegasusTameState.WILD) {
                return false;
            }
            Player intruder = level().getNearestPlayer(PegasusEntity.this, PegasusTaming.FLEE_RADIUS);
            return intruder != null && !intruder.isCrouching() && !intruder.isSpectator();
        }

        @Override
        public void start() {
            beginEscape();
        }
    }

    // -----------------------------------------------------------------------
    // Ritual & escape ticking
    // -----------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
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

        if (this.escaping) {
            this.tickEscape();
            return;
        }

        switch (this.taming.state()) {
            case FEEDING -> this.tickFeeding();
            case BONDED -> this.tickBonded();
            case BUCKING -> this.tickBucking();
            default -> { }
        }
    }

    /** Lift, then close the takeoff once the authored take_off animation has had its 0.75 s. */
    private void sustainTakeoff() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.y < this.getTakeoffLiftSpeed() * 2.0) {
            this.setDeltaMovement(motion.x * 0.6, this.getTakeoffLiftSpeed() * 2.0, motion.z * 0.6);
            this.hurtMarked = true;
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

    /** Straight up and gone. Flying entities take no fall damage, so nothing catches it on the way. */
    private void tickEscape() {
        this.setDeltaMovement(this.getDeltaMovement().x * 0.8, ESCAPE_CLIMB_SPEED, this.getDeltaMovement().z * 0.8);
        this.hurtMarked = true;
        if (this.getY() - this.escapeStartY >= ESCAPE_ALTITUDE) {
            this.discard();
        }
    }

    private void beginEscape() {
        this.escaping = true;
        this.escapeStartY = this.getY();
        this.phaseTicks = 0;
        this.getNavigation().stop();
        if (!this.isFlying()) {
            this.beginTakeoff();
        }
        this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.2F);
        this.animator.play(this.animator.getByName("tame_fail"));
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
            this.beginEscape();
        }
    }

    /**
     * Bridging phase: the take-off plays out, then the pegasus climbs until being thrown off would
     * actually be a fall worth fearing. Only then does the bucking proper start.
     */
    private void tickBonded() {
        if (this.isTakingOff() || !this.isFlying()) {
            return;
        }
        if (this.getY() - this.groundHeight() < BUCK_ALTITUDE) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.9, BONDED_CLIMB_SPEED, motion.z * 0.9);
            this.hurtMarked = true;
            return;
        }
        this.taming.beginBucking();
        this.buckRetargetTimer = 0;
        this.syncState();
    }

    private double groundHeight() {
        return this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition()).getY();
    }

    private void tickBucking() {
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

    /** Erratic flight: a fresh destination every second or so, with no regard for the rider. */
    private void driveBucking() {
        if (--this.buckRetargetTimer <= 0) {
            this.buckRetargetTimer = BUCK_RETARGET_MIN + this.random.nextInt(BUCK_RETARGET_MAX - BUCK_RETARGET_MIN);
            double groundY = this.groundHeight();
            this.buckTarget = new Vec3(
                    this.getX() + (this.random.nextDouble() - 0.5) * 2.0 * BUCK_RADIUS,
                    groundY + BUCK_ALTITUDE + this.random.nextDouble() * 8.0,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 2.0 * BUCK_RADIUS);
            this.playSound(SoundEvents.HORSE_ANGRY, 0.8F, 1.0F + this.random.nextFloat() * 0.3F);
        }

        Vec3 toTarget = this.buckTarget.subtract(this.position());
        if (toTarget.lengthSqr() > 1.0E-4) {
            Vec3 push = toTarget.normalize().scale(0.35);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.85).add(push));
            this.hurtMarked = true;
            this.setYRot((float) (Mth.atan2(push.z, push.x) * (180.0 / Math.PI)) - 90.0F);
            this.yBodyRot = this.getYRot();
        }
    }

    /** The window closed. Down goes the rider, and the pegasus is wild again — but still here. */
    private void throwRider() {
        this.ejectPassengers();
        this.playSound(SoundEvents.HORSE_ANGRY, 1.2F, 0.8F);
        this.taming.reset();
        this.syncState();
        this.buckTarget = Vec3.ZERO;
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

        // Fitting the bridle mid-buck is the whole point of the air phase: it always works.
        if (this.tameState() == PegasusTameState.BUCKING
                && held.is(MythosMortalsItems.ATHENA_BRIDLE.get())
                && player == this.getFirstPassenger()) {
            if (!this.level().isClientSide()) {
                this.fitBridle(player, held);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.tameState().isTaming()) {
            return InteractionResult.PASS;
        }

        if (this.isTamed()) {
            return this.interactTamed(player, hand, held);
        }

        // Wild: only a crouching player holding an apple gets anywhere.
        if (player.isCrouching() && isApple(held)) {
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
     * {@link PegasusBridleEvents}, since a rider cannot reliably click the mount beneath them.
     * Server-side only, and always succeeds — that certainty is the point of the bridle.
     */
    void fitBridle(Player player, ItemStack bridle) {
        this.equipment.setItem(PegasusEquipment.BRIDLE_SLOT, bridle.split(1));
        this.taming.complete(player.getUUID());
        this.setPersistenceRequired();
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
        // Deliberately null during the ritual: the rider is aboard but has no say until the bridle
        // is on. Afterwards it takes a saddle to steer and a bridle to leave the ground.
        if (this.isTamed()
                && this.isSaddled()
                && this.getFirstPassenger() instanceof Player player
                && this.isOwnedBy(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        if (this.level().isClientSide()) {
            return;
        }
        var input = PegasusFlightController.riderInput(player);
        if (!this.isFlying() && !this.isTakingOff() && input.jump() && this.hasBridle()) {
            this.beginTakeoff();
        } else if (this.isFlying() && !this.isLanding() && input.shift() && this.isNearGround()) {
            this.beginLanding();
        }
    }

    private boolean isNearGround() {
        return this.getY() - this.groundHeight() <= 2.5;
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        if (this.isFlying() && !this.isTakingOff() && !this.isLanding()
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
            this.beginEscape();
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

        idle.setPlayCondition(anim -> !this.isFlying() && !this.isGroundMoving());
        walk.setPlayCondition(anim -> !this.isFlying() && this.isGroundMoving() && !this.isSprintingOnGround());
        sprint.setPlayCondition(anim -> !this.isFlying() && this.isGroundMoving() && this.isSprintingOnGround());

        flyIdle.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && !this.isFlyingMoving());
        fly.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && !this.isLanding()
                        && this.isFlyingMoving() && !this.isFastFlight());
        flySprint.setPlayCondition(anim ->
                this.isFlying() && !this.isTakingOff() && !this.isLanding()
                        && this.isFlyingMoving() && this.isFastFlight());

        this.animator.register(idle, walk, sprint, takeOff, flyIdle, fly, flySprint, landing, tameFail);
        this.animator.registerDeath(death);
        this.animator.registerFallingDeath(fall, hitGround);
    }

    /** Gallop threshold on the ground. */
    private boolean isSprintingOnGround() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 0.02;
    }

    /** Dashing or otherwise moving fast enough that the flap cycle should speed up. */
    private boolean isFastFlight() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 0.30 || this.dashCooldown() > 80;
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
