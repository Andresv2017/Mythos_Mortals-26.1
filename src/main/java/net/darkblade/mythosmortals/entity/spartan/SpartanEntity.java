package net.darkblade.mythosmortals.entity.spartan;

import net.darkblade.deluxelib.anim.AnimSound;
import net.darkblade.deluxelib.anim.AnimSource;
import net.darkblade.deluxelib.anim.Loop;
import net.darkblade.deluxelib.anim.MobAnimator;
import net.darkblade.deluxelib.anim.StandardAnimation;
import net.darkblade.deluxelib.combat.AttackShape;
import net.darkblade.deluxelib.combat.HitWindow;
import net.darkblade.deluxelib.combat.WeaponPoise;
import net.darkblade.deluxelib.entity.GuardingMeleeEntity;
import net.darkblade.deluxelib.entity.ai.goal.GuardedMeleeAttackGoal;
import net.darkblade.deluxelib.entity.ai.pathing.DirectionalMoveControl;
import net.darkblade.deluxelib.entity.ai.rotation.SmoothBodyRotationControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.darkblade.mythosmortals.entity.SoldierSounds;
import net.darkblade.mythosmortals.registry.MythosMortalsSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanAnimation;

public class SpartanEntity extends GuardingMeleeEntity {

    private final MobAnimator<SpartanEntity> animator;
    private boolean nextAttackIsSlice = false;
    private boolean wasRaisingGuard = false;

    public SpartanEntity(EntityType<? extends SpartanEntity> type, Level level) {
        super(type, level);
        this.animator = new MobAnimator<>(this);
        this.moveControl = new DirectionalMoveControl<>(this).setTurnSpeed(7.0F).setCombatTurnSpeed(50.0F);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new SmoothBodyRotationControl<>(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.0);

    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.guardGoal = new GuardedMeleeAttackGoal(this, 1.8)
                .reach(3.0F)
                .guardDistance(6.0F)
                .guardDuration(40, 80)
                .cooldown(10)
                .attackAnimations("attack", "attack_slice")
                .onAttack((enemy, animator) -> {
                    // Damage lives in each attack's HitWindow (see registerAnimations) so it lands
                    // on the impact frame — the goal only alternates and triggers the swing.
                    boolean useSlice = this.nextAttackIsSlice;
                    this.nextAttackIsSlice = !this.nextAttackIsSlice;
                    animator.play(animator.getByName(useSlice ? "attack_slice" : "attack"));
                    return useSlice ? 12 : 10;
                });
        this.goalSelector.addGoal(1, this.guardGoal);
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Husk.class, true));
    }

    @Override
    public @NotNull MobAnimator<SpartanEntity> animator() {
        return this.animator;
    }


    // Vanilla sound hooks. Mob#baseTick drives the ambient timer (getAmbientSoundInterval() = 80
    // ticks) and LivingEntity#playHurtSound the hurt one; neither GuardingMeleeEntity nor the mod
    // overrides baseTick, so both fire normally. Both returned null before, which is why the
    // soldiers were mute outside of their animations.
    @Override
    protected SoundEvent getAmbientSound() {
        return MythosMortalsSounds.SOLDIER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return MythosMortalsSounds.SOLDIER_HURT.get();
    }

    // Edge-detect the guard raise. isRaisingGuard() is a state that holds for the first 6 ticks of
    // the guard phase, not a one-shot, so polling it would fire the sound six times per raise —
    // the same mistake that put the block sound on the guard animation. Only the false -> true
    // transition is the gesture of bringing the shield up.
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        boolean raising = this.guardGoal != null && this.guardGoal.isRaisingGuard();
        if (raising && !this.wasRaisingGuard) {
            SoldierSounds.shieldUp(this);
        }
        this.wasRaisingGuard = raising;
    }

    // The shield sound belongs to the hit, not to the pose. GuardingMeleeEntity#hurtServer is
    // where a frontal hit is actually absorbed, and it hardcodes SoundEvents.SHIELD_BLOCK with no
    // hook to swap it — so the blocked branch is mirrored here to play our own clang instead.
    // Everything else (the poise damage, cancelling the hit) matches the library exactly, and
    // unblocked hits fall through to super, which re-checks the guard and then takes the normal
    // damage path.
    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        if (this.isGuardingFrontal(source.getEntity())) {
            this.applyPoiseDamage(WeaponPoise.forHit(source, this) * this.blockedPoiseFactor(), source);
            SoldierSounds.blocked(this);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // --- Poise / guard-break tuning (Spartan guards a touch tougher than the Athenian) ---
    @Override
    public float maxPoise() {
        return 22.0F;
    }

    @Override
    public float staggeredDamageMultiplier() {
        return 1.5F;
    }

    @Override
    public void registerAnimations() {
        AnimSource idleData       = new AnimSource(() -> SpartanAnimation.IDLE);
        AnimSource walkData       = new AnimSource(() -> SpartanAnimation.WALK);
        AnimSource runData        = new AnimSource(() -> SpartanAnimation.RUN);
        AnimSource attackData     = new AnimSource(() -> SpartanAnimation.ATTACK);
        AnimSource attackSliceData = new AnimSource(() -> SpartanAnimation.ATTACK_SLICE);
        AnimSource guardData      = new AnimSource(() -> SpartanAnimation.GUARD);
        AnimSource guardLeftData  = new AnimSource(() -> SpartanAnimation.GUARD_LEFT);
        AnimSource guardRightData = new AnimSource(() -> SpartanAnimation.GUARD_RIGHT);
        AnimSource deathData      = new AnimSource(() -> SpartanAnimation.DEATH);
        AnimSource death2Data     = new AnimSource(() -> SpartanAnimation.DEATH2);
        AnimSource guardBreakData  = new AnimSource(() -> SpartanAnimation.GUARD_BREAK);
        AnimSource guardBreak2Data = new AnimSource(() -> SpartanAnimation.GUARD_BREAK2);

        StandardAnimation idle        = new StandardAnimation("idle",         idleData,        Loop.REPEATING, 0, 3, 2.0F);
        StandardAnimation walk        = new StandardAnimation("walk",         walkData,        Loop.REPEATING, 0, 2, 1.3514F);
        StandardAnimation run         = new StandardAnimation("run",          runData,         Loop.REPEATING, 0, 1, 0.5F);
        // Gameplay duration MUST match each clip's withLength (ATTACK=0.7606, ATTACK_SLICE=0.7936)
        // — the same convention Athenian follows. If it's shorter, the animation is cut off before
        // its recovery keyframes finish (the arm/shield settling back toward guard), and the blend
        // into the guard pose starts from a mid-swing pose → looks abrupt.
        StandardAnimation attack      = new StandardAnimation("attack",       attackData,      Loop.PLAY_ONCE,  0, 0, 0.7606F);
        StandardAnimation attackSlice = new StandardAnimation("attack_slice", attackSliceData, Loop.PLAY_ONCE,  0, 0, 0.7936F);
        StandardAnimation guard       = new StandardAnimation("guard",        guardData,       Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation guardLeft   = new StandardAnimation("guard_left",   guardLeftData,   Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation guardRight  = new StandardAnimation("guard_right",  guardRightData,  Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation death       = new StandardAnimation("death",        deathData,       Loop.PLAY_ONCE,  0, 0, 2.0F);
        StandardAnimation death2      = new StandardAnimation("death2",       death2Data,      Loop.PLAY_ONCE,  0, 0, 2.0F);
        // Stagger variants: gameplay duration = each clip's withLength (that IS the stun length).
        StandardAnimation guardBreak  = new StandardAnimation("guard_break",  guardBreakData,  Loop.PLAY_ONCE,  0, 0, 1.5F);
        StandardAnimation guardBreak2 = new StandardAnimation("guard_break2", guardBreak2Data, Loop.PLAY_ONCE,  0, 0, 2.0F);

        // Per-animation blend tuning, matching Athenian: responsive locomotion swaps, attacks
        // that commit fast (200ms in) and settle slow out of the swing (300ms out).
        idle.blendInMs(150).blendOutMs(150);
        walk.blendInMs(150).blendOutMs(150);
        run.blendInMs(150).blendOutMs(150);
        // blockAdditive: a corpse must not keep tracking the player with the look-at layer.
        death.blendInMs(150).blockAdditive();
        death2.blendInMs(150).blockAdditive();
        // Stagger: NOT invulnerable (it's the opening), blockAdditive, and chain straight into the
        // guard cycle when it ends so the shield doesn't drop to rest and re-raise (double-raise fix).
        guardBreak.blendInMs(120).blendOutMs(200).blockAdditive().setNextAnimation("guard");
        guardBreak2.blendInMs(120).blendOutMs(200).blockAdditive().setNextAnimation("guard");

        // Shield blocks while guarding. Spartan's attack doesn't recover into the guard pose (its
        // shield stays down through the swing), so only the guard cycle grants immunity — no
        // attack-tail window like Athenian.
        guard.invulnerable();
        guardLeft.invulnerable();
        guardRight.invulnerable();

        // hyperArmor: a poise break mid-swing is deferred until the swing finishes (the attack lands,
        // THEN the Spartan staggers) instead of cutting the animation in half.
        attack.blendInMs(200).blendOutMs(300)
                .blockAdditive()
                .hyperArmor()
                .sound(AnimSound.at(2, MythosMortalsSounds.SOLDIER_ATTACK.get()).pitchJitter(0.08F));
        attackSlice.blendInMs(200).blendOutMs(300)
                .blockAdditive()
                .hyperArmor()
                .sound(AnimSound.at(3, MythosMortalsSounds.SOLDIER_ATTACK.get()).pitchJitter(0.08F));

        // --- Footsteps -------------------------------------------------------------------
        // Same rig and the same contact frames as the Athenian (see SoldierSounds): AnimSound
        // frames are in TICKS, durationTicks = (int)(withLength * 20), and the contacts are
        // where the waist bone's vertical bob bottoms out.
        //
        // walk (1.3514s = 27 ticks): waist Y bottoms out at 0.0s and 0.6757s.
        SoldierSounds.steps(walk, 0.85F, 0, 13);
        // run (0.5s = 10 ticks): same two-contact cycle at double tempo.
        SoldierSounds.steps(run, 1.0F, 0, 5);
        // guard_left / guard_right (2.0s = 40 ticks) are a shielded side-shuffle: the lead foot
        // plants at 1.5s (leg Y = -0.12, waist at its lowest -4.1/-4.22) and the trailing foot
        // only drags in to settle at 2.0s == 0.0s, so the tick-0 scuff stays quieter.
        SoldierSounds.steps(guardLeft, 0.45F, 0);
        SoldierSounds.steps(guardLeft, 0.7F, 30);
        SoldierSounds.steps(guardRight, 0.45F, 0);
        SoldierSounds.steps(guardRight, 0.7F, 30);

        death.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_DEATH1.get()));
        death2.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_DEATH2.get()));
        guardBreak.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_POISE_BREAK.get()));
        guardBreak2.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_POISE_BREAK.get()));

        // Damage window: a frontal sector active over the impact ticks of the swing. Values are
        // a starting point — tune the tick range (5-7), reach and arc live with the hitbox debug
        // (/deluxelib debug hitboxes). Skips fellow guarding mobs so Spartans don't hit each other.
        // attack_slice mirrors attack's damage/knockback/shape exactly — both swings hit equally
        // hard, only the animation and timing differ.
        HitWindow.of(5, 7)
                .shape(AttackShape.sector(2.2F, 120.0F))
                .anchor(1.0F, 0.0F, 1.0F)
                .damage(8.0F)
                .knockback(0.5F)
                .filter(t -> !(t instanceof GuardingMeleeEntity))
                .applyTo(attack);

        HitWindow.of(5, 7)
                .shape(AttackShape.sector(2.2F, 120.0F))
                .anchor(1.0F, 0.0F, 1.0F)
                .damage(8.0F)
                .knockback(0.5F)
                .filter(t -> !(t instanceof GuardingMeleeEntity))
                .applyTo(attackSlice);

        walk.setPlayCondition(anim ->
                !this.isAggressive() &&
                        (this.isMoving() || !this.getNavigation().isDone())
        );
        run.setPlayCondition(anim ->
                this.isAggressive() && (this.guardGoal == null || !this.guardGoal.isGuarding())
                        && this.isMoving()
        );
        guard.setPlayCondition(anim ->
                this.guardGoal != null && this.guardGoal.isGuarding()
                        && (this.guardGoal.isRaisingGuard() || !this.isMoving())
        );
        guardLeft.setPlayCondition(anim ->
                this.guardGoal != null && this.guardGoal.isGuarding() && !this.guardGoal.isRaisingGuard()
                        && this.isMoving()
                        && this.guardGoal.getStrafeDirection() > 0
        );
        guardRight.setPlayCondition(anim ->
                this.guardGoal != null && this.guardGoal.isGuarding() && !this.guardGoal.isRaisingGuard()
                        && this.isMoving()
                        && this.guardGoal.getStrafeDirection() < 0
        );

        this.animator.register(idle).register(walk).register(run).register(attack).register(attackSlice)
                .register(guard).register(guardLeft).register(guardRight)
                .registerStagger(guardBreak, guardBreak2)
                .registerDeath(death, death2);
    }
}