package net.darkblade.mythosmortals.content.minotaur;

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
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.TimedAnimationBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.WanderBehavior;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.CompositeTargeting;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.HurtByAttackerTargeting;
import net.darkblade.deluxelib.entity.ai.cortex.target.impl.NearestEntityTargeting;
import net.darkblade.deluxelib.entity.ai.pathing.DirectionalMoveControl;
import net.darkblade.deluxelib.entity.ai.rotation.SmoothBodyRotationControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.darkblade.mythosmortals.content.minotaur.behavior.ChargeHitBehavior;
import net.darkblade.mythosmortals.content.minotaur.behavior.ChargeRunBehavior;
import net.darkblade.mythosmortals.content.minotaur.behavior.ChargeStunBehavior;
import net.darkblade.mythosmortals.content.minotaur.behavior.ChargeWindupBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * Mini-boss minotauro: esqueleto de cableado del sistema Cortex + MobAnimator.
 *
 * <p>El boilerplate (sync de estado, reenvío de eventos al FSM, goal del cortex)
 * vive en {@link CortexMonster}. Aquí solo queda lo propio del minotauro: su
 * repertorio de ataques ({@link #pickAttack}), la embestida y las animaciones.</p>
 *
 * <p>La locomoción (idle/walk/run/combat_idle) ya tiene keyframes de Blockbench; el resto
 * sigue registrado con sus duraciones y HitWindows pero con el supplier en {@code null}
 * (ver TODO en {@link #registerAnimations()}).</p>
 */
public class MinotaurEntity extends CortexMonster<MinotaurEntity, MinotaurState> implements Animatable<MinotaurEntity> {

    private final MobAnimator<MinotaurEntity> animator;

    public MinotaurEntity(EntityType<? extends MinotaurEntity> type, Level level) {
        super(type, level);
        this.animator = new MobAnimator<>(this);
        this.moveControl = new DirectionalMoveControl<>(this).setTurnSpeed(6.0F).setCombatTurnSpeed(40.0F);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new SmoothBodyRotationControl<>(this);
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

    // ------------------------------------------------------------------
    // Montar (demo del sistema rider pose): click derecho monta al jugador
    // ------------------------------------------------------------------

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.level().isClientSide() && !this.isVehicle()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** The riding player drives movement/rotation (WASD), overriding the FSM's own locomotion. */
    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // Face where the rider looks — also keeps the rider's head within vanilla's ±85° passenger
        // yaw clamp, so it no longer snaps at the extremes.
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F; // backpedal slower than forward
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // ------------------------------------------------------------------
    // FSM
    // ------------------------------------------------------------------

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
                .register(MinotaurState.SPOTTED,
                        new TimedAnimationBehavior<MinotaurEntity, MinotaurState>(
                                "target_spotted", MinotaurCtx.SPOTTED_ROAR_TICKS, MinotaurState.COMBAT_IDLE)
                                .faceTarget())
                .register(MinotaurState.CHASE,
                        new ChaseTargetBehavior<MinotaurEntity, MinotaurState>(MinotaurCtx.RUN_SPEED, MinotaurEntity::pickAttack)
                                .guard(MinotaurState.COMBAT_IDLE, MinotaurCtx.MELEE_RANGE))
                .register(MinotaurState.COMBAT_IDLE,
                        new GuardBehavior<MinotaurEntity, MinotaurState>(MinotaurEntity::pickAttack)
                                .chase(MinotaurState.CHASE, MinotaurCtx.MELEE_RANGE + 1.0F))
                // Combo horizontal: A encadena a B (75%) si el objetivo sigue en rango
                .register(MinotaurState.ATTACK_HORIZONTAL_1,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_horizontal_1", 15, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(6)
                                .combo(MinotaurState.ATTACK_HORIZONTAL_2, 0.75F, MinotaurCtx.MELEE_RANGE + 0.5F)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.ATTACK_HORIZONTAL_2,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_horizontal_2", 15, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(4)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.ATTACK_VERTICAL,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_vertical", 35, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(16)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.ATTACK_PUSH,
                        new AnimatedMeleeBehavior<MinotaurEntity, MinotaurState>("attack_push", 12, MinotaurState.COMBAT_IDLE)
                                .faceTargetUntil(3)
                                .cooldown(MinotaurCtx.NEXT_MELEE_TIME, MinotaurCtx.MELEE_COOLDOWN))
                .register(MinotaurState.CHARGE_WINDUP, new ChargeWindupBehavior())
                .register(MinotaurState.CHARGE_RUN, new ChargeRunBehavior())
                .register(MinotaurState.CHARGE_HIT, new ChargeHitBehavior())
                .register(MinotaurState.CHARGE_STUN, new ChargeStunBehavior())

                // Target muerto/perdido → volver al reposo, desde cualquier estado que lo permita
                .globalRule((entity, ctx, currentStateId) -> {
                    final LivingEntity target = entity.getTarget();
                    if ((target == null || !target.isAlive()) && currentStateId != MinotaurState.IDLE.id()) {
                        return MinotaurState.IDLE.id();
                    }
                    return null;
                })

                // Saliendo del stun siempre pasa por la guardia: nunca ataca directo mareado
                .blockTransitions(MinotaurState.CHARGE_STUN,
                        MinotaurState.ATTACK_HORIZONTAL_1, MinotaurState.ATTACK_HORIZONTAL_2,
                        MinotaurState.ATTACK_VERTICAL, MinotaurState.ATTACK_PUSH,
                        MinotaurState.CHARGE_WINDUP)

                // Adquisición de objetivo: el más cercano entre jugadores y cualquier
                // GuardingMeleeEntity (atenienses, espartanos), o quien lo golpee.
                //
                // Un solo escaneo sobre LivingEntity con filtro, y no dos NearestEntityTargeting
                // apilados, porque esos se pisarían: cada instancia solo conserva el target si es
                // de SU clase (currentOfType) y si no lo reemplaza por el suyo sin comparar
                // distancias, así que el último de la lista le robaría el objetivo al otro cada
                // ciclo de retargeting aunque estuviera más lejos.
                .targeting(new CompositeTargeting<MinotaurEntity>(
                        new NearestEntityTargeting<MinotaurEntity, LivingEntity>(LivingEntity.class, 20.0, 10, true,
                                candidate -> candidate instanceof Player player
                                        ? !player.isCreative() && !player.isSpectator()
                                        : candidate instanceof GuardingMeleeEntity),
                        new HurtByAttackerTargeting<>(400)
                ))
                .build();
    }

    /**
     * Selección de ataque compartida por CHASE y COMBAT_IDLE (vía AttackSelector).
     * Devuelve null si nada es viable (cooldowns / rango) y hay que seguir
     * persiguiendo o guardando.
     */
    @Nullable
    public MinotaurState pickAttack(BehaviorContext ctx) {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return null;
        }

        final long now = level().getGameTime();
        final double distance = distanceTo(target);

        // Embestida: objetivo lejos y cooldown listo
        if (distance >= MinotaurCtx.CHARGE_MIN_RANGE && now >= ctx.get(MinotaurCtx.NEXT_CHARGE_TIME)) {
            return MinotaurState.CHARGE_WINDUP;
        }

        if (now < ctx.get(MinotaurCtx.NEXT_MELEE_TIME)) {
            return null;
        }

        // Empuje reactivo cuando lo tienen encima
        if (distance <= MinotaurCtx.PUSH_RANGE && getRandom().nextFloat() < 0.35F) {
            return MinotaurState.ATTACK_PUSH;
        }

        if (distance <= MinotaurCtx.MELEE_RANGE) {
            return getRandom().nextFloat() < 0.3F
                    ? MinotaurState.ATTACK_VERTICAL
                    : MinotaurState.ATTACK_HORIZONTAL_1;
        }

        return null;
    }

    // ------------------------------------------------------------------
    // Reacciones (el reenvío de eventos al FSM lo hace CortexMonster)
    // ------------------------------------------------------------------

    @Override
    public boolean hurtServer(@NotNull net.minecraft.server.level.ServerLevel serverLevel, @NotNull DamageSource source, float amount) {
        final boolean wasHurt = super.hurtServer(serverLevel, source, amount);

        // Flinch de hurt solo fuera de los swings (super armor durante ataques/embestida)
        if (wasHurt) {
            final MinotaurState state = serverState();
            if (state == MinotaurState.IDLE || state == MinotaurState.CHASE
                    || state == MinotaurState.COMBAT_IDLE || state == MinotaurState.CHARGE_STUN) {
                this.animator.play(this.animator.getByName("hurt"));
            }
        }

        return wasHurt;
    }

    // ------------------------------------------------------------------
    // Animaciones (muerte: registerDeath dispara la animación al morir y
    // retrasa la eliminación hasta que termina — ver MobAnimator)
    // ------------------------------------------------------------------

    @Override
    public @NotNull MobAnimator<MinotaurEntity> animator() {
        return this.animator;
    }

    @Override
    public void registerAnimations() {
        // TODO(Blockbench): faltan los keyframes de charge_loop y del resto de los bloques 2/3/4
        //  (attack_horizontal_1 ya tiene su definición real).
        //  Reemplazar cada `() -> null` por su definición real cuando estén. Con supplier
        //  null el servidor funciona completo (FSM, HitWindows, sync) y el cliente
        //  simplemente no dibuja esa animación — el resto sí se renderiza.

        // --- Bloque 1: locomoción (loops, arrancan solos por play condition) ---
        // La duración de un loop es la longitud real del clip: al agotarse reinicia el ciclo
        // (restartCycle) y vuelve a evaluar la play condition, así que desalinearla contra el
        // AnimationDefinition solo desincroniza ese chequeo del ciclo visual.
        StandardAnimation idle       = new StandardAnimation("idle",        new AnimSource(() -> MinotaurAnimation.IDLE), Loop.REPEATING, 0, 3, 2.0F);
        StandardAnimation walk       = new StandardAnimation("walk",        new AnimSource(() -> MinotaurAnimation.WALK), Loop.REPEATING, 0, 2, 2.0F);
        StandardAnimation run        = new StandardAnimation("run",         new AnimSource(() -> MinotaurAnimation.RUN), Loop.REPEATING, 0, 1, 0.75F);
        StandardAnimation combatIdle = new StandardAnimation("combat_idle", new AnimSource(() -> MinotaurAnimation.COMBAT_STANCE), Loop.REPEATING, 0, 1, 2.0F);
        StandardAnimation chargeLoop = new StandardAnimation("charge_loop", new AnimSource(() -> null), Loop.REPEATING, 0, 1, 0.5F);

        // --- Bloque 2: reacciones ---
        StandardAnimation hurt  = new StandardAnimation("hurt",  new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 0.3F);
        StandardAnimation death = new StandardAnimation("death", new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 2.0F);

        // --- Bloques 3 y 4: one-shots disparados por los behaviors en onEnter ---
        StandardAnimation spotted     = new StandardAnimation("target_spotted",      new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 1.0F);
        StandardAnimation horizontal1 = new StandardAnimation("attack_horizontal_1", new AnimSource(() -> MinotaurAnimation.ATTACK_HORIZONTAL_1), Loop.PLAY_ONCE, 0, 0, 0.75F);
        StandardAnimation horizontal2 = new StandardAnimation("attack_horizontal_2", new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 0.75F);
        StandardAnimation vertical    = new StandardAnimation("attack_vertical",     new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 1.75F);
        StandardAnimation push        = new StandardAnimation("attack_push",         new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 0.6F);
        StandardAnimation chargeStart = new StandardAnimation("charge_start",        new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 0.75F);
        StandardAnimation chargeHit   = new StandardAnimation("charge_hit",          new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 0.5F);
        StandardAnimation chargeStun  = new StandardAnimation("charge_stun",         new AnimSource(() -> null), Loop.PLAY_ONCE, 0, 0, 2.0F);

        // Blends: locomoción responsiva; ataques comprometen rápido y asientan lento.
        // Los loops cruzan 300 ms y no 150: IDLE no anima ni la raíz, ni `top`, ni las piernas,
        // así que al pasar a walk/combat_stance esos huesos no interpolan "pose de idle → pose de
        // walk" sino "pose de bind → mitad de zancada" (top gira 14°, leftLeg se traslada 5
        // unidades). 3 ticks para eso se lee como un salto.
        idle.blendInMs(300).blendOutMs(300);
        walk.blendInMs(300).blendOutMs(300);
        run.blendInMs(200).blendOutMs(250);
        combatIdle.blendInMs(300).blendOutMs(300);
        horizontal1.blendInMs(200).blendOutMs(300).blockAdditive();
        horizontal2.blendInMs(150).blendOutMs(300).blockAdditive();
        vertical.blendInMs(200).blendOutMs(300).blockAdditive();
        push.blendInMs(120).blendOutMs(200).blockAdditive();

        // --- Ventanas de daño atadas a los ticks de impacto de cada animación ---
        // Valores de arranque: afinar rangos/arcos con /deluxelib debug hitboxes

        // Combo A: barrido derecha → izquierda (impacto ticks 5-9, arco 120°)
        // La hoja viaja del extremo de wind-up (tick 3.3) al final del golpe (tick 8.3), con el
        // latigazo de `axe` en el 5.8; del 8.3 al 10.8 el brazo está congelado. La ventana cubre
        // el tramo que se mueve, no la pose retenida.
        HitWindow.of(5, 9)
                .shape(AttackShape.sector(3.2F, 90.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(-60.0F, 60.0F)
                .damage(9.0F)
                .knockback(0.6F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(horizontal1);

        // Combo B: arco inverso izquierda → derecha (impacto ticks 5-8)
        HitWindow.of(5, 8)
                .shape(AttackShape.sector(3.2F, 90.0F))
                .anchor(1.4F, 0.0F, 1.4F)
                .sweepAngle(60.0F, -60.0F)
                .damage(9.0F)
                .knockback(0.6F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(horizontal2);

        // Hachazo vertical: desciende de arriba al suelo (impacto ticks 17-20). Rompe escudos.
        HitWindow.of(17, 20)
                .shape(AttackShape.sector(2.8F, 50.0F))
                .anchor(1.6F, 0.0F, 2.5F)
                .sweepHeight(2.5F, 0.0F)
                .damage(16.0F)
                .knockback(0.3F)
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

        // Empuje con el mango: poco daño, mucho knockback (impacto ticks 4-7)
        HitWindow.of(4, 7)
                .shape(AttackShape.sector(2.0F, 70.0F))
                .anchor(1.2F, 0.0F, 1.2F)
                .damage(4.0F)
                .knockback(1.4F)
                .filter(t -> !(t instanceof MinotaurEntity))
                .applyTo(push);

        // --- Play conditions: los loops de locomoción siguen el estado del FSM ---
        // syncedState(), NO serverState(): las play conditions se evalúan en ambos lados a
        // propósito (así el loop arranca en el cliente sin esperar el round-trip del paquete de
        // sync), y el precio de ese tradeoff es que solo pueden leer estado syncado — la misma
        // razón por la que isMoving() se sincroniza acá y por la que OwlEntity sincroniza
        // DATA_IS_SCREECHING/PERCH_TARGET_ID. El cortex es server-only, así que en el cliente
        // serverState() devuelve siempre el estado por defecto (IDLE); syncedState() es el que
        // CortexMonster expone justamente para esto.
        walk.setPlayCondition(anim ->
                syncedState() == MinotaurState.IDLE
                        && this.isMoving());
        run.setPlayCondition(anim ->
                syncedState() == MinotaurState.CHASE
                        && this.isMoving());
        combatIdle.setPlayCondition(anim -> syncedState() == MinotaurState.COMBAT_IDLE);
        chargeLoop.setPlayCondition(anim -> syncedState() == MinotaurState.CHARGE_RUN);

        this.animator
                .register(idle).register(walk).register(run).register(combatIdle).register(chargeLoop)
                .register(hurt).registerDeath(death)
                .register(spotted).register(horizontal1).register(horizontal2)
                .register(vertical).register(push)
                .register(chargeStart).register(chargeHit).register(chargeStun);
    }
}
