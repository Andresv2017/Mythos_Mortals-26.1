package net.darkblade.mythosmortals.entity.athenian;

import net.darkblade.deluxelib.anim.AnimSound;
import net.darkblade.deluxelib.anim.AnimSource;
import net.darkblade.deluxelib.anim.Loop;
import net.darkblade.deluxelib.anim.MobAnimator;
import net.darkblade.deluxelib.anim.StandardAnimation;
import net.darkblade.deluxelib.combat.AttackShape;
import net.darkblade.deluxelib.combat.HitWindow;
import net.darkblade.deluxelib.entity.GuardingMeleeEntity;
import net.darkblade.deluxelib.entity.ai.goal.GuardedMeleeAttackGoal;
import net.darkblade.deluxelib.entity.ai.pathing.DirectionalMoveControl;
import net.darkblade.deluxelib.entity.ai.rotation.SmoothBodyRotationControl;
import net.darkblade.mythosmortals.entity.SoldierSounds;
import net.darkblade.mythosmortals.registry.MythosMortalsSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
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
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianAnimation;

public class AthenianEntity extends GuardingMeleeEntity {

    private final MobAnimator<AthenianEntity> animator;
    private boolean nextAttackIsSlice = false;

    public AthenianEntity(EntityType<? extends AthenianEntity> type, Level level) {
        super(type, level);
        this.animator = new MobAnimator<>(this);
        this.moveControl = new DirectionalMoveControl<>(this).setTurnSpeed(7.0F).setCombatTurnSpeed(50.0F);
    }

    // 26.1: Mob#bodyRotationControl is private/final; supply a custom control by overriding
    // createBodyControl (called from Mob's constructor) instead of assigning the field.
    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        return new SmoothBodyRotationControl<>(this);
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.STEP_HEIGHT, 1.0);   // 26.1: step height is an attribute, was setMaxUpStep(1.0F)
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Note: no StaggerGoal here — the guard goal freezes itself while staggered (holding the
        // guard phase) so recovery flows straight back into the guard cycle. StaggerGoal stays for
        // future non-guarding staggerable mobs.
        this.guardGoal = new GuardedMeleeAttackGoal(this, 1.8)
            .reach(3.0F)
            .guardDistance(6.0F)
            .guardDuration(40, 80)
            .cooldown(10)
            .attackAnimations("attack", "attack_slice")
            .onAttack((enemy, animator) -> {
                // Damage lives in each attack's HitWindow (see initAnimations) so it lands on the
                // impact frame and can be dodged — the goal only alternates and triggers the swing.
                boolean useSlice = this.nextAttackIsSlice;
                this.nextAttackIsSlice = !this.nextAttackIsSlice;
                animator.play(animator.getByName(useSlice ? "attack_slice" : "attack"));
                return useSlice ? 15 : 10;
            });
        this.goalSelector.addGoal(1, this.guardGoal);
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Husk.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public @NotNull MobAnimator<AthenianEntity> animator() {
        return this.animator;
    }

    // --- Poise / guard-break tuning ---
    @Override
    public float maxPoise() {
        return 20.0F;
    }

    @Override
    public float staggeredDamageMultiplier() {
        return 1.5F;
    }

    @Override
    public void registerAnimations() {
        AnimSource idleData       = new AnimSource(() -> AthenianAnimation.IDLE);
        AnimSource walkData       = new AnimSource(() -> AthenianAnimation.WALK);
        AnimSource runData        = new AnimSource(() -> AthenianAnimation.RUN);
        AnimSource attackData     = new AnimSource(() -> AthenianAnimation.ATTACK);
        AnimSource attackSliceData = new AnimSource(() -> AthenianAnimation.ATTACK_SLICE);
        AnimSource guardData      = new AnimSource(() -> AthenianAnimation.GUARD);
        AnimSource guardLeftData  = new AnimSource(() -> AthenianAnimation.GUARD_LEFT);
        AnimSource guardRightData = new AnimSource(() -> AthenianAnimation.GUARD_RIGHT);
        AnimSource deathData      = new AnimSource(() -> AthenianAnimation.DEATH);
        AnimSource death2Data     = new AnimSource(() -> AthenianAnimation.DEATH2);
        AnimSource guardBreakData  = new AnimSource(() -> AthenianAnimation.GUARD_BREAK);
        AnimSource guardBreak2Data = new AnimSource(() -> AthenianAnimation.GUARD_BREAK2);

        StandardAnimation idle        = new StandardAnimation("idle",         idleData,        Loop.REPEATING, 0, 3, 2.0F);
        StandardAnimation walk        = new StandardAnimation("walk",         walkData,        Loop.REPEATING, 0, 2, 1.3514F);
        StandardAnimation run         = new StandardAnimation("run",          runData,         Loop.REPEATING, 0, 1, 0.5F);
        // Gameplay duration matches each clip's withLength (ATTACK=0.8125, ATTACK_SLICE=0.7936) so
        // the swing plays its full recovery back to rest before blending to guard — a shorter
        // duration cuts off the settle and the guard blend starts from a mid-swing pose (abrupt).
        StandardAnimation attack      = new StandardAnimation("attack",       attackData,      Loop.PLAY_ONCE,  0, 0, 0.8125F);
        StandardAnimation attackSlice = new StandardAnimation("attack_slice", attackSliceData, Loop.PLAY_ONCE,  0, 0, 0.7936F);
        StandardAnimation guard       = new StandardAnimation("guard",        guardData,       Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation guardLeft   = new StandardAnimation("guard_left",   guardLeftData,   Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation guardRight  = new StandardAnimation("guard_right",  guardRightData,  Loop.REPEATING, 0, 0, 2.0F);
        StandardAnimation death       = new StandardAnimation("death",        deathData,       Loop.PLAY_ONCE,  0, 0, 2.1667F);
        StandardAnimation death2      = new StandardAnimation("death2",       death2Data,      Loop.PLAY_ONCE,  0, 0, 2.0F);
        // Stagger variants: gameplay duration matches each clip's withLength — that duration IS the
        // stun length (animator.isStaggering()). PLAY_ONCE like attacks/deaths.
        StandardAnimation guardBreak  = new StandardAnimation("guard_break",  guardBreakData,  Loop.PLAY_ONCE,  0, 0, 1.5F);
        StandardAnimation guardBreak2 = new StandardAnimation("guard_break2", guardBreak2Data, Loop.PLAY_ONCE,  0, 0, 2.0F);

        idle.blendInMs(150).blendOutMs(150);
        walk.blendInMs(150).blendOutMs(150);
        run.blendInMs(150).blendOutMs(150);
        // blockAdditive: a corpse must not keep tracking the player with the look-at layer.
        death.blendInMs(150).blockAdditive();
        death2.blendInMs(150).blockAdditive();
        // Stagger: blockAdditive so the broken mob doesn't keep look-at tracking; NOT invulnerable —
        // the whole point is it's open. Quick blend-in to snap into the flinch. nextAnimation "guard"
        // chains straight into the guard cycle when the stagger ends (the goal keeps guard armed
        // through the stagger) so the shield doesn't drop to rest and re-raise a second time.
        guardBreak.blendInMs(120).blendOutMs(200).blockAdditive().setNextAnimation("guard");
        guardBreak2.blendInMs(120).blendOutMs(200).blockAdditive().setNextAnimation("guard");

        // Shield blocks whenever it's animated up: the whole guard cycle, plus the tail of an
        // attack where the shield has already swung into the guard position. The trail bridges the
        // 1-tick gap between an attack ending and guard starting. Frontal-arc restriction is applied
        // by GuardingMeleeEntity on top of this omnidirectional window.
        guard.invulnerable();
        guardLeft.invulnerable();
        guardRight.invulnerable();

        // hyperArmor: a poise break landing mid-swing is deferred until the swing ends, so a
        // committed attack isn't cut in half — it lands, THEN the mob staggers.
        attack.blendInMs(200).blendOutMs(300)
                .invulnerableLastTicks(6).invulnerableTrailTicks(2)
                .blockAdditive()
                .hyperArmor()
                .sound(AnimSound.at(2, MythosMortalsSounds.SOLDIER_ATTACK.get()).pitchJitter(0.08F));
        attackSlice.blendInMs(200).blendOutMs(300)
                .invulnerableLastTicks(6).invulnerableTrailTicks(2)
                .blockAdditive()
                .hyperArmor()
                .sound(AnimSound.at(4, MythosMortalsSounds.SOLDIER_ATTACK.get()).pitchJitter(0.08F));

        // --- Footsteps -------------------------------------------------------------------
        // AnimSound frames are in TICKS, and durationTicks = (int)(withLength * 20). Contact
        // frames come from the waist bone's vertical bob in each clip: the body sits lowest
        // exactly when a foot takes the weight.
        //
        // walk (1.3514s = 27 ticks): waist Y bottoms out at 0.0s and 0.6757s.
        SoldierSounds.steps(walk, 0.85F, 0, 13);
        // run (0.5s = 10 ticks): same two-contact cycle at double tempo.
        SoldierSounds.steps(run, 1.0F, 0, 5);
        // guard_left / guard_right (2.0s = 40 ticks) are a shielded side-shuffle, not a walk:
        // the lead foot plants at 1.5s (leg Y = -0.12, waist at its lowest -4.1/-4.22) and the
        // trailing foot only drags in to settle at 2.0s == 0.0s. The drag is quieter than the
        // plant, which is what makes it read as a shuffle instead of a march.
        SoldierSounds.steps(guardLeft, 0.45F, 0);
        SoldierSounds.steps(guardLeft, 0.7F, 30);
        SoldierSounds.steps(guardRight, 0.45F, 0);
        SoldierSounds.steps(guardRight, 0.7F, 30);

        // Shield-up. guard is REPEATING, and a plain sound on a repeating animation fires every
        // cycle — .once() restricts it to the first, so it marks raising the guard rather than
        // clanking every 2 seconds while it's held.
        guard.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_BLOCK.get()).once());

        death.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_DEATH1.get()));
        death2.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_DEATH2.get()));
        guardBreak.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_POISE_BREAK.get()));
        guardBreak2.sound(AnimSound.at(0, MythosMortalsSounds.SOLDIER_POISE_BREAK.get()));

        // attack = long thrust: a thin CAPSULE (the spear) whose spearhead segment travels
        // forward across the window (sweepForward) — near targets are pricked before far ones,
        // and backing straight up dodges it. Tune tick range / reach / anchor with the hitbox
        // debug (/deluxelib debug hitboxes).
        HitWindow.of(4, 7)
                .shape(AttackShape.capsule(1.35F, 0.5F))
                .anchor(0.5F, -0.4F, 1.3F)          // right hand (side < 0 = right)
                .sweepForward(0.3F, 2.5F)           // spearhead extends out
                .damage(5.0F)
                .knockback(0.4F)
                .filter(t -> !(t instanceof GuardingMeleeEntity))
                .applyTo(attack);

        // attack_slice = horizontal sweep: a narrow SECTOR (the blade) that TRAVELS across the
        // arc (sweepAngle) — left side is hit before right, matching the visible edge. Flip the
        // sweepAngle sign if the debug shows it sweeping the wrong way.
        // attack_slice = diagonal downward cut. The melee test is top-down — Sector.contains ignores
        // height — so only the blade's HORIZONTAL travel matters. The visible cut is the spear
        // scale-pop at tick ~6 through the arm reaching front at tick ~8, so the window is tight
        // (6–9) and the sweep tracks that arc, instead of the old -70→+70 spread over 5–11 that
        // kept slicing through the recovery. The blade travels left→right: wound-up left (+65°) to
        // just past front on the right (-35°).
        HitWindow.of(6, 9)
                .shape(AttackShape.sector(3.0F, 45.0F))
                .anchor(0.8F, 0.0F, 1.3F)
                .sweepAngle(75.0F, -75.0F)          // blade travels left → right
                .damage(6.0F)
                .knockback(0.6F)
                .filter(t -> !(t instanceof GuardingMeleeEntity))
                .applyTo(attackSlice);

        idle.setPlayCondition(anim ->
            !this.isAggressive()
                && !this.isMoving()
                && this.getNavigation().isDone()
        );
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
