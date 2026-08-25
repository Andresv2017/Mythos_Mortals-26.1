package net.darkblade.mythosmortals.entity.minotaur;

import net.darkblade.deluxelib.anim.AnimSource;
import net.darkblade.deluxelib.anim.Animatable;
import net.darkblade.deluxelib.anim.Loop;
import net.darkblade.deluxelib.anim.MobAnimator;
import net.darkblade.deluxelib.anim.StandardAnimation;
import net.darkblade.deluxelib.combat.AttackShape;
import net.darkblade.deluxelib.combat.HitWindow;
import net.darkblade.deluxelib.entity.CortexMonster;
import net.darkblade.deluxelib.entity.GuardingMeleeEntity;
import net.darkblade.deluxelib.entity.ai.cortex.Cortex;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.AnimatedMeleeBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.ChaseTargetBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.GuardBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.WanderBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.CompositeTargeting;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.HurtByAttackerTargeting;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.NearestEntityTargeting;
import net.darkblade.deluxelib.entity.ai.pathing.DirectionalMoveControl;
import net.darkblade.deluxelib.entity.ai.rotation.SmoothBodyRotationControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.darkblade.mythosmortals.entity.minotaur.behavior.ChargeHitBehavior;
import net.darkblade.mythosmortals.entity.minotaur.behavior.ChargeRunBehavior;
import net.darkblade.mythosmortals.entity.minotaur.behavior.ChargeStunBehavior;
import net.darkblade.mythosmortals.entity.minotaur.behavior.ChargeWindupBehavior;
import net.darkblade.mythosmortals.entity.minotaur.behavior.PushBehavior;
import net.darkblade.mythosmortals.entity.minotaur.behavior.SpottedRoarBehavior;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurAnimation;
import net.darkblade.mythosmortals.entity.minotaur.debug.MinotaurAnimDebug;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinotaurEntity extends CortexMonster<MinotaurEntity, MinotaurState> implements Animatable<MinotaurEntity> {

    private final MobAnimator<MinotaurEntity> animator;

    private final MinotaurAnimDebug debug = new MinotaurAnimDebug(this);

    public MinotaurEntity(EntityType<? extends MinotaurEntity> type, Level level) {
        super(type, level);
        this.animator = new MobAnimator<>(this);
        this.moveControl = new DirectionalMoveControl<>(this).setTurnSpeed(6.0F).setCombatTurnSpeed(40.0F);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        final SmoothBodyRotationControl<MinotaurEntity> control = new SmoothBodyRotationControl<>(this);
        control.bodyLagStill = MinotaurCtx.BODY_TURN_STILL;
        return control;
    }

    @Override
    public void tick() {
        super.tick();
        if ((MinotaurCtx.DEBUG_ANIM_ACTION_BAR || MinotaurCtx.DEBUG_ANIM_CONSOLE)
                && !this.level().isClientSide()) {
            this.debug.tick();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (MinotaurCtx.ENABLE_RIDING && !this.level().isClientSide() && !this.isVehicle()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return MinotaurCtx.ENABLE_RIDING && this.getFirstPassenger() instanceof Player player
                ? player
                : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // --- FSM ---

    @Override
    protected MinotaurState defaultState() {
        return MinotaurState.IDLE;
    }

    @Override
    protected Cortex<MinotaurEntity, MinotaurState> buildCortex() {
        return Cortex.<MinotaurEntity, MinotaurState>builder(MinotaurState.IDLE)
                .register(MinotaurState.IDLE,
                        new WanderBehavior<MinotaurEntity, MinotaurState>(MinotaurCtx.WALK_SPEED)
                                .onTargetFound(MinotaurState.SPOTTED))
                // Fixed opener: roar → charge, skipping pickAttack's cooldown check.
                .register(MinotaurState.SPOTTED, new SpottedRoarBehavior())
                .register(MinotaurState.CHASE,
                        new ChaseTargetBehavior<MinotaurEntity, MinotaurState>(MinotaurCtx.RUN_SPEED, MinotaurEntity::pickAttack)
                                .guard(MinotaurState.COMBAT_IDLE, MinotaurCtx.MELEE_RANGE))
                .register(MinotaurState.COMBAT_IDLE,
                        new GuardBehavior<MinotaurEntity, MinotaurState>(MinotaurEntity::pickAttack)
                                .chase(MinotaurState.CHASE, MinotaurCtx.ATTACK_RANGE))
                // faceTargetUntil(3): the sweep starts at tick 3.3, so the facing must be
                // committed by then or the damage arc and the drawn arc diverge.
                .register(MinotaurState.ATTACK_HORIZONTAL_1,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_horizontal_1", MinotaurCtx.COMBO_A_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(3)
                                .combo(MinotaurState.ATTACK_HORIZONTAL_2, MinotaurCtx.COMBO_CHAIN_CHANCE, MinotaurCtx.COMBO_CHAIN_RANGE)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                // Finishers are tried in order, so the short-range one goes first or the
                // long-range one swallows its cases.
                .register(MinotaurState.ATTACK_HORIZONTAL_2,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_horizontal_2", MinotaurCtx.COMBO_B_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(3)
                                .combo(MinotaurState.ATTACK_PUSH, MinotaurCtx.COMBO_FINISHER_CHANCE, MinotaurCtx.PUSH_RANGE)
                                .combo(MinotaurState.ATTACK_VERTICAL, MinotaurCtx.COMBO_FINISHER_CHANCE, MinotaurCtx.COMBO_CHAIN_RANGE)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.ATTACK_VERTICAL,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_vertical", MinotaurCtx.VERTICAL_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(16)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.ATTACK_PUSH, new PushBehavior())
                .register(MinotaurState.CHARGE_WINDUP, new ChargeWindupBehavior())
                .register(MinotaurState.CHARGE_RUN, new ChargeRunBehavior())
                .register(MinotaurState.CHARGE_HIT, new ChargeHitBehavior())
                .register(MinotaurState.CHARGE_STUN, new ChargeStunBehavior())

                .globalRule((entity, ctx, currentStateId) -> {
                    final LivingEntity target = entity.getTarget();
                    if ((target == null || !target.isAlive()) && currentStateId != MinotaurState.IDLE.id()) {
                        return MinotaurState.IDLE.id();
                    }
                    return null;
                })

                // Leaving the stun always goes through the guard.
                .blockTransitions(MinotaurState.CHARGE_STUN,
                        MinotaurState.ATTACK_HORIZONTAL_1, MinotaurState.ATTACK_HORIZONTAL_2,
                        MinotaurState.ATTACK_VERTICAL, MinotaurState.ATTACK_PUSH,
                        MinotaurState.CHARGE_WINDUP)

                // One filtered LivingEntity scan, not two stacked NearestEntityTargeting: those
                // only keep a target of their own class, so the last one would steal it every cycle.
                .targeting(new CompositeTargeting<MinotaurEntity>(
                        new NearestEntityTargeting<MinotaurEntity, LivingEntity>(LivingEntity.class, 20.0, 10, true,
                                candidate -> candidate instanceof Player player
                                        ? !player.isCreative() && !player.isSpectator()
                                        : candidate instanceof GuardingMeleeEntity),
                        new HurtByAttackerTargeting<>(400)
                ))
                .build();
    }


    @Nullable
    public MinotaurState pickAttack(BehaviorContext ctx) {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return null;
        }

        final long now = level().getGameTime();
        final double distance = distanceTo(target);
        final boolean chargeReady = MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && now >= ctx.get(MinotaurCtx.NEXT_CHARGE_TIME);

        if (chargeReady
                && distance >= MinotaurCtx.CHARGE_MIN_RANGE
                && distance <= MinotaurCtx.CHARGE_MAX_RANGE) {
            return MinotaurState.CHARGE_WINDUP;
        }

        if (distance > MinotaurCtx.ATTACK_RANGE) {
            return null;
        }

        if (now < ctx.get(MinotaurCtx.NEXT_MELEE_TIME)) {
            return null;
        }

        if (MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && chargeReady && distance <= MinotaurCtx.PUSH_RANGE) {
            return MinotaurState.ATTACK_PUSH;
        }

        if (MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && distance <= MinotaurCtx.PUSH_CONTACT_RANGE) {
            return MinotaurState.ATTACK_PUSH;
        }

        if (MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && distance <= MinotaurCtx.VERTICAL_RANGE
                && getRandom().nextFloat() < MinotaurCtx.VERTICAL_CHANCE) {
            return MinotaurState.ATTACK_VERTICAL;
        }

        return MinotaurState.ATTACK_HORIZONTAL_1;
    }

    // --- animations ---

    @Override
    public @NotNull MobAnimator<MinotaurEntity> animator() {
        return this.animator;
    }

    private StandardAnimation sinKeyframes(String name, Loop loop, int priority, float duration) {
        this.debug.markMissing(name);
        return new StandardAnimation(name, new AnimSource(() -> null), loop, 0, priority, duration);
    }

    @Override
    public void registerAnimations() {
        // --- locomotion loops ---
        // A loop's duration must match the real clip length or the play-condition re-check
        // desyncs from the visual cycle.
        StandardAnimation idle       = new StandardAnimation("idle",        new AnimSource(() -> MinotaurAnimation.IDLE), Loop.REPEATING, 0, 3, 2.0F);
        StandardAnimation walk       = new StandardAnimation("walk",        new AnimSource(() -> MinotaurAnimation.WALK), Loop.REPEATING, 0, 2, 2.0F);
        StandardAnimation run        = new StandardAnimation("run",         new AnimSource(() -> MinotaurAnimation.RUN), Loop.REPEATING, 0, 1, 0.75F);
        StandardAnimation combatIdle = new StandardAnimation("combat_idle", new AnimSource(() -> MinotaurAnimation.COMBAT_STANCE), Loop.REPEATING, 0, 1, 2.0F);
        StandardAnimation chargeLoop = sinKeyframes("charge_loop", Loop.REPEATING, 1, 0.5F);

        // No hurt clip on purpose: a flinch would read as an interrupt the super armor never gives.
        StandardAnimation death = sinKeyframes("death", Loop.PLAY_ONCE, 0, 2.0F);

        // --- one-shots, played by the behaviors in onEnter ---
        StandardAnimation spotted     = sinKeyframes("target_spotted",      Loop.PLAY_ONCE, 0, 1.0F);
        StandardAnimation horizontal1 = new StandardAnimation("attack_horizontal_1", new AnimSource(() -> MinotaurAnimation.COMBO_A), Loop.PLAY_ONCE, 0, 0, MinotaurCtx.COMBO_CLIP_SECONDS);
        StandardAnimation horizontal2 = new StandardAnimation("attack_horizontal_2", new AnimSource(() -> MinotaurAnimation.COMBO_B), Loop.PLAY_ONCE, 0, 0, MinotaurCtx.COMBO_CLIP_SECONDS);
        StandardAnimation vertical    = sinKeyframes("attack_vertical",     Loop.PLAY_ONCE, 0, 1.75F);
        StandardAnimation push        = sinKeyframes("attack_push",         Loop.PLAY_ONCE, 0, 0.6F);
        StandardAnimation chargeStart = sinKeyframes("charge_start",        Loop.PLAY_ONCE, 0, 0.75F);
        StandardAnimation chargeHit   = sinKeyframes("charge_hit",          Loop.PLAY_ONCE, 0, 0.5F);
        StandardAnimation chargeStun  = sinKeyframes("charge_stun",         Loop.PLAY_ONCE, 0, 2.0F);

        // Loops cross in 300 ms, not 150: IDLE animates neither the root, `top` nor the legs, so
        // leaving it interpolates from the bind pose rather than from an idle pose.
        idle.blendInMs(300).blendOutMs(300);
        walk.blendInMs(300).blendOutMs(300);
        run.blendInMs(200).blendOutMs(250);
        combatIdle.blendInMs(300).blendOutMs(300);
        // The combo crosses short: A and B share the same neutral start/end pose.
        horizontal1.blendInMs(200).blendOutMs(250).blockAdditive();
        horizontal2.blendInMs(80).blendOutMs(250).blockAdditive();
        vertical.blendInMs(200).blendOutMs(300).blockAdditive();
        push.blendInMs(120).blendOutMs(200).blockAdditive();

        // --- hit windows, bound to each animation's impact ticks ---
        //
        // Sweep angles are read off the keyframes (body + torso + top yaw) and NEGATED: in the
        // bone a positive yaw turns right, while sweepAngle documents positive as left.
        // Verify in-game with /deluxelib debug hitboxes before trusting the numbers.

        HitWindow.of(4, 8)
                .shape(AttackShape.sector(3.2F, 50.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(50.0F, -65.0F)
                .damage(9.0F)
                // Deliberately low: A's job is to leave the target where B can still reach.
                .knockback(0.25F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(horizontal1);

        // Combo B starts at tick 5, not 4: at 4 the arc is still behind the right shoulder.
        HitWindow.of(5, 9)
                .shape(AttackShape.sector(3.4F, 50.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(-60.0F, 30.0F)
                .damage(11.0F)
                .knockback(0.9F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(horizontal2);

        HitWindow.of(17, 20)
                // Narrow arc, long reach (~6.3 blocks by the end of the sweep): with ~1s of
                // telegraph, dodging it should cost a step SIDEWAYS, not one step back.
                .shape(AttackShape.sector(3.2F, 40.0F))
                .anchor(1.6F, 0.0F, 2.5F)
                .sweepHeight(2.5F, 0.0F)
                .sweepForward(1.6F, 2.8F)
                .damage(18.0F)
                .knockback(0.4F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .onHit((attacker, target) -> {
                    // 26.1: Player#disableShield is gone; break the raised shield via the item's
                    // BlocksAttacks component (same path Player#blockUsingItem uses).
                    if (target instanceof Player player && player.isBlocking()
                            && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        net.minecraft.world.item.ItemStack blocking = player.getItemBlockingWith();
                        net.minecraft.world.item.component.BlocksAttacks blocksAttacks =
                                blocking != null ? blocking.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS) : null;
                        if (blocksAttacks != null) {
                            blocksAttacks.disable(serverLevel, player, 5.0F, blocking);
                        }
                    }
                })
                .applyTo(vertical);

        // Push: the damage is decorative, the knockback is the point. forward = 0 puts the cone's
        // vertex at the feet — offset forward, a target hugging the mob falls behind it and the
        // push reproduces the very dead zone it exists to cover.
        HitWindow.of(4, 7)
                .shape(AttackShape.sector(3.4F, 100.0F))
                .anchor(0.0F, 0.0F, 1.2F)
                .damage(5.0F)
                .knockback(MinotaurCtx.PUSH_KNOCKBACK)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(push);

        // --- play conditions: the five loops PARTITION the FSM state ---
        //
        // Mutually exclusive and total, so exactly one loop is ever a candidate. `idle` used to
        // have no condition and leaked into combat as the fallback.
        //
        // syncedState(), NOT serverState(): these run on both sides and the cortex is server-only.
        idle.setPlayCondition(anim ->
                syncedState() == MinotaurState.IDLE
                        && !this.isMoving());
        walk.setPlayCondition(anim ->
                syncedState() == MinotaurState.IDLE
                        && this.isMoving());
        run.setPlayCondition(anim ->
                syncedState() == MinotaurState.CHASE
                        && this.isMoving());
        chargeLoop.setPlayCondition(anim -> syncedState() == MinotaurState.CHARGE_RUN);
        combatIdle.setPlayCondition(anim -> {
            final MinotaurState state = syncedState();
            return state != MinotaurState.IDLE
                    && state != MinotaurState.CHARGE_RUN
                    && !(state == MinotaurState.CHASE && this.isMoving());
        });

        this.animator
                .register(idle).register(walk).register(run).register(combatIdle).register(chargeLoop)
                .registerDeath(death)
                .register(spotted).register(horizontal1).register(horizontal2)
                .register(vertical).register(push)
                .register(chargeStart).register(chargeHit).register(chargeStun);
    }
}
