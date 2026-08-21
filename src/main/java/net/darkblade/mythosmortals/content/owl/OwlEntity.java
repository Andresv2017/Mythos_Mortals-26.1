package net.darkblade.mythosmortals.content.owl;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.anim.*;
import net.darkblade.deluxelib.camera.ScreenShake;
import net.darkblade.deluxelib.combat.AttackAnchor;
import net.darkblade.deluxelib.combat.AttackShape;
import net.darkblade.deluxelib.combat.HitWindow;
import net.darkblade.deluxelib.entity.AbstractFlyingEntity;
import net.darkblade.deluxelib.entity.perch.PerchManager;
import net.darkblade.deluxelib.entity.perch.PerchPlacement;
import net.darkblade.deluxelib.entity.perch.Perchable;
import net.darkblade.deluxelib.entity.possession.Possessable;
import net.darkblade.deluxelib.entity.possession.PossessionManager;
import net.darkblade.deluxelib.math.Interpolation;
import net.darkblade.mythosmortals.content.arpy.ArpyAnimation;
import net.darkblade.mythosmortals.content.arpy.ArpyEntity;
import net.darkblade.mythosmortals.content.owl.perch.OwlPerchPlacement;
import net.darkblade.deluxelib.vfx.ParticleFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OwlEntity extends AbstractFlyingEntity implements Animatable<OwlEntity>, Possessable, Perchable {

    private static final EntityDataAccessor<Boolean> DATA_IS_DIVING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> CONTROLLER_ID =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> PERCH_TARGET_ID =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_IS_SCREECHING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);

    private final MobAnimator<OwlEntity> animator = new MobAnimator<>(this);


    // -----------------------------------------------------------------------
    // Possession (Athena's Sight) — server-side authority
    // -----------------------------------------------------------------------
    private static final int POSSESSION_DURATION_TICKS = 600;   // 30 s
    private static final int POST_POSSESSION_COOLDOWN_TICKS = 200;
    private static final double POSSESSED_SPEED = 0.5;          // target flight speed (blocks/tick)
    private static final double POSSESSED_ACCEL = 0.2;          // velocity blend per tick
    private static final double POSSESSED_DIVE_SPEED = 0.95;    // swoop speed during the auto-dive
    private static final double POSSESSED_DIVE_ACCEL = 0.5;     // commits into the dive quickly
    private static final double POSSESSED_DIVE_DOWN_BIAS = 0.7; // downward tilt added to the aim
    private static final int POSSESSED_RETURN_TICKS = 14;       // auto pull-up after the dive
    private static final double POSSESSED_RETURN_CLIMB = 0.55;  // upward speed during the pull-up
    private static final double POSSESSED_RETURN_FWD = 0.25;    // forward drift while climbing out
    private static final double POSSESSED_RETURN_ACCEL = 0.35;
    private static final double POSSESSED_MAX_RADIUS = 56.0;    // horizontal leash from the body
    private static final double POSSESSED_MAX_CLIMB = 40.0;     // ceiling above the anchor

    // -----------------------------------------------------------------------
    // Defending its owner (see DefendOwnerGoal and the two targeting goals)
    // -----------------------------------------------------------------------
    private static final double DEFEND_ACQUIRE_RANGE = 16.0;
    private static final double DEFEND_LEASH_RADIUS = 24.0;
    private static final double ORDER_LEASH_RADIUS = 112.0;
    private boolean targetWasOrdered;

    private static final double SAFETY_RADIUS = 10.0;

    // -----------------------------------------------------------------------
    // Sonic screech — the ranged attack, mouse wheel while possessed
    // -----------------------------------------------------------------------
    private static final int SONIC_TOTAL_TICKS = 26;
    private static final int SONIC_RELEASE_TICK = 14;
    private static final int SONIC_COOLDOWN_TICKS = 80;      // 4 s between screeches
    private static final float SONIC_RANGE = 18.0F;
    private static final float SONIC_RADIUS = 1.1F;
    private static final float SONIC_DAMAGE = 7.0F;
    private static final float SONIC_POISE_DAMAGE = 12.0F;
    private static final double SONIC_KNOCKBACK_H = 1.6;
    private static final double SONIC_KNOCKBACK_V = 0.4;
    private static final float SONIC_ORIGIN_FORWARD = 0.45F;
    private static final double SONIC_PARTICLE_SPACING = 1.0;
    private static final int SONIC_PARTICLE_SKIP = 1;
    private static final double SONIC_BRAKE_ACCEL = 0.35;


    private boolean hasAthenaSightUpgrade;
    private boolean hasSonicUpgrade;
    private int copperUpgrades;
    private boolean airborneInitialized;
    private boolean pendingAwaken;
    private boolean awakeStarted;
    private @Nullable UUID ownerUUID;

    private @Nullable ServerPlayer controller;
    private int possessionTicksRemaining;
    private int possessionCooldownTicks;
    private @Nullable Vec3 possessionAnchor;
    private Input controlInput = Input.EMPTY;
    /** Look rotation sent by the controlling client. The server player's own rotation is unreliable
     * while the camera is on the owl, so the client reports it directly. */
    private float controlYaw;
    private float controlPitch;
    /** Ticks left in the active dive-strike (the auto-dive window); also gates re-triggering. */
    private int controlAttackCooldown;
    /** Ticks left in the automatic pull-up return that follows the dive. */
    private int controlReturnTicks;
    /** Ticks left in the active sonic screech (windup + release), or 0 when not screeching. */
    private int controlSonicTicks;
    /** Ticks until the screech is available again, counted from the moment it starts. */
    private int controlSonicCooldown;
    /** Mobs the owl hit while piloted, matched by identity in {@link #clearAggro} so distance and
     * travel time don't matter. Value = the tick recorded, so entries that never resolve age out
     * via {@link #GRUDGE_MAX_AGE_TICKS}. */
    private final Map<Mob, Integer> grudgeMobs = new HashMap<>();
    /** Debug only: last target seen on each {@link #grudgeMobs} entry. */
    private final Map<Mob, LivingEntity> grudgeDebugLastTarget = new HashMap<>();

    /** Logs every target change on a tracked mob and each {@link #clearAggro} pass. */
    private static final boolean DEBUG_AGGRO = false;
    private static final Logger AGGRO_LOG = LoggerFactory.getLogger("DlxOwlAggro");

    // -----------------------------------------------------------------------
    // Construction & attributes
    // -----------------------------------------------------------------------
    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // -----------------------------------------------------------------------
    // Copper ingot upgrade — owner feeds copper ingots to raise max health permanently
    // -----------------------------------------------------------------------
    /** Max health granted per copper ingot fed. */
    private static final double COPPER_HEALTH_PER_INGOT = 4.0;
    /** Ingots accepted before the owl is maxed out (16 base + 5*4 = 36 HP / 18 hearts). */
    private static final int MAX_COPPER_UPGRADES = 5;
    /** Stable modifier id per upgrade level, so re-applying on load can check
     * {@link AttributeInstance#hasModifier} instead of guessing what vanilla's own attribute NBT
     * round-trip already restored. */
    private static @NotNull Identifier copperHealthModifierId(int level) {
        return Identifier.fromNamespaceAndPath(MythosMortals.MODID, "owl_copper_health_" + level);
    }

    // -----------------------------------------------------------------------
    // Bronze ingot heal — tops up current health only, unlimited uses, refused at full health
    // -----------------------------------------------------------------------
    private static final double BRONZE_HEAL_AMOUNT = 6.0;
    private static final int BRONZE_HEAL_PARTICLES = 8;
    private static final double BRONZE_HEAL_SPREAD = 0.3;
    private static final double BRONZE_HEAL_PARTICLE_SPEED = 0.08;

    // -----------------------------------------------------------------------
    // Synced data
    // -----------------------------------------------------------------------
    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_DIVING, false);
        builder.define(CONTROLLER_ID, -1);
        builder.define(PERCH_TARGET_ID, -1);
        builder.define(DATA_IS_SCREECHING, false);
    }

    public boolean isDiving() { return this.entityData.get(DATA_IS_DIVING); }
    public void setDiving(boolean v) { this.entityData.set(DATA_IS_DIVING, v); }

    /** True while the sonic screech (windup or release) is running. */
    public boolean isScreeching() { return this.entityData.get(DATA_IS_SCREECHING); }
    public void setScreeching(boolean v) { this.entityData.set(DATA_IS_SCREECHING, v); }

    /** True while the owl is perched on its owner's arm (any side — reads the synced target id). */
    @Override
    public boolean isPerched() { return this.entityData.get(PERCH_TARGET_ID) != -1; }

    /** Entity id of the player this owl is perched on, or {@code -1} when flying free. */
    @Override
    public int getPerchTargetId() { return this.entityData.get(PERCH_TARGET_ID); }

    /** Where this owl sits on its owner's arm — see {@link OwlPerchPlacement}, which is also what the
     * hitbox placement in {@link #perchPosition} reads, so the drawn bird and its real position come
     * from one set of numbers. */
    @Override
    public @NotNull PerchPlacement perchPlacement() { return OwlPerchPlacement.current(); }


    /** The player this owl is bonded to, or {@code null} if somehow spawned without one (e.g.
     * {@code /summon}) — such an owl just sits idle since {@link FollowOwnerGoal} has no one to
     * follow and {@link #checkPossessionGate} has no owner to match. */
    @Override
    public @Nullable UUID getOwnerUUID() { return this.ownerUUID; }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("AthenaSightUpgrade", this.hasAthenaSightUpgrade);
        output.putBoolean("SonicUpgrade", this.hasSonicUpgrade);
        output.putInt("CopperUpgrades", this.copperUpgrades);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        this.hasAthenaSightUpgrade = input.getBooleanOr("AthenaSightUpgrade", false);
        this.hasSonicUpgrade = input.getBooleanOr("SonicUpgrade", false);
        this.copperUpgrades = input.getIntOr("CopperUpgrades", 0);
        this.ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.applyCopperHealthModifiers();
    }

    /** Re-applies the copper max-health modifiers after a load, skipping any vanilla already
     * restored. Does not heal: a reload should restore the bonus, not top up missing health. */
    private void applyCopperHealthModifiers() {
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        for (int i = 0; i < this.copperUpgrades; i++) {
            Identifier id = copperHealthModifierId(i);
            if (!maxHealth.hasModifier(id)) {
                maxHealth.addPermanentModifier(new AttributeModifier(id, COPPER_HEALTH_PER_INGOT, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // This owl never lands, so no takeoff/landing goals are registered: force it airborne
            // once on spawn. A statue-spawned owl (pendingAwaken) first plays the grounded "awake"
            // transition — see bondTo().
            if (!this.airborneInitialized) {
                if (this.pendingAwaken) {
                    emitAwakeningParticles();
                    if (!this.animator.isPlaying("awake")) {
                        if (this.awakeStarted) {
                            // The one-shot transition finished — take flight.
                            this.pendingAwaken = false;
                            this.awakeStarted = false;
                            this.airborneInitialized = true;
                            this.startAirborne();
                        } else {
                            this.awakeStarted = true;
                            this.animator.play(this.animator.getByName("awake"));
                        }
                    }
                } else {
                    this.airborneInitialized = true;
                    this.startAirborne();
                }
            }
            // Ticked here, not in PossessionGoal: the cooldown must run while nobody is piloting.
            if (this.possessionCooldownTicks > 0) {
                this.possessionCooldownTicks--;
            }
            // Same for the screech: the defence AI fires it with nobody piloting, so ticking it in
            // PossessionGoal would leave isScreeching() stuck true forever.
            if (this.controlSonicCooldown > 0) {
                this.controlSonicCooldown--;
            }
            if (this.controlSonicTicks > 0 && --this.controlSonicTicks == 0) {
                this.setScreeching(false);   // lets idle_fly/fly_sprint take the pose back
            }
            // Only after a possession session, and paused while the owl has a target of its own —
            // mid-fight is exactly when its opponents are supposed to fight back.
            if (this.aggroClearTicks > 0) {
                this.aggroClearTicks--;
                if (getTarget() == null && this.tickCount % AGGRO_CHECK_INTERVAL_TICKS == 0) {
                    this.clearAggro();
                }
            }
            if (DEBUG_AGGRO) {
                this.debugWatchGrudgeTargets();
            }
        }
    }

    /** Debug only: logs the exact tick a tracked mob's target changes, to see what re-points it at
     * the owl between {@link #clearAggro} passes. */
    private void debugWatchGrudgeTargets() {
        if (this.grudgeMobs.isEmpty()) {
            return;
        }
        for (Mob mob : this.grudgeMobs.keySet()) {
            LivingEntity current = mob.getTarget();
            LivingEntity last = this.grudgeDebugLastTarget.get(mob);
            if (current == last) {
                continue;
            }
            this.grudgeDebugLastTarget.put(mob, current);
            AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} mob={}(id={}, alive={}) target: {} -> {}  lastHurtByMob={}",
                    this.tickCount, this.getId(),
                    mob.getClass().getSimpleName(), mob.getId(), mob.isAlive(),
                    describe(last), describe(current), describe(mob.getLastHurtByMob()));
        }
    }

    private static @NotNull String describe(@Nullable LivingEntity e) {
        if (e == null) {
            return "null";
        }
        return e.getClass().getSimpleName() + "#" + e.getId();
    }

    // -----------------------------------------------------------------------
    // Possession (Athena's Sight)
    // -----------------------------------------------------------------------
    /** True while a player is piloting this owl (any side — reads the synced controller id). */
    @Override
    public boolean isPossessed() { return this.entityData.get(CONTROLLER_ID) != -1; }

    /** Entity id of the controlling player, or {@code -1}. The controlling client uses this to
     * decide whether to move its camera onto the owl. */
    @Override
    public int getControllerId() { return this.entityData.get(CONTROLLER_ID); }

    /** Server-side: adopt the controller's latest movement intent + look direction (from
     * {@code PossessedInputServerPacket}). */
    @Override
    public void setControlState(Input input, float yaw, float pitch) {
        this.controlInput = input;
        this.controlYaw = yaw;
        this.controlPitch = pitch;
    }

    /** Server-side: the left-click dive strike. The {@code dive_attack} animation's
     * {@link HitWindow} carries the damage; its active window doubles as the re-trigger cooldown. */
    public void performControlledAttack() {
        if (this.controlAttackCooldown > 0 || this.controlReturnTicks > 0 || this.controlSonicTicks > 0) {
            return;   // already diving, pulling up, or mid-screech
        }
        this.controlAttackCooldown = 16;   // ~one dive_attack swing
        this.setDiving(true);
    }

    /**
     * Server-side: the sonic screech, ranged counterpart to the dive. The {@code sonic_screech}
     * animation's {@link HitWindow} fires an {@link AttackShape.Beam} along the aim on one exact
     * tick (see {@link #registerAnimations}).
     *
     * <p>Started imperatively because the clip is {@link Loop#PLAY_ONCE} and {@link MobAnimator}
     * only auto-starts REPEATING ones. The cooldown starts now, not when the clip ends, so the
     * usable rate is exactly {@link #SONIC_COOLDOWN_TICKS}.
     */
    public void performSonicAttack() {
        // No isPossessed() check: the defence AI fires this too (see DefendOwnerGoal).
        if (!this.hasSonicUpgrade
                || this.controlSonicTicks > 0 || this.controlSonicCooldown > 0
                || this.controlAttackCooldown > 0 || this.controlReturnTicks > 0) {
            return;   // not upgraded, on cooldown, or already committed to another move
        }
        this.controlSonicTicks = SONIC_TOTAL_TICKS;
        this.controlSonicCooldown = SONIC_COOLDOWN_TICKS;
        this.setScreeching(true);
        this.animator.play(this.animator.getByName("sonic_screech"));
    }

    /**
     * Right-click, by held item — owner-gated in every case:
     * <ul>
     *   <li><b>Empty hand</b>: toggles perching on the owner's raised right arm.</li>
     *   <li><b>Copper ingot</b>, below {@link #MAX_COPPER_UPGRADES}: raises max health by
     *       {@link #COPPER_HEALTH_PER_INGOT} and heals the same amount.</li>
     *   <li><b>Bronze ingot</b>, while not at full health: heals {@link #BRONZE_HEAL_AMOUNT}.</li>
     *   <li><b>Athena Ocular / Sonic Screech upgrade</b>: unlocks that ability, once each.</li>
     * </ul>
     * Athena's Sight is activated by keybind, never here — see {@link #tryStartPossession}.
     */
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getItemInHand(hand);

            if (held.isEmpty()) {
                if (this.level().isClientSide()) {
                    return InteractionResult.SUCCESS;   // predicts the arm-swing for the perch toggle
                }
                // Only the bonded owner can call it to their arm.
                if (player.getUUID().equals(this.ownerUUID) && !this.isPossessed()) {
                    if (this.isPerched()) {
                        this.stopPerching();
                    } else {
                        this.startPerching(player);
                    }
                    return InteractionResult.SUCCESS;
                }
                return super.mobInteract(player, hand);
            }

            // Owner-gated: a companion's stats shouldn't be tunable by a passer-by.
            if (held.is(Items.COPPER_INGOT) && player.getUUID().equals(this.ownerUUID)
                    && this.copperUpgrades < MAX_COPPER_UPGRADES) {
                if (this.level().isClientSide()) {
                    return InteractionResult.SUCCESS;   // predicts the arm-swing for feeding the ingot
                }
                AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    Identifier id = copperHealthModifierId(this.copperUpgrades);
                    if (!maxHealth.hasModifier(id)) {
                        maxHealth.addPermanentModifier(new AttributeModifier(id, COPPER_HEALTH_PER_INGOT, AttributeModifier.Operation.ADD_VALUE));
                    }
                }
                this.copperUpgrades++;
                this.heal((float) COPPER_HEALTH_PER_INGOT);
                if (!player.hasInfiniteMaterials()) {
                    held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            // Bronze ingot: pure heal, no upgrade count. Gated on not already being at full health.
            if (held.is(MythosMortalsItems.BRONZE_INGOT.get()) && player.getUUID().equals(this.ownerUUID)
                    && this.getHealth() < this.getMaxHealth()) {
                if (this.level().isClientSide()) {
                    return InteractionResult.SUCCESS;   // predicts the arm-swing for feeding the ingot
                }
                this.heal((float) BRONZE_HEAL_AMOUNT);
                if (this.level() instanceof ServerLevel server) {
                    Vec3 centre = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
                    ParticleFx.burst(server,
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COPPER_BLOCK.defaultBlockState()),
                            centre, BRONZE_HEAL_PARTICLES, BRONZE_HEAL_SPREAD, BRONZE_HEAL_PARTICLE_SPEED);
                }
                if (!player.hasInfiniteMaterials()) {
                    held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            // The two progression upgrades. The spyglass is not an upgrade item — it aims orders at
            // a target (see OwlOrderInput) and is only a crafting ingredient for the Athena Ocular.
            if (player.getUUID().equals(this.ownerUUID)) {
                if (held.is(MythosMortalsItems.ATHENA_OCULAR_UPGRADE.get()) && !this.hasAthenaSightUpgrade) {
                    if (this.level().isClientSide()) {
                        return InteractionResult.SUCCESS;   // predicts the arm-swing
                    }
                    this.hasAthenaSightUpgrade = true;
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (held.is(MythosMortalsItems.SONIC_SCREECH_UPGRADE.get()) && !this.hasSonicUpgrade) {
                    if (this.level().isClientSide()) {
                        return InteractionResult.SUCCESS;   // predicts the arm-swing
                    }
                    this.hasSonicUpgrade = true;
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Server-side: snap onto {@code player}'s arm and hold there (see {@link PerchGoal}). */
    private void startPerching(Player player) {
        this.entityData.set(PERCH_TARGET_ID, player.getId());
        this.setDiving(false);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        // A perched owl shares space with its host, so entity separation must be off: Entity#push
        // shoves both participants apart unless one has noPhysics, which would drag the host
        // sideways whenever they turn the camera.
        this.noPhysics = true;
        Vec3 spot = perchPosition(player);
        this.teleportTo(spot.x, spot.y, spot.z);
        // Claim the selected hotbar slot so the hand stays occupied (see PerchManager).
        PerchManager.begin(player, this, player.getInventory().getSelectedSlot());
    }

    /** Entry point for {@code DismountPerchServerPacket} ("right-click anywhere to dismount"): no
     * need to aim at the owl, whose hitbox lags the rendered arm. Re-validates server-side. */
    @Override
    public void tryStopPerching(ServerPlayer player) {
        if (this.isPerched() && !this.isPossessed() && player.getUUID().equals(this.ownerUUID)) {
            this.stopPerching();
        }
    }

    /** Releases the host's hotbar slot. Safe to call unperched and safe to call twice, so every
     * path that ends a perch calls it — including death and removal, which skip
     * {@link #stopPerching()}. */
    private void releasePerchedHand() {
        if (!this.level().isClientSide()
                && this.level().getEntity(this.getPerchTargetId()) instanceof Player host) {
            PerchManager.end(host);
        }
    }

    /** Server-side: leave the arm and go back to free companion flight. */
    private void stopPerching() {
        // Release the hand first: the target id is the only way back to the host.
        releasePerchedHand();
        this.entityData.set(PERCH_TARGET_ID, -1);
        this.noPhysics = false;
        // A small upward nudge so the take-off reads as launching off the arm rather than dropping.
        this.setDeltaMovement(0.0, 0.25, 0.0);
    }

    /** Complements the {@code noPhysics} flag in {@link #startPerching}: a perched owl also never
     * initiates pushes of its own. */
    @Override
    protected void pushEntities() {
        if (!this.isPerched()) {
            super.pushEntities();
        }
    }

    /** World position of {@code player}'s raised right hand. Their right side is
     * {@code (-cos(yaw), -sin(yaw))}: at yaw 0 they face +Z, so the right hand points toward -X. */
    private static Vec3 perchPosition(Player player) {
        // Places the hitbox from the same numbers the renderer uses (see OwlPerchPlacement). While
        // /deluxelib debug owlperch tunes them client-side the two drift apart, as expected.
        PerchPlacement placement = OwlPerchPlacement.current();
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
        double sin = Mth.sin(yawRad);
        double cos = Mth.cos(yawRad);
        double rightX = -cos, rightZ = -sin;
        double fwdX = -sin, fwdZ = cos;
        return player.position().add(
                rightX * placement.side() + fwdX * placement.forward(),
                placement.height(),
                rightZ * placement.side() + fwdZ * placement.forward());
    }

    /** Bonds this owl to {@code player}. Called once by the Owl Statue block right after spawning.
     * Arms {@link #pendingAwaken} so {@link #tick()} plays the grounded "awake" transition first. */
    public void bondTo(ServerPlayer player) {
        this.ownerUUID = player.getUUID();
        this.pendingAwaken = true;
    }

    /** The bonded owner, or {@code null} if unbonded, offline, or in another dimension. */
    private @Nullable Player findOwner() {
        UUID owner = this.ownerUUID;
        if (owner == null) {
            return null;
        }
        Player player = level().getPlayerByUUID(owner);
        return player != null && player.level() == level() ? player : null;
    }

    /** {@link #isAttackableForOwner} plus a distance test, so the owl doesn't launch at something
     * left over in the owner's damage history from a fight hundreds of blocks away. */
    private boolean canTargetForOwner(@Nullable LivingEntity candidate, @NotNull Player owner) {
        return isAttackableForOwner(candidate)
                && candidate.distanceToSqr(owner) <= DEFEND_ACQUIRE_RANGE * DEFEND_ACQUIRE_RANGE;
    }

    /**
     * What may be attacked, ignoring distance — split from {@link #canTargetForOwner} because a
     * spyglass order reaches past {@link #DEFEND_ACQUIRE_RANGE} but obeys the same exclusions.
     *
     * <p>Other players are valid targets: defending the owner means fighting whoever attacks them.
     * Only the owner, this owl, other owls and spectators are excluded.
     */
    private boolean isAttackableForOwner(@Nullable LivingEntity candidate) {
        return candidate != null
                && candidate.isAlive()
                && candidate != this
                && !(candidate instanceof OwlEntity)
                && !isOwner(candidate)
                && !(candidate instanceof Player player && player.isSpectator())
                && candidate.level() == level();
    }

    /** Whether {@code candidate} is this owl's bonded owner — never a valid target. */
    private boolean isOwner(@NotNull LivingEntity candidate) {
        return this.ownerUUID != null && this.ownerUUID.equals(candidate.getUUID());
    }

    /**
     * Server-side: send the owl after a target the owner picked with the spyglass
     * ({@code OwlOrderAttackServerPacket}). Re-validates owner, owl state and target rather than
     * trusting the client. An order overrides whatever the owl had picked automatically.
     *
     * @return {@code true} if the order was taken.
     */
    public boolean orderAttack(@NotNull ServerPlayer player, @Nullable LivingEntity target) {
        if (!player.getUUID().equals(this.ownerUUID)
                || !this.isAlive() || this.isPossessed() || this.isPerched() || this.isAwakening()
                || !isAttackableForOwner(target)) {
            return false;
        }
        this.targetWasOrdered = true;
        this.setTarget(target);
        return true;
    }

    /**
     * Copper shedding off the statue for as long as the awakening lasts.
     *
     * <p>Spread over time rather than fired as one burst: particle lifetime is fixed client-side and
     * vanilla's block shards only live a few ticks, so a single burst would be gone instantly.
     * Spawning a few every couple of ticks keeps the effect up for the animation's real duration.
     */
    private void emitAwakeningParticles() {
        if (this.tickCount % AWAKEN_PARTICLE_INTERVAL != 0 || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 centre = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
        ParticleFx.burst(server,
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COPPER_BLOCK.defaultBlockState()),
                centre, AWAKEN_SHARDS_PER_PUFF, AWAKEN_SPREAD, AWAKEN_SHARD_SPEED);
        ParticleFx.burst(server, ParticleTypes.ELECTRIC_SPARK,
                centre, AWAKEN_SPARKS_PER_PUFF, AWAKEN_SPREAD, AWAKEN_SPARK_SPEED);
    }

    /** Ticks between puffs during the awakening. Small enough that the stream reads as continuous,
     * large enough that it isn't a wall of particles. */
    private static final int AWAKEN_PARTICLE_INTERVAL = 2;
    private static final int AWAKEN_SHARDS_PER_PUFF = 6;
    private static final int AWAKEN_SPARKS_PER_PUFF = 3;
    private static final double AWAKEN_SPREAD = 0.35;
    private static final double AWAKEN_SHARD_SPEED = 0.08;
    private static final double AWAKEN_SPARK_SPEED = 0.04;

    /** True while the owl is grounded playing the one-shot "awake" transition (see {@link #bondTo}).
     * Every flight goal stands down until it clears. */
    public boolean isAwakening() { return this.pendingAwaken; }

    /** Entry point for the Athena's Sight keybind ({@code ActivatePossessionServerPacket}, which
     * picks the nearest eligible owl server-side). Starts a session if every gate passes, otherwise
     * reports why — see {@link #reportPossessionBlocked}. */
    @Override
    public boolean tryStartPossession(ServerPlayer player) {
        PossessionGate gate = checkPossessionGate(player);
        if (gate != PossessionGate.OK) {
            this.reportPossessionBlocked(player, gate);
            return false;
        }
        startPossession(player);
        return true;
    }

    /** Why {@link #checkPossessionGate} refused. {@link #SETUP} bundles the structural gates (not
     * bonded, not upgraded, not perched, already piloting, dead) that shouldn't come up in normal
     * play, so it stays silent; the rest get player feedback. */
    private enum PossessionGate { OK, COOLDOWN, HOSTILES_NEARBY, IN_COMBAT, SETUP }

    /** All activation gates in reporting order: ownership, combat, the rest of the setup, cooldown,
     * then nearby hostiles. Split out so the caller can report the specific reason. */
    private @NotNull PossessionGate checkPossessionGate(@NotNull ServerPlayer player) {
        // Ownership first, so a stranger's key press stays silent.
        if (!this.isAlive() || !player.getUUID().equals(this.ownerUUID)) {
            return PossessionGate.SETUP;
        }
        // Before the setup gates: a fighting owl is never perched, so it would otherwise fall into
        // the silent SETUP case and the key would look broken.
        if (this.isInCombat()) {
            return PossessionGate.IN_COMBAT;
        }
        boolean setupOk = this.hasAthenaSightUpgrade
                && this.isPerched()
                && !this.isPossessed()
                && !PossessionManager.isPossessing(player);
        if (!setupOk) {
            return PossessionGate.SETUP;
        }
        if (this.possessionCooldownTicks > 0) {
            return PossessionGate.COOLDOWN;
        }
        if (hasHostilesNearby(player)) {
            return PossessionGate.HOSTILES_NEARBY;
        }
        return PossessionGate.OK;
    }

    /** Action-bar feedback for the two gates worth telling the player about. */
    private void reportPossessionBlocked(@NotNull ServerPlayer player, @NotNull PossessionGate gate) {
        switch (gate) {
            case COOLDOWN -> {
                double secondsLeft = this.possessionCooldownTicks / 20.0;
                sendActionBar(player, Component.literal(
                                String.format("Athena's Sight on cooldown: %.1fs", secondsLeft))
                        .withStyle(ChatFormatting.RED));
            }
            case HOSTILES_NEARBY -> sendActionBar(player, Component.literal(
                            "Enemies are too close to deploy Athena's Sight")
                    .withStyle(ChatFormatting.RED));
            case IN_COMBAT -> sendActionBar(player, Component.literal(
                            "The owl is fighting — recall it before deploying Athena's Sight")
                    .withStyle(ChatFormatting.RED));
            case SETUP, OK -> {}
        }
    }

    /** Sends to the action bar rather than chat, through the packet directly:
     * {@code Player#displayClientMessage}'s signature has moved around across MC versions. */
    private static void sendActionBar(@NotNull ServerPlayer player, @NotNull Component message) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(message));
    }

    /** You must be safe before deploying recon: blocks activation while any {@link Enemy} is alive
     * within {@link #SAFETY_RADIUS} of the player. Owls are excluded — {@code OwlEntity} inherits
     * {@code Enemy} from its flying-mob base, so the companion itself would otherwise count as a
     * nearby hostile and permanently block activation. */
    private static boolean hasHostilesNearby(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(SAFETY_RADIUS);
        return !player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e instanceof Enemy && !(e instanceof OwlEntity)).isEmpty();
    }

    /** Radius of {@link #clearAggro}'s area scan — wider than {@link #SAFETY_RADIUS}, since a mob
     * still closing in may be well outside melee range. */
    private static final double DEAGGRO_RADIUS = 48.0;
    /** How often {@link #clearAggro} runs during the cleanup window. */
    private static final int AGGRO_CHECK_INTERVAL_TICKS = 20;
    /** How long a {@link #grudgeMobs} entry is kept before giving up on it — long enough for a slow
     * mob to close a realistic distance, short enough that the map can't grow all session. */
    private static final int GRUDGE_MAX_AGE_TICKS = 20 * 60;

    /**
     * Ticks left in the post-possession aggro cleanup, armed by {@link #stopPossession()}.
     *
     * <p>Scoped to a window rather than running whenever the owl isn't piloted: "not piloted" is its
     * normal fighting state, so an unbounded cleanup would make every mob it attacks forget it a
     * second later and leave the owl untouchable.
     *
     * <p>Matched to {@link #GRUDGE_MAX_AGE_TICKS} so the window ends exactly when the bookkeeping it
     * drives ages out.
     */
    private static final int AGGRO_CLEAR_WINDOW_TICKS = GRUDGE_MAX_AGE_TICKS;

    /** Counts {@link #AGGRO_CLEAR_WINDOW_TICKS} down after a possession ends; 0 = no clean-up owed. */
    private int aggroClearTicks;

    /** Records a mob the owl just hit, so {@link #clearAggro} can find it later by identity instead
     * of rediscovering it spatially. Called from both attacks' {@code HitWindow.onHit}. */
    private void recordGrudge(@NotNull LivingEntity target) {
        // Only a pilot's fights leave a mess worth cleaning up. Both attacks are shared with the
        // defence AI, so without this the owl would be made to forget mobs it is actively fighting.
        if (!this.isPossessed()) {
            return;
        }
        if (target instanceof Mob mob) {
            this.grudgeMobs.put(mob, this.tickCount);
            if (DEBUG_AGGRO) {
                AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} RECORD mob={}(id={}) at {}",
                        this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId(),
                        mob.position());
            }
        }
    }

    /**
     * Force-stops every running goal in {@code mob}'s target-selector. {@code setTarget(null)} alone
     * is not enough: a targeting goal stays "running" until its own {@code canContinueToUse()} says
     * otherwise, and can restore the target on its very next tick. Stopping the {@link WrappedGoal}
     * clears its {@code isRunning} flag, so the goal must re-satisfy {@code canUse()} from scratch —
     * which for a hurt-based goal means an actual new hit.
     */
    private void forceStopTargeting(@NotNull Mob mob) {
        for (WrappedGoal wrapped : mob.targetSelector.getAvailableGoals()) {
            if (wrapped.isRunning()) {
                if (DEBUG_AGGRO) {
                    AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} FORCE-STOP goal={} on mob={}(id={})",
                            this.tickCount, this.getId(), wrapped.getGoal().getClass().getSimpleName(),
                            mob.getClass().getSimpleName(), mob.getId());
                }
                wrapped.stop();
            }
        }
    }

    /**
     * Clears hostile mobs' memory of this owl, in two passes run once a second
     * ({@link #AGGRO_CHECK_INTERVAL_TICKS}) during the post-possession window
     * ({@link #AGGRO_CLEAR_WINDOW_TICKS}), paused whenever the owl has a target of its own.
     *
     * <p>Both attacks credit damage to the owl rather than the pilot, so hit mobs chase it — and via
     * vanilla's {@code setAlertOthers()}, potentially their whole group. That is the intended read
     * while the owl is flying and fighting, but not once it is back on the player's arm. Each pass
     * clears {@code getTarget()} and {@code getLastHurtByMob()}, or {@code HurtByTargetGoal} would
     * re-trigger on its next evaluation.
     *
     * <ol>
     *   <li>{@link #grudgeMobs} — mobs the owl hit, matched by identity, so neither distance nor how
     *   long they take to catch up matters. Entries that never resolve are dropped after
     *   {@link #GRUDGE_MAX_AGE_TICKS}.</li>
     *   <li>A {@link #DEAGGRO_RADIUS} area scan — the fallback for mobs pulled in by
     *   {@code setAlertOthers()}, which fires no event to hook, so it can only be a spatial guess.</li>
     * </ol>
     */
    private void clearAggro() {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        this.grudgeMobs.entrySet().removeIf(entry -> {
            Mob mob = entry.getKey();
            if (!mob.isAlive() || mob.isRemoved()) {
                if (DEBUG_AGGRO) {
                    AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} GRUDGE DROP (gone) mob={}(id={}) alive={} removed={}",
                            this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId(),
                            mob.isAlive(), mob.isRemoved());
                }
                return true;
            }
            if (DEBUG_AGGRO) {
                AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} GRUDGE CLEAR mob={}(id={}, dist={}) before: target={} lastHurtByMob={} age={}",
                        this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId(),
                        String.format("%.1f", Math.sqrt(mob.distanceToSqr(this))),
                        describe(mob.getTarget()), describe(mob.getLastHurtByMob()),
                        this.tickCount - entry.getValue());
            }
            // Unconditional, not gated on getTarget() == this: a mob that re-acquires the owl
            // between checks would otherwise age out while still chasing it. Stripping on every pass
            // is a harmless no-op when it is already null.
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            this.forceStopTargeting(mob);
            if (DEBUG_AGGRO) {
                AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} GRUDGE CLEAR mob={}(id={}) after: target={} lastHurtByMob={}",
                        this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId(),
                        describe(mob.getTarget()), describe(mob.getLastHurtByMob()));
            }
            boolean expired = this.tickCount - entry.getValue() > GRUDGE_MAX_AGE_TICKS;
            if (expired && DEBUG_AGGRO) {
                AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} GRUDGE EXPIRE (gave up) mob={}(id={})",
                        this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId());
            }
            return expired;
        });

        AABB area = this.getBoundingBox().inflate(DEAGGRO_RADIUS);
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, area, m -> m.getTarget() == this)) {
            if (DEBUG_AGGRO) {
                AGGRO_LOG.info("[DlxOwlAggro] tick={} owl={} AREA CLEAR mob={}(id={}, dist={}) — NOT in grudgeMobs={}",
                        this.tickCount, this.getId(), mob.getClass().getSimpleName(), mob.getId(),
                        String.format("%.1f", Math.sqrt(mob.distanceToSqr(this))),
                        !this.grudgeMobs.containsKey(mob));
            }
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            this.forceStopTargeting(mob);
        }
    }

    private void startPossession(ServerPlayer player) {
        // checkPossessionGate requires isPerched(), so this always launches the owl off the arm.
        this.stopPerching();
        this.controller = player;
        this.entityData.set(CONTROLLER_ID, player.getId());
        this.possessionTicksRemaining = POSSESSION_DURATION_TICKS;
        this.possessionAnchor = player.position();
        this.controlInput = Input.EMPTY;
        this.controlYaw = player.getYRot();
        this.controlPitch = player.getXRot();
        this.controlAttackCooldown = 0;
        this.controlReturnTicks = 0;
        this.controlSonicTicks = 0;
        this.controlSonicCooldown = 0;
        this.setScreeching(false);
        PossessionManager.begin(player, this);
        // Owls see in the dark. Effectively infinite duration: every possession-ending path
        // (stopPossession, die, remove) removes it explicitly rather than letting it expire.
        applyOwlNightVision(player);
        // Airborne setup and per-tick control live in PossessionGoal, which the selector starts as
        // soon as isPossessed() flips true. It has to run as a goal with AI on: setNoAi stops
        // travel() from applying the velocity and rotation we set.
    }

    /**
     * The pilot's raw 3D aim (unit vector), from {@link #controlYaw}/{@link #controlPitch} rather
     * than this entity's own rotation.
     *
     * <p>Vanilla's default {@code LookControl.tick()} runs right after the goal selector — so right
     * after {@link PossessionGoal#tick()} sets {@code xRot} — and resets {@code xRot} toward 0
     * whenever nothing calls {@code lookAt(...)}, which nothing here does. So
     * {@code getXRot()}/{@code getViewVector()} cannot be trusted for aim while possessed; reading
     * them made the sonic beam always fire level. Movement reads the same raw fields, so aim, flight
     * direction and beam can never disagree.
     */
    private Vec3 pilotLookVector() {
        float yawRad = this.controlYaw * ((float) Math.PI / 180.0F);
        float pitchRad = this.controlPitch * ((float) Math.PI / 180.0F);
        float cosP = Mth.cos(pitchRad);
        return new Vec3(-Mth.sin(yawRad) * cosP, -Mth.sin(pitchRad), Mth.cos(yawRad) * cosP);
    }

    /**
     * The one aim source for the screech, shared by its {@code HitWindow}'s {@code facing()} and its
     * knockback direction.
     *
     * <p>Piloted, that is {@link #pilotLookVector()}. Fired by the owl's own defence there is no
     * pilot and {@code controlYaw}/{@code controlPitch} hold whatever the last session left behind,
     * so the direction is computed from the owl's eyes to the middle of its target instead. Falls
     * back to the entity's own look only if there is no target, which the callers make unreachable.
     */
    private Vec3 sonicAimVector() {
        if (this.isPossessed()) {
            return pilotLookVector();
        }
        LivingEntity target = getTarget();
        if (target != null) {
            Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                    .subtract(this.getEyePosition());
            if (dir.lengthSqr() > 1.0E-6) {
                return dir.normalize();
            }
        }
        return this.getLookAngle();
    }

    private static void applyOwlNightVision(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }

    private static void removeOwlNightVision(ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }

    /** Ends a possession: lands the owl back on the player's arm and starts the cooldown. Safe to
     * call when the controller is gone (logout/death) — it just resumes free flight instead. */
    @Override
    public void stopPossession() {
        ServerPlayer player = this.controller;
        this.controller = null;
        this.entityData.set(CONTROLLER_ID, -1);
        this.possessionTicksRemaining = 0;
        this.possessionCooldownTicks = POST_POSSESSION_COOLDOWN_TICKS;
        this.possessionAnchor = null;
        this.controlInput = Input.EMPTY;
        this.controlAttackCooldown = 0;
        this.controlReturnTicks = 0;
        this.controlSonicTicks = 0;
        this.controlSonicCooldown = 0;
        this.setDiving(false);
        this.setScreeching(false);
        // One pass now for whoever is already close, then arm the window for stragglers. Outside it
        // the owl is fought normally — see AGGRO_CLEAR_WINDOW_TICKS.
        this.aggroClearTicks = AGGRO_CLEAR_WINDOW_TICKS;
        this.clearAggro();
        if (player != null) {
            PossessionManager.end(player);
            removeOwlNightVision(player);
        }
        if (!this.isAlive()) {
            return;     // dying owl: leave the corpse to the base death handling
        }
        if (player != null && player.isAlive() && !player.isRemoved()) {
            // PerchGoal picks this up next tick, so it reads as landing rather than freezing.
            this.startPerching(player);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.setFlying(true);
        }
    }

    /**
     * Drives the owl while a player pilots it, as an exclusive MOVE+LOOK goal at priority 0. Mirrors
     * the controller's aim onto the owl (the owl is the camera) and flies elytra-style: W is the
     * only movement key, all steering is the mouse.
     */
    private class PossessionGoal extends Goal {
        PossessionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isPossessed();
        }

        @Override
        public boolean canContinueToUse() {
            return isPossessed();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            setNoGravity(true);
            setFlying(true);
            setTakingOff(false);
            setLanding(false);
            setDiving(false);
            setTarget(null);
            if (onGround()) {
                setDeltaMovement(0.0, 0.3, 0.0);   // little hop so it lifts clear of the ground
            }
        }

        @Override
        public void tick() {
            ServerPlayer player = OwlEntity.this.controller;
            if (player == null || player.isRemoved() || !player.isAlive() || player.level() != level()) {
                stopPossession();
                return;
            }
            if (--OwlEntity.this.possessionTicksRemaining <= 0) {
                stopPossession();
                return;
            }
            // Attack state machine: dive → automatic pull-up → free control. The screech's timers
            // live in OwlEntity.tick() instead, since the defence AI fires it with no pilot.
            if (OwlEntity.this.controlAttackCooldown > 0) {
                if (--OwlEntity.this.controlAttackCooldown == 0) {
                    setDiving(false);   // end the dive_attack swing / HitWindow
                    OwlEntity.this.controlReturnTicks = POSSESSED_RETURN_TICKS;
                    animator.play(animator.getByName("dive_attack_return"));   // pull-up anim
                }
            } else if (OwlEntity.this.controlReturnTicks > 0) {
                OwlEntity.this.controlReturnTicks--;
            }

            // Aim comes from the client: the server player's rotation isn't synced while piloting.
            float yaw = OwlEntity.this.controlYaw;
            float pitch = OwlEntity.this.controlPitch;
            setYRot(yaw);
            setXRot(pitch);
            OwlEntity.this.yHeadRot = yaw;
            OwlEntity.this.yBodyRot = yaw;

            // Steerable aim — see pilotLookVector() for why it isn't read back off getXRot().
            Vec3 look = pilotLookVector();
            double lookX = look.x, lookY = look.y, lookZ = look.z;

            Vec3 desired;
            double accel;
            if (OwlEntity.this.controlSonicTicks > 0) {
                // Screech: brake to a hover, still fully steerable. Drifting through the release
                // tick would smear the beam across the sky.
                desired = Vec3.ZERO;
                accel = SONIC_BRAKE_ACCEL;
            } else if (OwlEntity.this.controlAttackCooldown > 0) {
                // Auto-dive: a fast downward-biased swoop, no W needed, that carries the owl onto
                // the target so the dive_attack HitWindow lands.
                desired = new Vec3(lookX, lookY - POSSESSED_DIVE_DOWN_BIAS, lookZ)
                        .normalize().scale(POSSESSED_DIVE_SPEED);
                accel = POSSESSED_DIVE_ACCEL;
            } else if (OwlEntity.this.controlReturnTicks > 0) {
                // Pull-up: climb out with a little forward drift along the aim's heading. Pitch is
                // dropped on purpose — the climb speed is its own term.
                Vec3 horiz = new Vec3(look.x, 0.0, look.z);
                Vec3 horizDir = horiz.lengthSqr() > 1.0E-6 ? horiz.normalize() : new Vec3(0.0, 0.0, 1.0);
                desired = new Vec3(horizDir.x * POSSESSED_RETURN_FWD, POSSESSED_RETURN_CLIMB, horizDir.z * POSSESSED_RETURN_FWD);
                accel = POSSESSED_RETURN_ACCEL;
            } else {
                // Cruise: W thrusts along the look direction, the mouse does all the steering.
                double thrust = OwlEntity.this.controlInput.forward() ? 1.0 : 0.0;
                desired = new Vec3(lookX, lookY, lookZ).scale(thrust * POSSESSED_SPEED);
                accel = POSSESSED_ACCEL;
            }

            Vec3 current = getDeltaMovement();
            Vec3 next = current.add(desired.subtract(current).scale(accel));
            setDeltaMovement(clampToLeash(next));
        }
    }

    /** Keeps the recon flight inside the client's entity-tracking range: strips the outward part of
     * the velocity past {@link #POSSESSED_MAX_RADIUS} horizontally and blocks climbing past
     * {@link #POSSESSED_MAX_CLIMB} above the anchor. */
    private Vec3 clampToLeash(Vec3 vel) {
        if (this.possessionAnchor == null) {
            return vel;
        }
        double dx = this.getX() - this.possessionAnchor.x;
        double dz = this.getZ() - this.possessionAnchor.z;
        double distSq = dx * dx + dz * dz;
        if (distSq > POSSESSED_MAX_RADIUS * POSSESSED_MAX_RADIUS) {
            double dist = Math.sqrt(distSq);
            double ox = dx / dist;
            double oz = dz / dist;
            double radial = vel.x * ox + vel.z * oz;
            if (radial > 0.0) {
                vel = new Vec3(vel.x - ox * radial, vel.y, vel.z - oz * radial);
            }
        }
        if (this.getY() > this.possessionAnchor.y + POSSESSED_MAX_CLIMB && vel.y > 0.0) {
            vel = new Vec3(vel.x, 0.0, vel.z);
        }
        return vel;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        // Release the controller before the corpse logic runs, or the player stays flagged as
        // possessing and their body invulnerable. No return teleport — the owl is dying.
        if (!this.level().isClientSide() && this.isPossessed()) {
            ServerPlayer player = this.controller;
            this.controller = null;
            this.entityData.set(CONTROLLER_ID, -1);
            this.possessionTicksRemaining = 0;
            this.possessionAnchor = null;
            if (player != null) {
                PossessionManager.end(player);
                removeOwlNightVision(player);
            }
        }
        // A dying owl never reaches stopPerching(), so the hand is released here too.
        releasePerchedHand();
        super.die(source);
    }

    /**
     * Releases the controller when the owl is removed without dying ({@code kill}/{@code discard}),
     * or their body would stay invulnerable with no owl left to end the session.
     *
     * <p>Not a catch-all: chunk unload and shutdown bypass this override entirely, via the
     * {@code final Entity.setRemoved} → {@code onRemoval} path. Those are covered by
     * {@link net.darkblade.deluxelib.entity.possession.PossessionEvents}, which sweeps the
     * {@code static} {@link net.darkblade.deluxelib.entity.possession.PossessionManager} map — an
     * entry left there would outlive the world and keep granting the player damage immunity.
     */
    @Override
    public void remove(net.minecraft.world.entity.Entity.@NotNull RemovalReason reason) {
        if (!this.level().isClientSide() && this.controller != null) {
            PossessionManager.end(this.controller);
            removeOwlNightVision(this.controller);
            this.controller = null;
        }
        releasePerchedHand();
        super.remove(reason);
    }

    // -----------------------------------------------------------------------
    // Goals — always airborne (see tick()): no takeoff/landing/ground-wander goals are registered,
    // so every AbstractFlyingEntity config hook tied to that state machine (flight altitude, ground
    // rest ticks, landing speeds, takeoff tilt) is dead here and deliberately left unoverridden.
    // -----------------------------------------------------------------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Possession and perching both claim MOVE+LOOK at priority 0, so they beat everything below.
        this.goalSelector.addGoal(0, new PossessionGoal());
        this.goalSelector.addGoal(0, new PerchGoal());
        // Defending outranks following: while there's a target the owl leaves the owner's side.
        this.goalSelector.addGoal(1, new DefendOwnerGoal());
        this.goalSelector.addGoal(2, new FollowOwnerGoal());

        // TARGET-flagged, so they run alongside the movement goals rather than competing with them.
        this.targetSelector.addGoal(1, new OwnerHurtByGoal());
        this.targetSelector.addGoal(2, new OwnerHurtGoal());
    }

    /**
     * Holds the owl on its owner's raised right arm by writing its position directly every tick: the
     * arm is a moving target, and any acceleration-based follow would visibly lag behind it. Body
     * yaw is matched to the owner's so it reads as riding on them rather than hovering nearby.
     */
    private class PerchGoal extends Goal {
        PerchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isPerched() && !isPossessed() && findPerchTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void stop() {
            // The owner may have logged out or died — don't leave the synced flag stuck on.
            if (isPerched() && findPerchTarget() == null) {
                stopPerching();
            }
        }

        @Override
        public void tick() {
            Player owner = findPerchTarget();
            if (owner == null) {
                return;
            }
            Vec3 spot = perchPosition(owner);
            setDeltaMovement(Vec3.ZERO);
            setPos(spot.x, spot.y, spot.z);

            float yaw = owner.getYRot();
            setYRot(yaw);
            OwlEntity.this.yBodyRot = yaw;
            OwlEntity.this.yHeadRot = yaw;
        }

        /** The player named by {@code PERCH_TARGET_ID}, or {@code null} if they're gone. */
        private @Nullable Player findPerchTarget() {
            int id = getPerchTargetId();
            if (id == -1) {
                return null;
            }
            return level().getEntity(id) instanceof Player player && player.isAlive() ? player : null;
        }
    }

    /** Acquires whoever just hurt the owner. Vanilla's {@code OwnerHurtByTargetGoal} is typed to
     * {@code TamableAnimal}, which this owl is not, so it gets its own. */
    private class OwnerHurtByGoal extends Goal {
        OwnerHurtByGoal() {
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isPossessed() || isPerched() || isAwakening()) {
                return false;
            }
            // Don't pull the owl off a fight it is committed to: getLastHurtByMob() lingers for
            // seconds and would keep re-electing the same attacker.
            LivingEntity current = getTarget();
            if (current != null && current.isAlive()) {
                return false;
            }
            Player owner = findOwner();
            return owner != null && canTargetForOwner(owner.getLastHurtByMob(), owner);
        }

        @Override
        public void start() {
            Player owner = findOwner();
            if (owner != null) {
                setTarget(owner.getLastHurtByMob());
            }
            super.start();
        }
    }

    /** Acquires whoever the owner is attacking, so the owl joins fights the owner picks rather than
     * only ones picked for them. Same shape and same reasoning as {@link OwnerHurtByGoal}. */
    private class OwnerHurtGoal extends Goal {
        OwnerHurtGoal() {
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isPossessed() || isPerched() || isAwakening()) {
                return false;
            }
            LivingEntity current = getTarget();
            if (current != null && current.isAlive()) {
                return false;
            }
            Player owner = findOwner();
            return owner != null && canTargetForOwner(owner.getLastHurtMob(), owner);
        }

        @Override
        public void start() {
            Player owner = findOwner();
            if (owner != null) {
                setTarget(owner.getLastHurtMob());
            }
            super.start();
        }
    }

    /**
     * The owl's melee defence: an aerial dive-bomb run against its current target. Structure ported
     * from {@code ArpyEntity.DiveAttackGoal} — climb to an offset point above the target
     * (REPOSITION), turn to face it (ALIGN), commit to a fast descent (DIVE), climb back out
     * (PULLUP), then go round again from a fresh angle. Direct velocity control with navigation
     * stopped, like every other goal here.
     *
     * <p>The damage is not applied here: {@link #setDiving(boolean)} plays {@code dive_attack} and
     * its {@link HitWindow} does the rest, the same path the possessed dive uses. This goal
     * deliberately leaves {@code controlAttackCooldown} alone — that field only ticks down while a
     * pilot is in control, so borrowing it would leave {@code isDiving()} stuck true.
     */
    private class DefendOwnerGoal extends Goal {
        private enum Phase { REPOSITION, ALIGN, DIVE, PULLUP }

        private static final double ATTACK_ALTITUDE = 6.0;    // blocks above the target to dive from
        private static final double REPOS_RADIUS = 7.0;       // horizontal offset → an angled dive
        private static final double CRUISE_SPEED = 0.45;
        private static final double ALIGN_SPEED = 0.18;
        private static final double ALIGN_YAW_THRESHOLD = 22.0;
        private static final double DIVE_SPEED = 0.70;
        private static final double CLIMB_SPEED = 0.45;
        private static final double PULLUP_CLEARANCE = 1.6;   // never sink below this above ground
        private static final int MAX_DIVE_TICKS = 45;         // safety: force pull-up if it drags

        private Phase phase = Phase.REPOSITION;
        private double reposX, reposY, reposZ;
        private int diveTicks;

        DefendOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isPossessed() && !isPerched() && !isAwakening() && hasDefendableTarget();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            setNoGravity(true);
            setFlying(true);
            setDiving(false);
            this.phase = Phase.REPOSITION;
            pickReposition();
        }

        @Override
        public void stop() {
            setDiving(false);
            setTarget(null);
            OwlEntity.this.targetWasOrdered = false;
            this.phase = Phase.REPOSITION;
            // Bleed the run off rather than stopping dead, so FollowOwnerGoal picks up a moving bird.
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x * 0.5, Math.max(m.y, 0.0), m.z * 0.5);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            // A screech in progress owns the owl: brake to a hover so the ray doesn't smear across
            // the sky, exactly as the piloted screech does. The phase machine resumes after it.
            if (OwlEntity.this.controlSonicTicks > 0) {
                Vec3 cur = getDeltaMovement();
                setDeltaMovement(cur.subtract(cur.scale(SONIC_BRAKE_ACCEL)));
                return;
            }
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
            double ang = OwlEntity.this.random.nextDouble() * Math.PI * 2.0;
            this.reposX = baseX + Math.cos(ang) * REPOS_RADIUS;
            this.reposZ = baseZ + Math.sin(ang) * REPOS_RADIUS;
            this.reposY = baseY + ATTACK_ALTITUDE;
        }

        private void tickReposition(@NotNull LivingEntity target) {
            steerTowards(new Vec3(this.reposX, this.reposY, this.reposZ), CRUISE_SPEED, 0.25);
            double dx = this.reposX - getX();
            double dz = this.reposZ - getZ();
            boolean inPlace = dx * dx + dz * dz < 4.0;
            boolean highEnough = getY() >= target.getY() + ATTACK_ALTITUDE * 0.7;
            if (inPlace && highEnough) {
                this.phase = Phase.ALIGN;
            }
        }

        /** Turn to face the target before committing. {@code isDiving()} stays false until already
         * pointed at it, so the dive pose only appears for the actual fall, not the mid-air pivot. */
        private void tickAlign(@NotNull LivingEntity target) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            faceHeading(dx, dz, 14.0F);

            float yawRad = (float) Math.toRadians(getYRot());
            double dy = Mth.clamp(this.reposY - getY(), -1.0, 1.0);
            Vec3 desired = new Vec3(-Math.sin(yawRad) * ALIGN_SPEED, dy * 0.1, Math.cos(yawRad) * ALIGN_SPEED);
            Vec3 cur = getDeltaMovement();
            setDeltaMovement(cur.add(desired.subtract(cur).scale(0.25)));

            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            if (Math.abs(Mth.wrapDegrees(targetYaw - getYRot())) >= ALIGN_YAW_THRESHOLD) {
                return;   // still turning
            }
            // Lined up. If the screech is ready, shout from here instead of diving — its cooldown
            // is what makes the two alternate rather than one replacing the other.
            if (canScreechAt(target)) {
                performSonicAttack();
                pickReposition();
                this.phase = Phase.REPOSITION;
                return;
            }
            this.phase = Phase.DIVE;
            this.diveTicks = 0;
            setDiving(true);   // arms the dive_attack HitWindow
        }

        /** Whether a screech is both unlocked/ready and worth taking from here — {@code target}
         * must be inside the beam's own reach, or the shout would visibly fire at nothing. */
        private boolean canScreechAt(@NotNull LivingEntity target) {
            return OwlEntity.this.hasSonicUpgrade
                    && OwlEntity.this.controlSonicCooldown <= 0
                    && OwlEntity.this.controlSonicTicks <= 0
                    && distanceToSqr(target) <= SONIC_RANGE * SONIC_RANGE;
        }

        private void tickDive(@NotNull LivingEntity target) {
            this.diveTicks++;
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
            steerTowards(aim, DIVE_SPEED, 0.5);

            double groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) getX(), (int) getZ());
            boolean tooLow = getY() - groundY < PULLUP_CLEARANCE;
            boolean reachedTarget = getY() <= target.getY() + 0.5;
            if (tooLow || reachedTarget || this.diveTicks > MAX_DIVE_TICKS) {
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
            faceHeading(dx, dz, 14.0F);

            if (getY() >= target.getY() + ATTACK_ALTITUDE * 0.8) {
                pickReposition();
                this.phase = Phase.REPOSITION;
            }
        }
    }

    /** True while the owl has something to attack. Keeps possession out of fights (see
     * {@link #checkPossessionGate}); clears itself when {@code DefendOwnerGoal} drops the target. */
    private boolean isInCombat() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive();
    }

    /** Whether there is a live target worth defending against: alive, and still close enough to the
     * OWNER to count as a threat to them (see {@link #DEFEND_LEASH_RADIUS}). */
    private boolean hasDefendableTarget() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        Player owner = findOwner();
        if (owner == null) {
            return false;
        }
        double leash = this.targetWasOrdered ? ORDER_LEASH_RADIUS : DEFEND_LEASH_RADIUS;
        return target.distanceToSqr(owner) <= leash * leash;
    }

    /**
     * Default behaviour once awake and un-possessed: stays near the bonded owner under direct
     * velocity control rather than pathfinding. While the owner moves it holds a point just off
     * their side and above head height; once they have stood still for {@link #ORBIT_AFTER_TICKS} it
     * slowly orbits them instead of freezing in place.
     */
    private class FollowOwnerGoal extends Goal {
        private static final double FOLLOW_SPEED = 0.5;
        /** Gain (kp) and damping (kd) of the PD controller in {@link #tick()}. Tune them as a pair:
         * raising the gain or lowering the damping brings back the ringing they were chosen to avoid
         * (they keep the recurrence's eigenvalues real, i.e. non-oscillating). */
        private static final double FOLLOW_POS_GAIN = 0.04;
        private static final double FOLLOW_DAMPING = 0.4;
        private static final double FOLLOW_HEIGHT = 2.4;
        private static final double FOLLOW_SIDE = 1.3;
        private static final int ORBIT_AFTER_TICKS = 100;   // ~5 s stationary before orbiting starts
        private static final double ORBIT_RADIUS = 1.8;
        private static final double ORBIT_HEIGHT = 2.4;
        private static final float ORBIT_ANGULAR_SPEED = 3.0F;   // degrees/tick
        private static final double IDLE_MOVE_THRESHOLD_SQ = 0.0025;

        private int idleTicks;
        private float orbitAngle;
        private @Nullable Vec3 lastPlayerPos;

        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isPossessed() && !isPerched() && !isAwakening() && findOwner() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !isPossessed() && !isPerched() && !isAwakening() && findOwner() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            this.idleTicks = 0;
            this.lastPlayerPos = null;
        }

        @Override
        public void tick() {
            Player player = findOwner();
            if (player == null) {
                return;
            }

            Vec3 curPos = player.position();
            boolean moved = this.lastPlayerPos == null
                    || curPos.subtract(this.lastPlayerPos).horizontalDistanceSqr() > IDLE_MOVE_THRESHOLD_SQ;
            this.lastPlayerPos = curPos;
            this.idleTicks = moved ? 0 : this.idleTicks + 1;

            Vec3 target;
            if (this.idleTicks >= ORBIT_AFTER_TICKS) {
                this.orbitAngle += ORBIT_ANGULAR_SPEED;
                double rad = Math.toRadians(this.orbitAngle);
                target = curPos.add(Math.cos(rad) * ORBIT_RADIUS, ORBIT_HEIGHT, Math.sin(rad) * ORBIT_RADIUS);
            } else {
                // A point off the player's side, above head height.
                float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
                double sideX = Math.cos(yawRad) * FOLLOW_SIDE;
                double sideZ = Math.sin(yawRad) * FOLLOW_SIDE;
                target = curPos.add(sideX, FOLLOW_HEIGHT, sideZ);
            }

            // Critically-damped PD controller: acceleration = kp*error - kd*velocity. Feeding a
            // distance-proportional target velocity through a second smoothing pass instead cascades
            // two lags into a lightly-damped 2nd-order system, which overshoots and ping-pongs on
            // any large initial gap (spawning grounded, un-perching).
            Vec3 error = target.subtract(position());
            Vec3 velocity = getDeltaMovement();
            Vec3 accel = error.scale(FOLLOW_POS_GAIN).subtract(velocity.scale(FOLLOW_DAMPING));
            Vec3 newVelocity = velocity.add(accel);
            double speed = newVelocity.length();
            if (speed > FOLLOW_SPEED) {
                newVelocity = newVelocity.scale(FOLLOW_SPEED / speed);
            }
            setDeltaMovement(newVelocity);

            Vec3 heading = getDeltaMovement();
            if (heading.horizontalDistanceSqr() > 1.0E-4) {
                faceHeading(heading.x, heading.z);
            } else {
                faceHeading(curPos.x - getX(), curPos.z - getZ());
            }
        }


        private void faceHeading(double dx, double dz) {
            if (dx * dx + dz * dz < 1.0E-4) {
                return;
            }
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float delta = Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -10.0F, 10.0F);
            setYRot(getYRot() + delta);
            OwlEntity.this.yBodyRot = getYRot();
        }
    }

    // -----------------------------------------------------------------------
    // Animatable — CopperOwlModel geometry, OwlAnimation keyframes
    // -----------------------------------------------------------------------
    @Override
    public @NotNull MobAnimator<OwlEntity> animator() {
        return this.animator;
    }

    @Override
    public void registerAnimations() {
        // The owl's own Blockbench export (see CopperOwlModel), not the borrowed arpy set.
        StandardAnimation idleFly = new StandardAnimation("idle_fly",
                new AnimSource(() -> OwlAnimation.FLY_IDLE), Loop.REPEATING, 0, 3, 0.7246F);
        StandardAnimation flySprint = new StandardAnimation("fly_sprint",
                new AnimSource(() -> OwlAnimation.FLY), Loop.REPEATING, 0, 2, 0.5F);
        StandardAnimation diveAttack = new StandardAnimation("dive_attack",
                new AnimSource(() -> OwlAnimation.DIVE_ATTACK), Loop.REPEATING, 0, 0, 0.517F);
        StandardAnimation diveReturn = new StandardAnimation("dive_attack_return",
                new AnimSource(() -> OwlAnimation.DIVE_RECOVER), Loop.PLAY_ONCE, 0, 0, 0.2433F);
        // Two-stage death: FALL loops while the corpse drops, then chains into HIT on landing. Any
        // death clip must use this model's bone names — the arpy's animate bones like "torso", which
        // CopperOwlModel lacks, and crashed the client with "Cannot animate torso" on every death.
        StandardAnimation deathFalling = new StandardAnimation("death_falling",
                new AnimSource(() -> OwlAnimation.FALL), Loop.REPEATING, 0, 0, 0.5F);
        StandardAnimation hitGround = new StandardAnimation("hit_ground",
                new AnimSource(() -> OwlAnimation.HIT), Loop.PLAY_ONCE, 0, 0, 1.0F);
        // One-shot "waking up" transition after spawning from the Owl Statue (see bondTo()/tick()):
        // grounded and folded → open, then the owl takes flight. Triggered imperatively rather than
        // by play condition, so it runs exactly once instead of restarting every eligible tick.
        StandardAnimation awake = new StandardAnimation("awake",
                new AnimSource(() -> OwlAnimation.AWAKE), Loop.PLAY_ONCE, 0, 0, 2.7917F);
        // Sonic screech — triggered imperatively from performSonicAttack(). Length is
        // SONIC_TOTAL_TICKS in seconds: clip and counter must agree, or the clip is cut short (or the
        // owl hangs braked after it visually ends).
        // TODO: swap in the owl's own SONIC_SCREECH clip once exported from Blockbench (bone names
        // must match CopperOwlModel's). DIVE_ATTACK is a stand-in so the mechanic is testable.
        StandardAnimation sonic = new StandardAnimation("sonic_screech",
                new AnimSource(() -> OwlAnimation.DIVE_ATTACK), Loop.PLAY_ONCE, 0, 0, SONIC_TOTAL_TICKS / 20.0F);
        // Pin the tick count: BaseAnimation's seconds→ticks conversion truncates, so without this
        // the clip and the state counter drift a tick apart.
        sonic.setDurationTicks(SONIC_TOTAL_TICKS);
        sonic.blendInMs(120).blendOutMs(200);
        // OwlAnimation.UNACTIVE is deliberately not registered: perching shows the model's bare rest
        // pose, and UNACTIVE belongs to the Owl Statue block's own (not yet built) renderer.

        idleFly.blendInMs(350).blendOutMs(250);
        flySprint.playbackSpeed(1.2F);
        flySprint.blendInMs(250).blendOutMs(250);
        diveAttack.blendInMs(100).blendOutMs(250);
        // blockAdditive on both: the rig runs a look-at additive on the head (see CopperOwlModel's
        // Rig), so without this a dead owl keeps tracking the player all the way down.
        deathFalling.blendInMs(200).blockAdditive();
        // Short blend: the fall→impact cut IS the impact — a long crossfade softens it away.
        hitGround.blendInMs(100).blockAdditive();

        // Same sphere HitWindow as the arpy's dive_attack — proximity-based, hit-once per swing.
        HitWindow.of(0, 20)
                .shape(AttackShape.sphere(1.9F))
                .anchor(0.0F, 0.0F, 0.0F)
                .damage(4.0F)
                .knockback(0.6F)
                .onHit((attacker, target) -> this.recordGrudge(target))
                .applyTo(diveAttack);

        // A ray along the aim, born at the beak, cut short by terrain, firing on one tick so the hit
        // lands exactly when the sound and particles do. Damage is credited to the owl, not the
        // pilot: the bird is what the enemy sees and should aggro onto.
        HitWindow.of(SONIC_RELEASE_TICK, SONIC_RELEASE_TICK)
                .shape(AttackShape.beam(SONIC_RANGE, SONIC_RADIUS))
                .anchor(AttackAnchor.look(SONIC_ORIGIN_FORWARD, 0.0F, 0.0F))
                // Not aimAlongLook(): it reads getViewVector(), which depends on getXRot(), and
                // vanilla's LookControl resets xRot every tick — see pilotLookVector().
                .facing(e -> this.sonicAimVector())
                .clipToBlocks()
                .damage(SONIC_DAMAGE)
                .poiseDamage(SONIC_POISE_DAMAGE)
                .knockback(0.0F)   // the radial default is wrong for a ray — see onHit below
                .damageSource(e -> e.damageSources().sonicBoom(e))
                // Never the pilot's own body, never another owl.
                .filter(t -> !(t instanceof OwlEntity) && t.getId() != this.getControllerId())
                .onSweep((attacker, origin, facing, shape, hits) -> {
                    // Draws the beam that was actually tested, already shortened by clipToBlocks, so
                    // the visual can't claim reach the hitbox doesn't have.
                    if (!(attacker.level() instanceof ServerLevel server)
                            || !(shape instanceof AttackShape.Beam beam)) {
                        return;
                    }
                    Vec3 end = origin.add(facing.normalize().scale(beam.length()));
                    // longDistance: the default 32-block cut-off would hide the screech at exactly
                    // the range the spyglass sends the owl to.
                    ParticleFx.beam(server, MythosMortalsRegistry.OWL_BOOM.get(), origin, end,
                            SONIC_PARTICLE_SPACING, SONIC_PARTICLE_SKIP, true);
                    attacker.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.5F);
                    // Recoil for the pilot only — the camera is the owl.
                    ServerPlayer pilot = this.controller;
                    if (pilot != null) {
                        ScreenShake.forPlayer(pilot)
                                .duration(10).fadeOut(6).frequency(16.0F).amplitude(0.22F)
                                .easing(Interpolation.EASE_OUT).seed(attacker.getId())
                                .fire();
                    }
                })
                .onHit((attacker, target) -> {
                    this.recordGrudge(target);
                    // Push along the ray's axis, not radially: a beam shoves things down its length,
                    // which from above means into the ground (hence the smaller vertical term).
                    double resist = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                    // Same aim source as facing(), so push and beam can never disagree.
                    Vec3 dir = this.sonicAimVector();
                    target.push(dir.x * SONIC_KNOCKBACK_H * resist,
                            dir.y * SONIC_KNOCKBACK_V * resist,
                            dir.z * SONIC_KNOCKBACK_H * resist);
                    // Player movement is client-authoritative; hurtMarked forces the velocity out.
                    target.hurtMarked = true;
                })
                .applyTo(sonic);

        // Charge cue at the start of the windup, so the release is telegraphed rather than instant.
        sonic.onFrame(0, e -> e.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.6F, 1.6F));

        // Always airborne, so these two cover the whole cruise pose: idle_fly while stationary,
        // fly_sprint while moving. Both stand down while perched, leaving the bare rest pose.
        // The attacks are excluded by play condition rather than by layer priority: MobAnimator#play
        // only stops animations at priority <= the incoming one, and idle sits at 3 while the attacks
        // sit at 0, so idle would otherwise keep playing underneath them.
        idleFly.setPlayCondition(anim -> !this.isDiving() && !this.isScreeching()
                && !this.isPerched() && !this.isFlyingMoving());
        flySprint.setPlayCondition(anim -> !this.isDiving() && !this.isScreeching()
                && !this.isPerched() && this.isFlyingMoving());
        diveAttack.setPlayCondition(anim -> this.isDiving());

        this.animator.register(idleFly, flySprint, diveAttack, diveReturn, awake, sonic);
        // Not gated on isFlying() the way the arpy's is: the arpy has ground-death variants to pick
        // between, the owl only has this one, so gating would leave ground deaths unanimated.
        // MobAnimator checks onGround() every tick anyway, so an owl that dies standing skips
        // straight to the impact stage, which reads as collapsing.
        this.animator.registerFallingDeath(deathFalling, hitGround);
    }
}
