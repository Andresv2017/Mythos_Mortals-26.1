package net.darkblade.mythosmortals.content.arpy;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.anim.*;
import net.darkblade.deluxelib.combat.AttackShape;
import net.darkblade.deluxelib.combat.HitWindow;
import net.darkblade.deluxelib.entity.AbstractFlyingEntity;
import net.darkblade.deluxelib.entity.IArmoredEntity;
import net.darkblade.deluxelib.entity.ai.goal.target.FlyingNearestAttackableTargetGoal;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ArpyEntity extends AbstractFlyingEntity implements Animatable<ArpyEntity>, IArmoredEntity, Enemy {

    private static final EntityDataAccessor<Boolean> DATA_IS_DIVING =
            SynchedEntityData.defineId(ArpyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ARMORED =
            SynchedEntityData.defineId(ArpyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final Identifier ARMORED_ID = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "armored");

    private static final AttributeModifier ARMORED_HEALTH =
            new AttributeModifier(ARMORED_ID, 8.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier ARMORED_ARMOR =
            new AttributeModifier(ARMORED_ID, 3.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier ARMORED_DAMAGE =
            new AttributeModifier(ARMORED_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);

    private final MobAnimator<ArpyEntity> animator = new MobAnimator<>(this);

    // -----------------------------------------------------------------------
    // Construction & attributes
    // -----------------------------------------------------------------------
    public ArpyEntity(EntityType<? extends ArpyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // -----------------------------------------------------------------------
    // Synced data
    // -----------------------------------------------------------------------
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_DIVING, false);
        builder.define(IS_ARMORED, false);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Armored", this.isArmored());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setArmored(input.getBooleanOr("Armored", this.isArmored()));
    }

    @Override
    public boolean isArmored() {return this.entityData.get(IS_ARMORED);}

    @Override
    public void setArmored(boolean armored) {
        this.entityData.set(IS_ARMORED, armored);
        this.updateArmoredModifiers(armored);
    }

    private void updateArmoredModifiers(boolean armored) {
        boolean added = false;
        added |= this.updateModifier(Attributes.MAX_HEALTH, ARMORED_HEALTH, armored);
        added |= this.updateModifier(Attributes.ARMOR, ARMORED_ARMOR, armored);
        added |= this.updateModifier(Attributes.ATTACK_DAMAGE, ARMORED_DAMAGE, armored);

        if (armored && added) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private boolean updateModifier(Holder<Attribute> attribute, AttributeModifier modifier, boolean apply) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        if (!apply) {
            instance.removeModifier(modifier.id());
            return false;
        }
        if (instance.getModifier(modifier.id()) != null) {
            return false;
        }
        instance.addPermanentModifier(modifier);
        return true;
    }

    public boolean isDiving() { return this.entityData.get(DATA_IS_DIVING); }
    public void setDiving(boolean v) { this.entityData.set(DATA_IS_DIVING, v); }

    // -----------------------------------------------------------------------
    // AbstractFlyingEntity config overrides
    // -----------------------------------------------------------------------
    @Override
    protected double getMinFlightAltitude() { return 6.0; }

    @Override
    protected double getMaxFlightAltitude() { return 12.0; }

    @Override
    protected double getWanderHorizontalRadius() { return 24.0; }

    private static final double NEST_WAKE_RANGE = 24.0;

    @Override
    protected boolean shouldStayGrounded() {
        if (!this.hasHome() || this.getTarget() != null) {
            return false;
        }
        return this.level().getNearestPlayer(this, NEST_WAKE_RANGE) == null;
    }

    @Override
    protected int computeGroundRestTicks() { return 120 + this.random.nextInt(80); }

    @Override
    protected int computeMaxFlightTicks() { return 200 + this.random.nextInt(160); }

    @Override
    protected double getLandingDescentSpeed() { return 0.06; }

    @Override
    protected double getTakeoffLiftSpeed() { return 0.08; }

    @Override
    protected double getLandingApproachAltitude() { return 0.45; }

    @Override
    protected boolean applyTiltDuringTakeoff() { return false; }

    // -----------------------------------------------------------------------
    // Animation hooks — called by AbstractFlyingEntity at lifecycle transitions
    // -----------------------------------------------------------------------
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
        this.animator.play(this.animator.getByName("idle_ground"));
    }

    // -----------------------------------------------------------------------
    // Goals — use base goals + override completion conditions for animations
    // -----------------------------------------------------------------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new ArpyTakeoffGoal());
        this.goalSelector.addGoal(2, new DiveAttackGoal());
        this.goalSelector.addGoal(3, new FlightWanderGoal());
        this.goalSelector.addGoal(4, new ArpyLandingGoal());

        WaterAvoidingRandomStrollGoal groundStroll = new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !ArpyEntity.this.hasHome() && super.canUse();
            }
        };
        groundStroll.setInterval(25);
        this.goalSelector.addGoal(5, groundStroll);
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new FlyingNearestAttackableTargetGoal<>(this, Cow.class, true, 20.0));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    private class ArpyTakeoffGoal extends TakeoffGoal {
        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            boolean engage = !isFlying() && !isTakingOff() && !isLanding()
                    && target != null && target.isAlive();
            return super.canUse() || engage;
        }

        @Override
        protected boolean shouldCompleteTakeoff() {
            return !animator.isPlaying("take_off");
        }
    }

    private class DiveAttackGoal extends Goal {
        private enum Phase { REPOSITION, ALIGN, DIVE, PULLUP }

        private static final double ATTACK_ALTITUDE = 8.0;   // blocks above the target to dive from
        private static final double REPOS_RADIUS = 9.0;      // horizontal offset → a farther, angled dive
        private static final double CRUISE_SPEED = 0.40;
        private static final double ALIGN_SPEED = 0.18;      // gentle drift while turning to face the target
        private static final double ALIGN_YAW_THRESHOLD = 22.0; // commit to the dive once facing within this
        private static final double DIVE_SPEED = 0.64;
        private static final double CLIMB_SPEED = 0.42;
        private static final double STRIKE_RANGE = 2.3;      // talons/beak reach
        private static final double PULLUP_CLEARANCE = 1.6;  // never sink below this above ground
        private static final int MAX_DIVE_TICKS = 45;        // safety: force pull-up if the dive drags

        private Phase phase = Phase.REPOSITION;
        private double reposX, reposY, reposZ;
        private boolean struck;
        private int diveTicks;

        DiveAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && isFlying() && !isTakingOff() && !isLanding();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && isFlying() && !isLanding();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            setAggressive(true);
            setDiving(false);
            this.phase = Phase.REPOSITION;
            this.struck = false;
            pickReposition();
        }

        @Override
        public void stop() {
            setDiving(false);
            setAggressive(false);
            this.phase = Phase.REPOSITION;
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x * 0.5, Math.max(m.y, 0.0), m.z * 0.5);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            switch (this.phase) {
                case REPOSITION -> tickReposition(target);
                case ALIGN -> tickAlign(target);
                case DIVE -> tickDive(target);
                case PULLUP -> tickPullUp(target);
            }
        }

        private void pickReposition() {
            LivingEntity t = getTarget();
            double baseX = t != null ? t.getX() : getX();
            double baseY = t != null ? t.getY() : getY();
            double baseZ = t != null ? t.getZ() : getZ();
            double ang = ArpyEntity.this.random.nextDouble() * Math.PI * 2.0;
            this.reposX = baseX + Math.cos(ang) * REPOS_RADIUS;
            this.reposZ = baseZ + Math.sin(ang) * REPOS_RADIUS;
            this.reposY = baseY + ATTACK_ALTITUDE;
        }

        private void tickReposition(@NotNull LivingEntity target) {
            steerToward(this.reposX, this.reposY, this.reposZ, CRUISE_SPEED, 0.25);
            double dx = this.reposX - getX();
            double dz = this.reposZ - getZ();
            boolean inPlace = dx * dx + dz * dz < 4.0;
            boolean highEnough = getY() >= target.getY() + ATTACK_ALTITUDE * 0.7;
            if (inPlace && highEnough) {
                this.phase = Phase.ALIGN;
            }
        }

        private void tickAlign(@NotNull LivingEntity target) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            faceHeading(dx, dz);

            float yawRad = (float) Math.toRadians(getYRot());
            double dy = Mth.clamp(this.reposY - getY(), -1.0, 1.0);
            Vec3 desired = new Vec3(-Math.sin(yawRad) * ALIGN_SPEED, dy * 0.1, Math.cos(yawRad) * ALIGN_SPEED);
            Vec3 cur = getDeltaMovement();
            setDeltaMovement(cur.add(desired.subtract(cur).scale(0.25)));

            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            if (Math.abs(Mth.wrapDegrees(targetYaw - getYRot())) < ALIGN_YAW_THRESHOLD) {
                this.phase = Phase.DIVE;
                this.struck = false;
                this.diveTicks = 0;
                setDiving(true);
            }
        }

        private void tickDive(@NotNull LivingEntity target) {
            this.diveTicks++;
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
            steerToward(aim.x, aim.y, aim.z, DIVE_SPEED, 0.5);

            if (!this.struck && distanceToSqr(target) <= STRIKE_RANGE * STRIKE_RANGE) {
                this.struck = true;
            }

            double groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) getX(), (int) getZ());
            boolean tooLow = getY() - groundY < PULLUP_CLEARANCE;
            boolean reachedTarget = getY() <= target.getY() + 0.5;
            if (this.struck || tooLow || reachedTarget || this.diveTicks > MAX_DIVE_TICKS) {
                this.phase = Phase.PULLUP;
                setDiving(false);
                animator.play(animator.getByName("dive_attack_return"));
            }
        }

        private void tickPullUp(@NotNull LivingEntity target) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            double fx = horiz > 0.1 ? dx / horiz : 0.0;
            double fz = horiz > 0.1 ? dz / horiz : 0.0;
            Vec3 desired = new Vec3(fx * CRUISE_SPEED * 0.6, CLIMB_SPEED, fz * CRUISE_SPEED * 0.6);
            Vec3 cur = getDeltaMovement();
            setDeltaMovement(cur.add(desired.subtract(cur).scale(0.3)));
            faceHeading(dx, dz);

            if (getY() >= target.getY() + ATTACK_ALTITUDE * 0.8) {
                pickReposition();
                this.phase = Phase.REPOSITION;
            }
        }

        private void steerToward(double tx, double ty, double tz, double speed, double accel) {
            double dx = tx - getX();
            double dy = ty - getY();
            double dz = tz - getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1.0E-4) return;
            Vec3 desired = new Vec3(dx / dist * speed, dy / dist * speed, dz / dist * speed);
            Vec3 cur = getDeltaMovement();
            setDeltaMovement(cur.add(desired.subtract(cur).scale(accel)));
            faceHeading(dx, dz);
        }

        private void faceHeading(double dx, double dz) {
            if (dx * dx + dz * dz < 0.01) return;
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float delta = Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -14.0F, 14.0F);
            setYRot(getYRot() + delta);
            ArpyEntity.this.yBodyRot = getYRot();
        }
    }


    private class ArpyLandingGoal extends LandingGoal {
        @Override
        protected boolean shouldCompleteLanding() {
            return super.shouldCompleteLanding();
        }
    }

    // -----------------------------------------------------------------------
    // Animatable
    // -----------------------------------------------------------------------
    @Override
    public @NotNull MobAnimator<ArpyEntity> animator() {
        return this.animator;
    }

    @Override
    public void registerAnimations() {
        StandardAnimation idleGround = new StandardAnimation("idle_ground",
                new AnimSource(() -> ArpyAnimation.idle_ground), Loop.REPEATING, 0, 3, 1.0F);
        StandardAnimation walk = new StandardAnimation("walk",
                new AnimSource(() -> ArpyAnimation.walk), Loop.REPEATING, 0, 2, 1.0F);
        StandardAnimation takeOff = new StandardAnimation("take_off",
                new AnimSource(() -> ArpyAnimation.take_off), Loop.PLAY_ONCE, 0, 0, 1.5F);
        StandardAnimation idleFly = new StandardAnimation("idle_fly",
                new AnimSource(() -> ArpyAnimation.idle_fly), Loop.REPEATING, 0, 3, 1.0528F);
        StandardAnimation flySprint = new StandardAnimation("fly_sprint",
                new AnimSource(() -> ArpyAnimation.fly_sprint), Loop.REPEATING, 0, 2, 1.0085F);
        StandardAnimation flySprintAngry = new StandardAnimation("fly_sprint_angry",
                new AnimSource(() -> ArpyAnimation.fly_sprint_angry), Loop.REPEATING, 0, 1, 1.0248F);
        StandardAnimation diveAttack = new StandardAnimation("dive_attack",
                new AnimSource(() -> ArpyAnimation.dive_attack), Loop.REPEATING, 0, 0, 1.0417F);
        StandardAnimation diveReturn = new StandardAnimation("dive_attack_return",
                new AnimSource(() -> ArpyAnimation.dive_attack_return), Loop.PLAY_ONCE, 0, 0, 0.5F);
        StandardAnimation landing = new StandardAnimation("landing",
                new AnimSource(() -> ArpyAnimation.landing), Loop.PLAY_ONCE, 0, 0, 0.5F);
        StandardAnimation deathGround = new StandardAnimation("death_ground",
                new AnimSource(() -> ArpyAnimation.death_ground), Loop.PLAY_ONCE, 0, 0, 2.375F);
        StandardAnimation deathGround2 = new StandardAnimation("death_ground2",
                new AnimSource(() -> ArpyAnimation.death_ground2), Loop.PLAY_ONCE, 0, 0, 2.4167F);
        StandardAnimation deathFalling = new StandardAnimation("death_falling",
                new AnimSource(() -> ArpyAnimation.death_falling), Loop.REPEATING, 0, 0, 1.0526F);
        StandardAnimation hitGround = new StandardAnimation("hit_ground",
                new AnimSource(() -> ArpyAnimation.hit_ground), Loop.PLAY_ONCE, 0, 0, 1.5228F);

        // Per-animation blend tuning. Ground locomotion swaps fast (responsive); aerial poses are
        // broad and slow-moving, so their crossfades breathe more (soaring feel). take_off commits
        // fast but settles slowly into flight; landing eases out into the glide/ground pose.
        // idle_ground blends IN slower than the rest of the ground family: at touchdown the wings
        // fold from the spread glide pose, and a quick 150ms fold reads as a second landing flare.
        idleGround.blendInMs(300).blendOutMs(150);
        walk.blendInMs(150).blendOutMs(150);
        // Slow blend INTO the glide pose (flap→glide is where a short crossfade freezes a wing
        // mid-stroke and reads as a snap); blend OUT stays quick so starting to flap is energetic.
        idleFly.blendInMs(350).blendOutMs(250);

        // idle_fly's hover tempo is now authored into the keyframes themselves — play it at 1:1
        // (the old 0.6 slowdown was compensation for the previous, faster authoring; applied to
        // the re-authored cycle it made the hover crawl). The travel flaps keep a small boost so
        // the wing tempo still tracks air speed.
        flySprint.playbackSpeed(1.2F);
        flySprintAngry.playbackSpeed(1.2F);
        flySprint.blendInMs(250).blendOutMs(250);
        flySprintAngry.blendInMs(250).blendOutMs(250);
        takeOff.blendInMs(100).blendOutMs(300);
        landing.blendInMs(150).blendOutMs(250);
        diveAttack.blendInMs(100).blendOutMs(250);
        deathGround.blendInMs(150);
        deathGround2.blendInMs(150);
        deathFalling.blendInMs(200);
        // Short blend: the fall→impact cut IS the impact — a long crossfade softens it away.
        hitGround.blendInMs(100);

        // dive_attack is CYCLE at 20 ticks/loop (tick resets to 0 each restartCycle) and isDiving()
        // can stay true for a variable number of loops depending on approach distance/speed — the
        // strike moment is physics-driven, not a scripted swing frame. Covering the whole tick
        // range with a SPHERE (no facing dependency) re-evaluates proximity every tick for as long
        // as the dive lasts, which is exactly the goal's own physics-driven check, just routed
        // through the shared combat API (debug particles, knockback, hit-once dedup).
        // Anchor is the arpy's own position (no forward offset) — tune with the hitbox debug
        // (/deluxelib debug hitboxes) if the talons should reach a bit ahead of the body instead.
        HitWindow.of(0, 20)
                .shape(AttackShape.sphere(1.9F))
                .anchor(0.0F, 0.0F, 0.0F)
                .damage(4.0F)   // matches ATTACK_DAMAGE in createAttributes() — a literal, same
                                // convention as Spartan/Athenian; doesn't dynamically track the
                                // live attribute the way doHurtTarget() did.
                .knockback(0.6F) // deliberate addition: doHurtTarget's default knockback for this
                                 // mob was effectively zero (no ATTACK_KNOCKBACK configured) — a
                                 // diving strike reads better with a real shove on impact.
                .applyTo(diveAttack);

        // Ground animations — only play when not flying
        idleGround.setPlayCondition(anim ->
                !this.isFlying()
                && this.getDeltaMovement().horizontalDistanceSqr() <= 2.5E-7);

        walk.setPlayCondition(anim ->
                !this.isFlying()
                && this.getDeltaMovement().horizontalDistanceSqr() > 2.5E-7);

        // Fly cycle — uses smoothed speed (isFlyingMoving) to prevent instant flicker between
        // idle_fly and fly_sprint. ~2.5s hold timer before transitioning back to idle_fly.
        // idle_fly intentionally does NOT exclude isLanding(): the landing anim (priority 0)
        // out-renders it while playing, and once that 0.5s anim ends idle_fly covers the rest
        // of the flare descent — without it the model freezes in bind pose until touchdown.
        idleFly.setPlayCondition(anim ->
                this.isFlying()
                && !this.isDiving()
                && !this.isTakingOff()
                && !this.isFlyingMoving());

        flySprint.setPlayCondition(anim ->
                this.isFlying()
                && !this.isDiving()
                && !this.isAggressive()
                && !this.isTakingOff()
                && !this.isLanding()
                && this.isFlyingMoving());

        flySprintAngry.setPlayCondition(anim ->
                this.isFlying()
                && !this.isDiving()
                && this.isAggressive()
                && !this.isTakingOff()
                && !this.isLanding()
                && this.isFlyingMoving());

        diveAttack.setPlayCondition(anim -> this.isDiving());

        // Death variant selection (conditions are consulted only at the moment of death):
        // grounded → one of the two ground variants at random; airborne → death_falling loops
        // while the corpse drops (die() clears noGravity), then chains into hit_ground on landing.
        deathGround.setPlayCondition(anim -> !this.isFlying());
        deathGround2.setPlayCondition(anim -> !this.isFlying());
        deathFalling.setPlayCondition(anim -> this.isFlying());

        this.animator.register(idleGround, walk, takeOff, idleFly, flySprint,
                flySprintAngry, diveAttack, diveReturn, landing);
        this.animator.registerDeath(deathGround, deathGround2);
        this.animator.registerFallingDeath(deathFalling, hitGround);
    }
}
