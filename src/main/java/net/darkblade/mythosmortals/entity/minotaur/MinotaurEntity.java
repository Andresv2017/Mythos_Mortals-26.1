package net.darkblade.mythosmortals.entity.minotaur;

import net.darkblade.deluxelib.anim.AnimSound;
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
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.SwingRoutine;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.PursueRoutine;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.PoiseRoutine;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.WanderRoutine;
import net.darkblade.deluxelib.entity.ai.cortex.sense.impl.CompositeSense;
import net.darkblade.deluxelib.entity.ai.cortex.sense.impl.HurtByAttackerSense;
import net.darkblade.deluxelib.entity.ai.cortex.sense.impl.NearestEntitySense;
import net.darkblade.deluxelib.entity.ai.pathing.DirectionalMoveControl;
import net.darkblade.deluxelib.entity.ai.rotation.SmoothBodyRotationControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.darkblade.mythosmortals.entity.minotaur.routine.ChargeHitRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.ChargeRecoverRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.ChargeRunRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.ChargeStunRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.ChargeWindupRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.PushRoutine;
import net.darkblade.mythosmortals.entity.minotaur.routine.SpottedRoarRoutine;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurAnimation;
import net.darkblade.mythosmortals.entity.minotaur.debug.MinotaurAnimDebug;
import net.darkblade.mythosmortals.registry.MythosMortalsDamageTypes;
import net.darkblade.mythosmortals.registry.MythosMortalsSounds;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.mythosmortals.minotaur"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.isRemoved() || !this.isAlive()) {
                this.bossEvent.removeAllPlayers();
            } else {
                this.updateBossBar();
            }
            if (MinotaurAnimDebug.isEnabled()) {
                this.debug.tick();
            }
        }
    }

    @Override
    public void remove(Entity.@NotNull RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    private void updateBossBar() {
        float max = this.getMaxHealth();
        this.bossEvent.setProgress(max <= 0.0F ? 0.0F : this.getHealth() / max);

        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        for (ServerPlayer player : server.players()) {
            if (this.distanceToSqr(player) <= MinotaurCtx.BOSS_BAR_RADIUS_SQR) {
                this.bossEvent.addPlayer(player);
            } else {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
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
                .bind(MinotaurState.IDLE,
                        new WanderRoutine<MinotaurEntity, MinotaurState>(MinotaurCtx.WALK_SPEED)
                                .onTargetFound(MinotaurState.SPOTTED))
                // Fixed opener: roar → charge, skipping pickAttack's cooldown check.
                .bind(MinotaurState.SPOTTED, new SpottedRoarRoutine())
                .bind(MinotaurState.CHASE,
                        new PursueRoutine<MinotaurEntity, MinotaurState>(MinotaurCtx.RUN_SPEED, MinotaurEntity::pickAttack)
                                .guard(MinotaurState.COMBAT_IDLE, MinotaurCtx.MELEE_RANGE))
                .bind(MinotaurState.COMBAT_IDLE,
                        new PoiseRoutine<MinotaurEntity, MinotaurState>(MinotaurEntity::pickAttack)
                                .chase(MinotaurState.CHASE, MinotaurCtx.ATTACK_RANGE))
                // faceTargetUntil(3): the sweep starts at tick 3.3, so the facing must be
                // committed by then or the damage arc and the drawn arc diverge.
                .bind(MinotaurState.ATTACK_HORIZONTAL_1,
                        new SwingRoutine<MinotaurEntity, MinotaurState>("attack_horizontal_1", MinotaurCtx.COMBO_A_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(3)
                                .combo(MinotaurState.ATTACK_HORIZONTAL_2, MinotaurCtx.COMBO_CHAIN_CHANCE, MinotaurCtx.COMBO_CHAIN_RANGE)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                // Finishers are tried in order, so the short-range one goes first or the
                // long-range one swallows its cases.
                .bind(MinotaurState.ATTACK_HORIZONTAL_2,
                        new SwingRoutine<MinotaurEntity, MinotaurState>("attack_horizontal_2", MinotaurCtx.COMBO_B_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(3)
                                .combo(MinotaurState.ATTACK_PUSH, MinotaurCtx.COMBO_FINISHER_CHANCE, MinotaurCtx.PUSH_RANGE)
                                .combo(MinotaurState.ATTACK_VERTICAL, MinotaurCtx.COMBO_FINISHER_CHANCE, MinotaurCtx.COMBO_CHAIN_RANGE)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .bind(MinotaurState.ATTACK_VERTICAL,
                        new SwingRoutine<MinotaurEntity, MinotaurState>("attack_vertical", MinotaurCtx.VERTICAL_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(16)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .bind(MinotaurState.ATTACK_PUSH, new PushRoutine())
                .bind(MinotaurState.CHARGE_WINDUP, new ChargeWindupRoutine())
                .bind(MinotaurState.CHARGE_RUN, new ChargeRunRoutine())
                .bind(MinotaurState.CHARGE_HIT, new ChargeHitRoutine())
                .bind(MinotaurState.CHARGE_STUN, new ChargeStunRoutine())
                .bind(MinotaurState.CHARGE_RECOVER, new ChargeRecoverRoutine())

                .reflex((entity, bb, active) -> {
                    final LivingEntity target = entity.getTarget();
                    if ((target == null || !target.isAlive()) && active != MinotaurState.IDLE) {
                        return MinotaurState.IDLE;
                    }
                    return null;
                })

                // Leaving the stun always goes through the guard.
                .denyAll(MinotaurState.CHARGE_STUN,
                        MinotaurState.ATTACK_HORIZONTAL_1, MinotaurState.ATTACK_HORIZONTAL_2,
                        MinotaurState.ATTACK_VERTICAL, MinotaurState.ATTACK_PUSH,
                        MinotaurState.CHARGE_WINDUP)

                // One filtered LivingEntity scan, not two stacked NearestEntitySense: those
                // only keep a target of their own class, so the last one would steal it every cycle.
                .sense(new CompositeSense<MinotaurEntity>(
                        new NearestEntitySense<MinotaurEntity, LivingEntity>(LivingEntity.class, 32.0, 10, true,
                                candidate -> candidate instanceof Player player
                                        ? !player.isCreative() && !player.isSpectator()
                                        : candidate instanceof GuardingMeleeEntity),
                        new HurtByAttackerSense<>(400)
                ))
                .build();
    }


    @Nullable
    public MinotaurState pickAttack(Blackboard bb) {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return null;
        }

        final long now = level().getGameTime();
        final double distance = distanceTo(target);
        final boolean chargeReady = MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && now >= bb.get(MinotaurCtx.NEXT_CHARGE_TIME);

        if (chargeReady
                && distance >= MinotaurCtx.CHARGE_MIN_RANGE
                && distance <= MinotaurCtx.CHARGE_MAX_RANGE) {
            return MinotaurState.CHARGE_WINDUP;
        }

        if (distance > MinotaurCtx.ATTACK_RANGE) {
            return null;
        }

        if (now < bb.get(MinotaurCtx.NEXT_MELEE_TIME)) {
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

    // --- Vanilla sound hooks -------------------------------------------------------------
    @Override
    protected SoundEvent getAmbientSound() {
        return MythosMortalsSounds.MINOTAUR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return MythosMortalsSounds.MINOTAUR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MythosMortalsSounds.MINOTAUR_DEATH.get();
    }

    @Override
    protected void tickDeath() {
        if (!this.level().isClientSide()) {
            this.debug.tickDeath();
        }
        super.tickDeath();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        if (!blockState.getFluidState().isEmpty()) {
            return;
        }
        SoundType soundType = blockState.getSoundType(this.level(), pos, this);
        this.playSound(SoundEvents.RAVAGER_STEP, soundType.getVolume() * 0.25F, soundType.getPitch() * 0.8F);
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
        // CHARGE_LOOP is the 0.5s gallop. Blockbench also exported a 2.0s variant of the exact same
        // motion (same values, times x4); the fast one is the keeper because 0.5s is what makes one
        // gallop cycle land per charge_loop.ogg (0.499s) with no overlap.
        StandardAnimation chargeLoop = new StandardAnimation("charge_loop",
                new AnimSource(() -> MinotaurAnimation.CHARGE_LOOP), Loop.REPEATING, 0, 1, 0.5F);

        // No hurt clip on purpose: a flinch would read as an interrupt the super armor never gives.
        StandardAnimation death = new StandardAnimation("death",
                new AnimSource(() -> MinotaurAnimation.DEATH), Loop.PLAY_ONCE, 0, 0, 2.4583F);

        // --- one-shots, played by the behaviors in onEnter ---
        // ALERT_ROAR was stretched to 1.70s so the bellow covers roar.ogg (1.68s) instead of
        // shutting the jaw a third of the way in. Clip length and SPOTTED_ROAR_TICKS (34) now match
        // exactly; the duration must stay the real length or AnimSound frames map to wrong ticks.
        StandardAnimation spotted     = new StandardAnimation("target_spotted",
                new AnimSource(() -> MinotaurAnimation.ALERT_ROAR), Loop.PLAY_ONCE, 0, 0, 1.70F);
        StandardAnimation horizontal1 = new StandardAnimation("attack_horizontal_1", new AnimSource(() -> MinotaurAnimation.COMBO_A), Loop.PLAY_ONCE, 0, 0, MinotaurCtx.COMBO_CLIP_SECONDS);
        StandardAnimation horizontal2 = new StandardAnimation("attack_horizontal_2", new AnimSource(() -> MinotaurAnimation.COMBO_B), Loop.PLAY_ONCE, 0, 0, MinotaurCtx.COMBO_CLIP_SECONDS);
        // COMBO_C is the overhead finisher. Its impact frame was read off the keyframes: the kinetic
        // chain peaks body (ticks 13-16) -> top (15-18) -> arm/forearm (16-19), so the axe connects
        // at 17-20 — which is where the placeholder's hit window already sat, so it carries over.
        StandardAnimation vertical    = new StandardAnimation("attack_vertical",
                new AnimSource(() -> MinotaurAnimation.COMBO_C), Loop.PLAY_ONCE, 0, 0, 1.864F);
        StandardAnimation push        = new StandardAnimation("attack_push",
                new AnimSource(() -> MinotaurAnimation.FRONT_PUSH), Loop.PLAY_ONCE, 0, 0, 0.9583F);
        // CHARGE_START's dead tail (keyframes stopped at 0.75s of a 1.0s clip) now carries the
        // settle onto CHARGE_LOOP's opening pose, so the windup uses its full second.
        StandardAnimation chargeStart = new StandardAnimation("charge_start",
                new AnimSource(() -> MinotaurAnimation.CHARGE_START), Loop.PLAY_ONCE, 0, 0, 1.40F);
        // Both were stretched for recovery: the ram's whip and the wall crash keep their original
        // speed, the settle after each is what got the extra time.
        StandardAnimation chargeHit   = new StandardAnimation("charge_hit",
                new AnimSource(() -> MinotaurAnimation.CHARGE_HIT), Loop.PLAY_ONCE, 0, 0, 1.0F);
        StandardAnimation chargeStun  = new StandardAnimation("charge_stun",
                new AnimSource(() -> MinotaurAnimation.CHARGE_STUN), Loop.PLAY_ONCE, 0, 0, 3.0F);

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

        // The roar does NOT end on the neutral pose: it finishes mid-turn (head -47 deg of yaw, torso
        // +12) and a step out of place, with the root never moving. A long blend out is what keeps
        // the return from reading as a slide.
        spotted.blendInMs(200).blendOutMs(300).blockAdditive();

        // Note that BlendState resolves a transition as max(outgoing.blendOut, incoming.blendIn),
        // so it is the INCOMING clip's blend in that sets how long a cross takes. A generous blend
        // out on the clip being left does nothing on its own.
        //
        //  - chargeStart has three possible predecessors (COMBAT_STANCE from the guard, RUN from
        //    the chase, ALERT_ROAR from the sighting) and matches none of them — they all arrive
        //    with the arm up. That is what a blend is for; 250 ms covers the worst of the three.
        //  - chargeLoop is the one case that needs almost nothing: CHARGE_START's tail was rewritten
        //    to settle exactly onto this clip's opening pose, so the cross is a continuation.
        //  - chargeHit and chargeStun open on the loop's t=0 pose, but the gallop's PHASE at the
        //    moment of impact is arbitrary — the cycle is 10 ticks and the ram lands wherever it
        //    lands — and a displaced loop freezes rather than running on. The legs can be half a
        //    cycle out, so these have to cross for real.
        chargeStart.blendInMs(250).blendOutMs(100).blockAdditive();
        chargeLoop.blendInMs(120).blendOutMs(100);
        chargeHit.blendInMs(180).blendOutMs(250).blockAdditive();
        chargeStun.blendInMs(180).blendOutMs(300).blockAdditive();

        // --- hit windows, bound to each animation's impact ticks ---
        //
        // Sweep angles are read off the keyframes (body + torso + top yaw) and NEGATED: in the
        // bone a positive yaw turns right, while sweepAngle documents positive as left.
        // Verify in-game with /deluxelib debug hitboxes before trusting the numbers.

        HitWindow.of(4, 8)
                .shape(AttackShape.sector(3.2F, 50.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(50.0F, -65.0F)
                .damage(13.0F)
                // Deliberately low: A's job is to leave the target where B can still reach.
                .knockback(0.25F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(horizontal1);

        // Combo B starts at tick 5, not 4: at 4 the arc is still behind the right shoulder.
        HitWindow.of(5, 9)
                .shape(AttackShape.sector(3.4F, 50.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(-60.0F, 30.0F)
                .damage(13.0F)
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
                .damage(20.0F)
                .damageSource(MythosMortalsDamageTypes::minotaurGore)
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
                .damage(8.0F)
                .knockback(MinotaurCtx.PUSH_KNOCKBACK)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(push);

        // --- sounds, anchored to the same ticks as the hit windows above -------------------
        // AnimSound frames are in TICKS (durationTicks = (int)(withLength * 20)). DeluxeLib ticks
        // these server-side only and broadcasts via Level#playSound, so no client guard is needed.
        //
        // Every clip below now has real keyframes, so these frames can be read against the actual
        // motion rather than against a placeholder's assumed timing.

        // The axe leads the blow: one tick ahead of each window so the whoosh reads as the cause
        // of the hit rather than its echo. pitchJitter keeps a chained combo from sounding cloned.
        horizontal1.sound(AnimSound.at(3, MythosMortalsSounds.MINOTAUR_SWING.get()).pitchJitter(0.08F));
        horizontal2.sound(AnimSound.at(4, MythosMortalsSounds.MINOTAUR_SWING.get()).pitchJitter(0.08F));

        // slam.ogg is front-loaded — the impact is in its first 8 ticks, there is no windup baked
        // in — so it fires ON the impact frame, not before it. It outlives the 35-tick clip by a
        // few ticks, which is fine: PLAY_ONCE animations do not restart it.
        vertical.sound(AnimSound.at(17, MythosMortalsSounds.MINOTAUR_SLAM.get()));

        push.sound(AnimSound.at(0, MythosMortalsSounds.MINOTAUR_PUSH.get()));
        // Tick 8, not 0, because roar.ogg starts DRY: measured in 5 ms windows it is at -6.5 dBFS
        // by 0.005s, i.e. full volume on the first sample, with no intake breath in front of the
        // bellow. Fired at 0 the minotaur roars at full blast for 8 ticks with its mouth shut.
        //
        // Tick 8 is where the jaw breaks open (0.4167s), and the rest lines up on its own: 99% of
        // the sample's energy is spent by its own tick 21 — clip tick 29 — which is exactly where
        // the jaw stops shaking and starts to close. What is left of the sample after that is below
        // -48 dBFS and covers the close. Its nominal 1.68s outlives the 34-tick clip by a few
        // ticks, which is inaudible and fine, the same way slam.ogg outlives COMBO_C.
        spotted.sound(AnimSound.at(8, MythosMortalsSounds.MINOTAUR_ROAR.get()).volume(2.0F));

        // charge_loop is REPEATING at 10 ticks and the sample is 0.499s, so one gallop cycle lands
        // per animation cycle with no overlap. No .once(): the point is that it keeps running for
        // as long as the charge does.
        chargeLoop.sound(AnimSound.at(0, MythosMortalsSounds.MINOTAUR_CHARGE_LOOP.get()));

        // The physical beats of the charge stay vanilla. These are not placeholders standing in for
        // something better — goat.prepare_ram and goat.ram_impact are literally "horned animal winds
        // up to ram" and "horned animal connects", and ravager.stunned is the same recoil state
        // charge_stun models.
        chargeStart.sound(AnimSound.at(0, SoundEvents.GOAT_PREPARE_RAM).pitch(0.7F));
        chargeHit.sound(AnimSound.at(0, SoundEvents.GOAT_RAM_IMPACT).pitch(0.7F));
        chargeStun.sound(AnimSound.at(0, SoundEvents.RAVAGER_STUNNED));

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
