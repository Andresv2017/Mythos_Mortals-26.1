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

/**
 * The Copper Owl companion ("Búho de Atenea"), on its own {@link EntityType}, rendered with its own
 * {@link CopperOwlModel}/{@link OwlAnimation} keyframes via {@link OwlRenderer}. No death animation is
 * registered (see {@link #registerAnimations}) — {@link ArpyAnimation}'s death clips reference bones
 * that don't exist on this model and crashed the client on death.
 *
 * <p>Statue block → companion entity: the decorative Owl Statue block (a separate {@code Block} +
 * {@code BlockEntity}, not this class) is where the dormant/inert state lives. Right-clicking that
 * block while holding the Minotaur's Greek Bronze Core removes it and spawns this entity in its
 * place, immediately calling {@link #bondTo} with the interacting player — that bond is what
 * {@link FollowOwnerGoal} and {@link #checkPossessionGate} key off of from then on. This entity
 * itself is always fully alive/flying from the moment it exists; it has no dormant state of its own.
 *
 * <p>Unlike {@link ArpyEntity} (a hostile monster this was prototyped on), this registers no
 * combat/targeting goals: it's a permanent flying companion (see {@link FollowOwnerGoal} — never
 * lands, follows/hovers/orbits its owner) that the owner can possess via {@link #tryStartPossession}.
 */
public class OwlEntity extends AbstractFlyingEntity implements Animatable<OwlEntity>, Possessable, Perchable {

    private static final EntityDataAccessor<Boolean> DATA_IS_DIVING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Entity id of the player currently piloting this owl via Athena's Sight, or {@code -1} when
     * free. Synced so every client can tell whether the owl is under a player's control — the
     * controlling client compares it against its own {@code localPlayer.getId()} to switch its
     * camera to the owl.
     */
    private static final EntityDataAccessor<Integer> CONTROLLER_ID =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);
    /**
     * Entity id of the player this owl is currently perched on, or {@code -1} when flying free.
     * Synced: the client needs it both for the idle/cruise play conditions (which stand down while
     * perched — see registerAnimations()) and to know which player to draw the owl on (PerchClient).
     */
    private static final EntityDataAccessor<Integer> PERCH_TARGET_ID =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);
    /**
     * True while the sonic screech is running. Synced for the same reason {@link #DATA_IS_DIVING} is:
     * the idle/cruise clips are excluded by play condition, and those conditions are evaluated on both
     * sides (see {@link #registerAnimations}).
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_SCREECHING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);

    private final MobAnimator<OwlEntity> animator = new MobAnimator<>(this);

    // -----------------------------------------------------------------------
    // Perching on the owner's arm
    // -----------------------------------------------------------------------
    // Where the owl sits relative to its host lives in OwlPerchPlacement, read from here to place the
    // HITBOX and from the library's render path to draw the bird. One set of numbers for both: they
    // used to be two hand-synced copies, and tuning one without the other silently put the hitbox
    // somewhere the owl wasn't drawn.

    // -----------------------------------------------------------------------
    // Possession (Athena's Sight) — server-side authority
    // -----------------------------------------------------------------------
    private static final int POSSESSION_DURATION_TICKS = 600;   // 30 s
    /** Cooldown before the owl can be possessed again after a session ends. Deliberately short right
     * now (10 s) so the mechanic stays fast to iterate on — raise to 4800 (4 min, the designed value)
     * once the rest of the feature (upgrade gate, safety radius) is in. */
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
    /** How far from the OWNER a threat can be and still be picked up. Measured to the owner, not to
     * the owl: what makes something a target is that it is near the person being defended. */
    private static final double DEFEND_ACQUIRE_RANGE = 16.0;
    /** How far a target may get FROM THE OWNER before the owl drops it and comes home. Chasing a
     * fleeing mob to the horizon leaves the owner undefended, which is the opposite of the job. */
    private static final double DEFEND_LEASH_RADIUS = 24.0;
    /**
     * The same leash for a target the owner picked out deliberately with the spyglass. Much longer,
     * because pointing at something far away IS the point of ordering — with the defensive leash the
     * owl would drop the order the instant it was given.
     *
     * <p>Must stay comfortably above {@code OwlOrderInput.ORDER_REACH} (96) or the furthest orders
     * would be dropped on arrival. Not raised beyond this either: past roughly 160 blocks the owl
     * leaves the server's simulation distance and its chunk stops ticking, which would strand it
     * mid-flight rather than send it further.
     */
    private static final double ORDER_LEASH_RADIUS = 112.0;

    /** True while the current target was designated by the owner rather than acquired automatically.
     * Only widens the leash; cleared when {@code DefendOwnerGoal} lets the target go. */
    private boolean targetWasOrdered;

    /** "No hostiles within N blocks of the player" activation safety gate. */
    private static final double SAFETY_RADIUS = 10.0;

    // -----------------------------------------------------------------------
    // Sonic screech — the ranged attack, mouse wheel while possessed
    // -----------------------------------------------------------------------
    /** Ticks the screech occupies end to end. Must stay in step with the registered animation's
     * length (26 ticks = 1.3 s): this counter is what holds {@link #isScreeching()} up, and both the
     * clip and the idle/cruise suppression are sized off it. */
    private static final int SONIC_TOTAL_TICKS = 26;
    /** Tick within the screech at which the ray actually fires. Everything before it is windup — the
     * charge cue plus the owl braking to a hover — so the hit is telegraphed rather than instant. */
    private static final int SONIC_RELEASE_TICK = 14;
    private static final int SONIC_COOLDOWN_TICKS = 80;      // 4 s between screeches
    /** Beam length in blocks, before {@link HitWindow#clipToBlocks()} cuts it at terrain. */
    private static final float SONIC_RANGE = 18.0F;
    /** How forgiving the ray is sideways — aiming a beam at a moving mob from 15 blocks up needs some. */
    private static final float SONIC_RADIUS = 1.1F;
    private static final float SONIC_DAMAGE = 7.0F;
    private static final float SONIC_POISE_DAMAGE = 12.0F;
    /** Push along the beam's own axis (not radially away from the owl) — see the {@code onHit} in
     * {@link #registerAnimations}. */
    private static final double SONIC_KNOCKBACK_H = 1.6;
    private static final double SONIC_KNOCKBACK_V = 0.4;
    /** Where the ray is born: this far forward of the eyes along the aim, i.e. at the beak. */
    private static final float SONIC_ORIGIN_FORWARD = 0.45F;
    /** Blocks between SONIC_BOOM sprites. They are large, so one per block already reads as a solid
     * ray — denser just makes an opaque wall. */
    private static final double SONIC_PARTICLE_SPACING = 1.0;
    /** Samples skipped at the origin, so the first (huge) sprite isn't drawn inside the owl's own head. */
    private static final int SONIC_PARTICLE_SKIP = 1;
    /** Velocity blend toward a standstill while screeching: a ray fired from a bird travelling at
     * {@link #POSSESSED_DIVE_SPEED} smears across the sky instead of reading as aimed. */
    private static final double SONIC_BRAKE_ACCEL = 0.35;

    /** Whether the Athena's Sight upgrade has been applied to this owl (see {@link #mobInteract}).
     * Persisted, not synced — it's a server-only gate, the client never needs to know. */
    private boolean hasAthenaSightUpgrade;
    /** Whether the sonic-screech upgrade has been applied (see {@link #mobInteract}). Gates the
     * screech both while piloted and in the owl's own defence. Server-only, like the one above: it is
     * a permission check, never something the client needs to render. */
    private boolean hasSonicUpgrade;
    /** How many copper-ingot health upgrades have been fed to this owl (see {@link #mobInteract}),
     * capped at {@link #MAX_COPPER_UPGRADES}. Persisted so {@link #readAdditionalSaveData} knows how
     * many {@link #copperHealthModifierId} modifiers to re-apply after a reload. */
    private int copperUpgrades;
    /** Guards the one-time {@code startAirborne()} call on a fresh spawn (see {@link #tick()}). Not
     * persisted: reloading from a save re-enters through {@code readAdditionalSaveData}'s own
     * {@code startAirborne()} call (the "DlxFlying" flag is always true for this entity), so calling
     * it again here too on the very next tick is harmless — just redundant. */
    private boolean airborneInitialized;
    /** True from {@link #bondTo} until the one-shot "awake" transition ({@link OwlAnimation#AWAKE})
     * finishes: the owl spawns grounded right where the statue stood and stays there playing that
     * animation, gating {@link #airborneInitialized} in {@link #tick()} until it completes. Only set
     * for a statue-spawned owl — a plain {@code /summon} (never bonded) skips straight to airborne,
     * same as before. Not persisted: a reload resumes flying immediately via
     * {@code readAdditionalSaveData}, same as {@link #airborneInitialized}. */
    private boolean pendingAwaken;
    /** Guards the one-time {@link MobAnimator#play} trigger for "awake" in {@link #tick()}, so a tick
     * where the clip has already finished isn't mistaken for "never started" and replayed forever. */
    private boolean awakeStarted;
    /** The player this owl is bonded to, set once by {@link #bondTo} — called by the Owl Statue
     * block right when it spawns this entity (statue block → companion entity). Persisted. */
    private @Nullable UUID ownerUUID;

    private @Nullable ServerPlayer controller;
    private int possessionTicksRemaining;
    private int possessionCooldownTicks;
    private @Nullable Vec3 possessionAnchor;
    /** Latest movement intent from the controlling client (only {@code forward} is read — see
     * {@link PossessionGoal}). Fed by {@code PossessedInputServerPacket}; the body's own input is frozen
     * client-side so we can't read it off the player. Reset to {@link Input#EMPTY} at the start/end
     * of each possession. */
    private Input controlInput = Input.EMPTY;
    /** Latest look rotation from the controlling client, sent in the same packet as the input. Used
     * as the flight direction — reading it off the server player is unreliable while the camera is
     * on the owl, so the client tells us directly. */
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
    /** Mobs the owl has directly hit (see {@link #recordGrudge}) — checked by IDENTITY, not
     * distance, in {@link #clearAggro}, so a hit mob gets its target cleared no matter how far it
     * wandered or how long it takes to catch up. Value = the tick it was recorded, so an entry that
     * never resolves ages out via {@link #GRUDGE_MAX_AGE_TICKS} instead of leaking forever. */
    private final Map<Mob, Integer> grudgeMobs = new HashMap<>();
    /** Last {@code getTarget()} observed on each {@link #grudgeMobs} entry, purely for the debug
     * watcher in {@link #tick()} to log the EXACT tick a change happens — not used by any actual
     * clearing logic. */
    private final Map<Mob, LivingEntity> grudgeDebugLastTarget = new HashMap<>();

    /** Console diagnostic for the aggro-clearing investigation: logs every target change on a
     * tracked mob (tick-precise) and the before/after state of every {@link #clearAggro} pass, so
     * the actual behavior is OBSERVED instead of guessed at. Flip off once the mechanism driving the
     * re-targeting is understood/confirmed fixed. */
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
    // Copper ingot health upgrade — owner feeds copper ingots to raise max health permanently
    // -----------------------------------------------------------------------
    /** Max health granted per copper ingot fed. */
    private static final double COPPER_HEALTH_PER_INGOT = 4.0;
    /** How many ingots the owl accepts before it's maxed out (10 base + 5*4 = 36 HP / 18 hearts). */
    private static final int MAX_COPPER_UPGRADES = 5;
    /** One stable {@link AttributeModifier} id per upgrade level, so re-applying on load (see
     * {@link #readAdditionalSaveData}) can safely check {@link AttributeInstance#hasModifier} instead
     * of guessing whether vanilla's own attribute NBT round-trip already restored it — same idiom
     * {@code Mob#finalizeSpawn} uses for its own permanent FOLLOW_RANGE bonus. */
    private static @NotNull Identifier copperHealthModifierId(int level) {
        return Identifier.fromNamespaceAndPath(MythosMortals.MODID, "owl_copper_health_" + level);
    }

    // -----------------------------------------------------------------------
    // Bronze ingot heal — owner feeds bronze ingots to top up CURRENT health, uncapped uses, no
    // max-health change (that's what the copper ingot above is for). Blocked once the owl is already
    // at full health so it can't be right-clicked for nothing.
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

    /** (Re-)applies every {@link #copperHealthModifierId} up to {@link #copperUpgrades}, guarded by
     * {@link AttributeInstance#hasModifier} so this is safe to call after a reload even if vanilla's
     * own attribute NBT round-trip already restored the same modifiers — same idiom
     * {@code Mob#finalizeSpawn} uses for its permanent FOLLOW_RANGE bonus. Does NOT heal — reloading
     * shouldn't top off missing health, only guarantee the max-health bonus itself survived. */
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
            // This owl never lands: it's a permanent flying companion, no takeoff/landing/ground
            // state at all. Force airborne once on a fresh spawn (a reload already restores it via
            // readAdditionalSaveData) instead of waiting on groundRestTimer/TakeoffGoal — neither of
            // which are registered anymore. A statue-spawned owl (pendingAwaken) instead stays
            // grounded through the one-shot "awake" transition first — see bondTo().
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
            // Runs every tick regardless of goals — the re-possession cooldown must count down even
            // while PossessionGoal (which only ticks while active) isn't selected.
            if (this.possessionCooldownTicks > 0) {
                this.possessionCooldownTicks--;
            }
            // Screech bookkeeping lives here rather than in PossessionGoal, because the defence AI
            // fires the same screech with nobody piloting — and PossessionGoal.tick() only runs while
            // a pilot is in control. Left there, an AI screech would leave isScreeching() stuck true
            // forever, freezing the pose and permanently blocking the next one.
            if (this.controlSonicCooldown > 0) {
                this.controlSonicCooldown--;
            }
            if (this.controlSonicTicks > 0 && --this.controlSonicTicks == 0) {
                this.setScreeching(false);   // lets idle_fly/fly_sprint take the pose back
            }
            // Cleaning up after a possession session only — NOT whenever the owl happens to be
            // un-piloted. See aggroClearTicks for why that distinction became load-bearing once the
            // owl started fighting on its own. Also paused while it HAS a target: mid-fight is
            // exactly when its opponents are supposed to be fighting back.
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

    /** Debug-only: runs every tick (while {@link #grudgeMobs} is non-empty) and logs the EXACT tick a
     * tracked mob's {@code getTarget()} changes to/from the owl, plus its {@code getLastHurtByMob()}
     * at that instant — so if something between our once-a-second {@link #clearAggro} passes keeps
     * re-pointing it at the owl, this shows precisely how many ticks after the clear it happens
     * (immediately, next tick, N ticks later) instead of us having to infer it. */
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

    /** Server-side: fire the owl's own custom strike on the controller's left-click — the
     * {@code dive_attack} animation (borrowed from the arpy for now) and its
     * {@link net.darkblade.deluxelib.combat.HitWindow}, which carries the real combat (damage /
     * knockback / poise / debug particles, hit-once per swing). The active window doubles as the
     * re-trigger cooldown (see {@code PossessionGoal.tick}). */
    public void performControlledAttack() {
        if (this.controlAttackCooldown > 0 || this.controlReturnTicks > 0 || this.controlSonicTicks > 0) {
            return;   // already diving, pulling up, or mid-screech
        }
        this.controlAttackCooldown = 16;   // ~one dive_attack swing
        this.setDiving(true);
    }

    /**
     * Server-side: fire the sonic screech on the controller's mouse-wheel click
     * ({@code OwlSonicAttackServerPacket}) — the ranged counterpart to the left-click dive. The
     * {@code sonic_screech} animation carries the real combat through its
     * {@link net.darkblade.deluxelib.combat.HitWindow}: a 3D {@link AttackShape.Beam} along the
     * pilot's aim, cut short by terrain, firing on one exact tick (see {@link #registerAnimations}).
     *
     * <p>Started imperatively rather than by play condition because the clip is {@link Loop#PLAY_ONCE}
     * and {@link MobAnimator} only auto-starts REPEATING animations — same as {@code awake} and
     * {@code dive_attack_return}. {@link #controlSonicTicks} is what keeps the state (and the idle
     * suppression) up for the clip's whole length; the cooldown starts now, not when it ends, so the
     * usable rate is exactly {@link #SONIC_COOLDOWN_TICKS}.
     */
    public void performSonicAttack() {
        // No isPossessed() check: the defence AI fires this too (see DefendOwnerGoal). The upgrade is
        // what gates it in both cases, and the cooldown/other-move guards are shared.
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
     * Right-click does one of three things, by held item:
     * <ul>
     *   <li><b>Empty hand</b> (owner only): toggles perching — the owl teleports onto the owner's
     *       raised right arm and holds the {@code perch} pose, or takes off again if already perched.</li>
     *   <li><b>Spyglass</b>, not yet upgraded: applies the Athena's Sight upgrade.</li>
     *   <li><b>Copper ingot</b> (owner only), below {@link #MAX_COPPER_UPGRADES}: feeds it, raising
     *       max health by {@link #COPPER_HEALTH_PER_INGOT} and healing by the same amount.</li>
     *   <li><b>Bronze ingot</b> (owner only), while not at full health: heals
     *       {@link #BRONZE_HEAL_AMOUNT} current health (no max-health change) and puffs a few copper
     *       particles. Refused outright once the owl is topped up.</li>
     * </ul>
     * Activating the ability itself is the keybind (see {@link #tryStartPossession}), never this — a
     * bonded owl flies beside you, it isn't something you walk up and click.
     */
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getItemInHand(hand);

            if (held.isEmpty()) {
                if (this.level().isClientSide()) {
                    return InteractionResult.SUCCESS;   // predicts the arm-swing for the perch toggle
                }
                // Only the bonded owner can call it to their arm; anyone else's empty-hand click is
                // left to the default (nothing).
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

            // Owner-gated (like the perch toggle) — a companion's own stats shouldn't be tunable by
            // just anyone who walks up to it.
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

            // Bronze ingot: pure heal, no max-health change, no upgrade count — just tops up current
            // health for as many ingots as it takes. Gated on not being full so it can't be clicked
            // for nothing once the owl's topped up.
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

            // The two progression upgrades. Owner-gated for the same reason feeding is: what a
            // companion can do shouldn't be decidable by a passer-by.
            //
            // The spyglass is NOT the upgrade item any more — it became the tool you point at a
            // target to send the owl (see OwlOrderInput), and an item cannot sensibly be both the
            // permanent unlock and the everyday tool. It survives as an ingredient of the Athena
            // Ocular's recipe instead.
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
        // A perched owl deliberately shares space with its host, so vanilla's entity separation has to
        // be switched off: Entity#push shoves BOTH participants apart, and it is only skipped when one
        // of them has noPhysics. Without this, merely turning the camera sweeps the owl's hitbox
        // around the host and drags them sideways.
        this.noPhysics = true;
        Vec3 spot = perchPosition(player);
        this.teleportTo(spot.x, spot.y, spot.z);
        // Claim the hotbar slot the owner had selected: perching requires an empty hand, and from
        // here on the game treats that slot as holding something (see PerchManager).
        PerchManager.begin(player, this, player.getInventory().getSelectedSlot());
    }

    /** Entry point for the "right click anywhere to dismount" input handler
     * ({@code DismountPerchServerPacket}): releases the owl from the arm without needing to aim at
     * its (hitbox-lags-the-arm-render) perched position, the same way {@link #tryStartPossession}
     * doesn't need a click on the owl either. Re-checks ownership and perch state server-side rather
     * than trusting the client. */
    @Override
    public void tryStopPerching(ServerPlayer player) {
        if (this.isPerched() && !this.isPossessed() && player.getUUID().equals(this.ownerUUID)) {
            this.stopPerching();
        }
    }

    /** Clears this owl's claim on its host's hotbar slot, if it has one. Safe to call when it isn't
     * perched, and safe to call twice — which is why every path that can end a perch calls it,
     * including the ones that never go through {@link #stopPerching()} (death, removal). */
    private void releasePerchedHand() {
        if (!this.level().isClientSide()
                && this.level().getEntity(this.getPerchTargetId()) instanceof Player host) {
            PerchManager.end(host);
        }
    }

    /** Server-side: leave the arm and go back to free companion flight. */
    private void stopPerching() {
        // Release the hand BEFORE clearing the target id — that id is the only way back to the host,
        // and a leaked entry leaves them with a hotbar slot the game quietly refuses to fill.
        releasePerchedHand();
        this.entityData.set(PERCH_TARGET_ID, -1);
        this.noPhysics = false;
        // A small upward nudge so the take-off reads as launching off the arm rather than dropping.
        this.setDeltaMovement(0.0, 0.25, 0.0);
    }

    /** Belt-and-braces with the {@code noPhysics} flag set in {@link #startPerching}: also stop the
     * owl from initiating pushes of its own while it is riding on someone. */
    @Override
    protected void pushEntities() {
        if (!this.isPerched()) {
            super.pushEntities();
        }
    }

    /** World position of {@code player}'s raised right hand — where the owl sits while perched.
     * Player's right side is {@code (-cos(yaw), -sin(yaw))}: at yaw 0 the player faces +Z (south),
     * so their right hand points toward -X (west). */
    private static Vec3 perchPosition(Player player) {
        // Same three numbers the render path uses (see OwlPerchPlacement) — this places the HITBOX,
        // i.e. the owl's logical position. A live tuning session overrides them client-side only, so
        // while /deluxelib debug owlperch is on the drawn bird and this position drift apart until
        // the values are pasted back; that's expected for a debug tool.
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

    /** Bonds this owl to {@code player} as its owner. Called once, right after spawning, by the Owl
     * Statue block's use-handler (statue block → companion entity — see the class javadoc); nothing
     * else ever needs to call this. From that point on {@link FollowOwnerGoal} and
     * {@link #checkPossessionGate} key off this specific player. Also arms {@link #pendingAwaken}, so
     * {@link #tick()} keeps the owl grounded playing the "awake" transition before it takes flight. */
    public void bondTo(ServerPlayer player) {
        this.ownerUUID = player.getUUID();
        this.pendingAwaken = true;
    }

    /** The bonded owner, or {@code null} if unbonded, offline, or in a different dimension right now
     * (chasing across dimensions makes no sense — same guard {@link PossessionGoal} uses). Shared by
     * the follow, defend and targeting goals. */
    private @Nullable Player findOwner() {
        UUID owner = this.ownerUUID;
        if (owner == null) {
            return null;
        }
        Player player = level().getPlayerByUUID(owner);
        return player != null && player.level() == level() ? player : null;
    }

    /**
     * Whether {@code candidate} is something the owl may attack on its owner's behalf.
     *
     * <p>Adds the distance test to {@link #isAttackableForOwner}: {@code DEFEND_ACQUIRE_RANGE} keeps
     * the owl from launching at something that merely happens to be in the owner's damage history
     * from a fight two hundred blocks ago.
     */
    private boolean canTargetForOwner(@Nullable LivingEntity candidate, @NotNull Player owner) {
        return isAttackableForOwner(candidate)
                && candidate.distanceToSqr(owner) <= DEFEND_ACQUIRE_RANGE * DEFEND_ACQUIRE_RANGE;
    }

    /**
     * The part of {@link #canTargetForOwner} that is about <em>what</em> may be attacked rather than
     * how close it is — split out because a spyglass order deliberately reaches past
     * {@link #DEFEND_ACQUIRE_RANGE}, but must obey the same exclusions.
     *
     * <p><b>Other players ARE valid targets</b>, both automatically and by order: if someone attacks
     * the owner, defending them means fighting that person. The exclusions are only the ones that
     * would be nonsense — the owner themselves, the owl itself, another companion owl, and
     * spectators, who cannot be interacted with at all.
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

    /** Whether {@code candidate} is this owl's bonded owner — the one living thing it must never
     * turn on, however the target was chosen. */
    private boolean isOwner(@NotNull LivingEntity candidate) {
        return this.ownerUUID != null && this.ownerUUID.equals(candidate.getUUID());
    }

    /**
     * Server-side: send the owl after a target the owner picked out with the spyglass
     * ({@code OwlOrderAttackServerPacket}).
     *
     * <p>Re-validates everything rather than trusting the client: that this is the bonded owner, that
     * the owl is in a state where it can go (not perched, not piloted, not still waking), and that the
     * target is something it is allowed to attack at all. An order overrides whatever the owl had
     * picked automatically — the owner outranks the AI.
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
     * Copper coming off the statue for as long as the awakening lasts.
     *
     * <p><b>Emitted over time rather than all at once</b>, because a particle's lifetime is fixed by
     * its own client-side class and cannot be set from here: vanilla's block shards live a handful of
     * ticks and fall, so a single burst is gone almost before you see it. What <em>is</em> ours to
     * control is when they are spawned — dribbling a few every couple of ticks keeps the effect on
     * screen for the whole transition, and ties it to the animation's real duration instead of
     * guessing at one.
     *
     * <p>Runs while the owl is still grounded mid-{@code awake}, so it reads as the statue shedding
     * itself rather than as a puff that happened to coincide.
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

    /** True while the owl is grounded playing the one-shot "awake" transition right after spawning
     * from the statue (see {@link #bondTo}) — goals that would fly it around must stand down until
     * this clears (see {@link FollowOwnerGoal#canUse}). */
    public boolean isAwakening() { return this.pendingAwaken; }

    /** Entry point for the Athena's Sight activation keybind ({@code ActivatePossessionServerPacket}):
     * starts a possession session if every gate passes, and reports whether it did. No entity id is
     * needed on the client side to reach this — the packet finds the nearest eligible companion owl
     * server-side and calls straight in here. On failure, tells the player specifically why for the
     * two actionable cases (cooldown, hostiles too close) — see {@link #reportPossessionBlocked}. */
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

    /** Why {@link #tryStartPossession} refused to start — {@link #checkPossessionGate}'s result.
     * Only {@link #COOLDOWN} and {@link #HOSTILES_NEARBY} are actionable feedback the player can act
     * on immediately (wait, or back off); {@link #SETUP} bundles the structural gates (not bonded,
     * not upgraded, not perched, already piloting something, dead) that shouldn't come up in normal
     * play once the companion is actually set up, so those stay silent as before. */
    private enum PossessionGate { OK, COOLDOWN, HOSTILES_NEARBY, IN_COMBAT, SETUP }

    /** Same gates {@code canStartPossession} used to check as one big {@code &&}, split out so the
     * caller can report the SPECIFIC reason instead of a flat "nothing happened". Order matters: the
     * structural gates are checked first (a broken setup is reported the same way regardless of
     * cooldown/hostiles), then cooldown, then hostiles — matching the two the player actually asked
     * to be told about. */
    private @NotNull PossessionGate checkPossessionGate(@NotNull ServerPlayer player) {
        // Ownership and liveness first, so a stranger's key press stays silent rather than being told
        // about someone else's owl.
        if (!this.isAlive() || !player.getUUID().equals(this.ownerUUID)) {
            return PossessionGate.SETUP;
        }
        // Checked BEFORE the rest of the setup gates, and that ordering is the whole point: an owl in
        // a fight is never perched, so it would otherwise always fall through to the deliberately
        // silent SETUP case and the key would appear broken. Athena's Sight is reconnaissance — a
        // drone you launch from your arm — not something you reach for mid-brawl.
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

    /** Sends {@code message} to the player's action bar specifically (the bar above the hotbar), not
     * chat — {@code Player#displayClientMessage}'s exact signature has moved around across MC
     * versions, so this goes straight through the dedicated vanilla packet instead, which has stayed
     * a stable single-{@code Component} shape for years. */
    private static void sendActionBar(@NotNull ServerPlayer player, @NotNull Component message) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(message));
    }

    /** "Debes estar a salvo antes de desplegar el reconocimiento": blocks activation if any hostile
     * (anything marked {@link Enemy}, vanilla or modded) is alive within {@link #SAFETY_RADIUS} of
     * the player's own position. Excludes owls themselves: {@code OwlEntity} inherits {@code Enemy}
     * from the shared flying-mob base it borrows (a hostile-monster leftover), but a companion owl —
     * this one or anyone else's — is never the threat this gate is guarding against. Without this
     * exclusion the owl standing right next to the player (to be interacted with) always counted as
     * its own nearby hostile and permanently blocked activation. */
    private static boolean hasHostilesNearby(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(SAFETY_RADIUS);
        return !player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e instanceof Enemy && !(e instanceof OwlEntity)).isEmpty();
    }

    /** Radius the area-scan half of {@link #clearAggro} covers — wider than {@link #SAFETY_RADIUS}
     * since a mob still closing in may be well outside melee range. */
    private static final double DEAGGRO_RADIUS = 48.0;
    /** How often {@link #clearAggro} runs while unposed (once a second) — cheap enough to repeat
     * indefinitely instead of expiring, see its javadoc. */
    private static final int AGGRO_CHECK_INTERVAL_TICKS = 20;
    /** How long a {@link #grudgeMobs} entry is kept waiting for its mob to actually re-target the owl
     * before giving up on it (60s) — long enough for a slow mob to cross realistic distances, short
     * enough that the map can't grow forever across a long session full of one-off fights. */
    private static final int GRUDGE_MAX_AGE_TICKS = 20 * 60;

    /**
     * Ticks left in the post-possession aggro cleanup, armed by {@link #stopPossession()}.
     *
     * <p><b>Why this is a window again, after having deliberately been unbounded.</b> The clean-up
     * used to run forever while the owl wasn't piloted, because a straggler can take a long time to
     * reach the owl and any fixed expiry risked missing it. That was correct when the owl never
     * fought: nothing should ever have been angry at it except leftovers from a session.
     *
     * <p>It stopped being correct when the owl became a combat companion. "Not piloted" is now its
     * normal fighting state, so an unbounded clean-up means every mob it attacks is made to forget it
     * a second later — the owl becomes untouchable and its opponents never fight back. Scoping the
     * clean-up to "just came out of a possession" restores the original intent: tidy the pilot's mess,
     * don't disarm the bird.
     *
     * <p>Matched to {@link #GRUDGE_MAX_AGE_TICKS} on purpose — the window ends exactly when the grudge
     * bookkeeping it drives ages out, so there is no stretch of time where entries are tracked but
     * nothing acts on them.
     */
    private static final int AGGRO_CLEAR_WINDOW_TICKS = GRUDGE_MAX_AGE_TICKS;

    /** Counts {@link #AGGRO_CLEAR_WINDOW_TICKS} down after a possession ends; 0 = no clean-up owed. */
    private int aggroClearTicks;

    /** Records {@code target} as a mob the owl itself just hit, so {@link #clearAggro} can resolve
     * its aggro later by IDENTITY rather than having to spatially rediscover it. Called from both
     * attacks' {@code HitWindow.onHit} — see {@code registerAnimations()}. */
    private void recordGrudge(@NotNull LivingEntity target) {
        // Only a PILOT's kills leave a mess worth cleaning up. Both attacks are shared with the
        // defence AI now, so without this the map would fill during ordinary fights — entries nothing
        // would ever consume (the clean-up only runs after a possession) and that would just sit there
        // ageing out. Worse, if a fight happened to overlap the post-possession window, the owl would
        // be made to forget mobs it is actively fighting.
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
     * Clears hostile mobs' memory of THIS owl as a target — two complementary passes, run once a
     * second ({@link #AGGRO_CHECK_INTERVAL_TICKS}) during the post-possession window
     * ({@link #AGGRO_CLEAR_WINDOW_TICKS}), and paused whenever the owl has a target of its own.
     *
     * <p><b>Scope note:</b> this used to run indefinitely whenever the owl wasn't piloted. That was
     * right while the owl never fought; it is wrong now that it defends its owner, because "not
     * piloted" is its normal fighting state and clearing then would make every opponent forget it a
     * second after being hit. See {@link #AGGRO_CLEAR_WINDOW_TICKS}.
     *
     * <p>Both attacks (dive and sonic) deliberately credit damage to the owl, not the pilot (see
     * their {@code HitWindow} configs), so a hit mob's vanilla {@code HurtByTargetGoal} remembers the
     * OWL and chases it — for vanilla zombies (which register that goal with
     * {@code setAlertOthers()}) potentially the whole nearby group. That's the intended read while
     * the owl is actively flying and fighting; once it's back on the player's arm being a passive
     * companion, a mob train marching toward it (and so toward the player standing right there)
     * isn't. Each clear sets both {@code getTarget()} (what a running target goal reads each tick)
     * and {@code getLastHurtByMob()} (what {@code HurtByTargetGoal.canUse()} would otherwise
     * re-trigger from on its very next evaluation), so the mob genuinely loses interest instead of
     * re-acquiring the owl a tick later.
     *
     * <ol>
     *   <li><b>{@link #grudgeMobs}</b> — mobs the owl itself hit (see {@link #recordGrudge}), checked
     *   by IDENTITY: however far away or however long it takes them to catch up, the instant
     *   {@code getTarget() == this} is true they're cleared and dropped from the map. This is what a
     *   fixed-radius/fixed-duration scan can never guarantee for a specific mob that started far
     *   away — there's no "correct" radius or timeout for a problem that's really about how long a
     *   given mob takes to close a gap, not where it currently is. An entry that never resolves
     *   within {@link #GRUDGE_MAX_AGE_TICKS} is dropped too, so this can't leak forever.</li>
     *   <li><b>A {@link #DEAGGRO_RADIUS}-block area scan</b> — the fallback for mobs the owl never
     *   directly hit but that got pulled in anyway via {@code setAlertOthers()}. There is no event to
     *   learn exactly who got alerted, so this is necessarily a spatial guess — but since it now runs
     *   forever (not a bounded window) rather than giving up after a few seconds, a straggler that
     *   takes longer than expected to close the distance still eventually falls inside the radius.</li>
     * </ol>
     */
    /**
     * Force-stops every currently RUNNING goal in {@code mob}'s target-selector — confirmed the real
     * fix, not {@code setTarget(null)} alone, via the debug log: a mob's targeting goal, once
     * started, stays "running" until ITS OWN {@code canContinueToUse()} says otherwise (checked by
     * {@link GoalSelector#tick()}'s cleanup pass), and a still-running goal can put the target right
     * back on its own next {@code tick()}/{@code start()} — exactly the one-tick-later restoration
     * the log showed after every external clear, on the ONE husk that had been continuously chasing
     * since being hit (versus the horde-alerted ones, whose goal had apparently already stopped
     * running by the time the area scan reached them, which is why clearing those stuck on the first
     * try). Stopping the {@link WrappedGoal} directly (public API, confirmed from
     * {@code GoalSelector}/{@code WrappedGoal} source) forces its {@code isRunning} flag false, so
     * the selector's cleanup has nothing left to "continue" — whatever the goal actually is (no need
     * to know its exact class), it must genuinely re-satisfy its own {@code canUse()} from scratch to
     * ever retarget the owl again, which for a hurt-based goal means an actual NEW hit.
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
            // Unconditional, NOT gated on getTarget()==this: dropping an entry the moment that check
            // happened to read false (before re-checking) let a mob that took longer than
            // GRUDGE_MAX_AGE_TICKS to re-acquire the owl age out of the map WITHOUT ever actually
            // being cleared — we'd stop watching it while it was still, in fact, targeting the owl.
            // Stripping it every single check instead (harmless no-op if it's already null) means it
            // gets corrected again next second even if something re-set it in between, for the whole
            // age-out window — only after that long with the target repeatedly stripped do we give up
            // bookkeeping it, by which point it's had every chance to actually lose interest.
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
        // checkPossessionGate now requires isPerched(), so this always launches the owl off the arm.
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
        // Owls see in the dark — grant the pilot night vision for the session's duration. Applied here
        // (not refreshed per-tick) with an effectively-infinite duration, since applyOwlNightVision is
        // paired with removeOwlNightVision on every possession-ending path (stopPossession, die,
        // remove), not left to expire naturally.
        applyOwlNightVision(player);
        // The airborne setup and per-tick control live in PossessionGoal (priority 0, MOVE+LOOK),
        // which the goal selector starts as soon as isPossessed() flips true. Driving movement from
        // a goal with AI on — the same proven path as the arpy's own DiveAttackGoal — is what
        // actually lets travel() apply the velocity/rotation we set (setNoAi disabled that).
    }

    /**
     * The pilot's raw 3D aim (unit vector), computed directly from {@link #controlYaw}/
     * {@link #controlPitch} rather than from this entity's own {@code getXRot()}/{@code getYRot()}.
     *
     * <p>Reason: vanilla's default {@code LookControl.tick()} — run every tick from {@code Mob.aiStep()}
     * right after the goal selector, so right after {@link PossessionGoal#tick()} sets {@code xRot} —
     * resets {@code xRot} back toward 0 whenever nothing has actively called {@code lookAt(...)},
     * which nothing does here. {@link #registerGoals} never disables that (no flying-mob look control
     * override exists in this codebase), so {@code getXRot()}/{@code getViewVector()} cannot be trusted
     * for aim while possessed — reading them made the sonic beam always fire level regardless of pitch.
     * This method and the movement code in {@link PossessionGoal#tick()} both read the same raw fields,
     * so aim, flight direction and the beam can never disagree.
     */
    private Vec3 pilotLookVector() {
        float yawRad = this.controlYaw * ((float) Math.PI / 180.0F);
        float pitchRad = this.controlPitch * ((float) Math.PI / 180.0F);
        float cosP = Mth.cos(pitchRad);
        return new Vec3(-Mth.sin(yawRad) * cosP, -Mth.sin(pitchRad), Mth.cos(yawRad) * cosP);
    }

    /**
     * Where the sonic beam actually points — the one aim source for both ways the screech can be
     * fired, used by its {@code HitWindow}'s {@code facing()} and by its knockback direction.
     *
     * <p>Piloted, that is {@link #pilotLookVector()}: the player's raw aim, for the reasons in its
     * javadoc. Fired by the owl's own defence there is no pilot at all — {@code controlYaw}/
     * {@code controlPitch} are whatever the last session left behind — so the direction is computed
     * straight from the owl's eyes to the middle of its target. Without this split an AI screech
     * would fire along a stale heading from some previous flight, and look like the beam simply
     * missing for no reason.
     *
     * <p>Falls back to the entity's own look only if there is somehow no target, which the callers
     * make unreachable in practice.
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

    /** Ends a live possession: lands the owl back on the player's arm (it always launched from there
     * — see {@link #checkPossessionGate}) and starts the re-possession cooldown. Safe to call when the
     * controller has gone (logout/death) — it just skips the return-to-arm and resumes free flight. */
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
        // Immediate pass right now (catches whoever's already close), then arm the window so tick()'s
        // per-second check keeps catching stragglers as they arrive. Outside this window the owl is
        // allowed to be fought normally — see AGGRO_CLEAR_WINDOW_TICKS.
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
            // PerchGoal (same priority-0 band PossessionGoal just vacated) picks this straight back
            // up next tick, so the auto-perch reads as landing rather than freezing mid-air.
            this.startPerching(player);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.setFlying(true);
        }
    }

    /**
     * Drives the owl while a player pilots it. Runs as an exclusive MOVE+LOOK goal (priority 0) with
     * AI on — driving movement from a goal with AI on is what makes {@code travel()} actually apply
     * the velocity/rotation we set each tick. Mirrors the controller's aim onto the owl (the owl IS
     * the camera) and turns W into flight (elytra-style: the only movement key, all steering is the
     * mouse).
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
            // Attack state machine: DIVE (diving) → automatic RETURN pull-up → free control. The
            // screech's own timers are NOT here — they live in OwlEntity.tick(), because the defence
            // AI fires the same screech with no pilot to tick them down. Still mutually exclusive:
            // performSonicAttack / performControlledAttack each refuse while the other is running.
            if (OwlEntity.this.controlAttackCooldown > 0) {
                if (--OwlEntity.this.controlAttackCooldown == 0) {
                    setDiving(false);   // end the dive_attack swing / HitWindow
                    OwlEntity.this.controlReturnTicks = POSSESSED_RETURN_TICKS;
                    animator.play(animator.getByName("dive_attack_return"));   // pull-up anim
                }
            } else if (OwlEntity.this.controlReturnTicks > 0) {
                OwlEntity.this.controlReturnTicks--;
            }

            // Aim comes straight from the controlling client (sent with the input packet), not from
            // the server player's rotation — that isn't reliably synced while the camera is the owl.
            float yaw = OwlEntity.this.controlYaw;
            float pitch = OwlEntity.this.controlPitch;
            setYRot(yaw);
            setXRot(pitch);
            OwlEntity.this.yHeadRot = yaw;
            OwlEntity.this.yBodyRot = yaw;

            // 3D look direction (unit) — the aim, steerable every tick. See pilotLookVector() for why
            // this is computed from controlYaw/controlPitch directly rather than read back off
            // getXRot()/getYRot() right after setting them above.
            Vec3 look = pilotLookVector();
            double lookX = look.x, lookY = look.y, lookZ = look.z;

            Vec3 desired;
            double accel;
            if (OwlEntity.this.controlSonicTicks > 0) {
                // Screech: brake to a hover and hold the aim. The ray is fired along the look vector,
                // so drifting through the release tick would smear the beam across the sky — and a
                // stationary owl telegraphing a charged shout is the read we want anyway. Still fully
                // steerable: the aim above is applied every tick regardless.
                desired = Vec3.ZERO;
                accel = SONIC_BRAKE_ACCEL;
            } else if (OwlEntity.this.controlAttackCooldown > 0) {
                // Auto-dive: while the strike is active the owl commits a fast downward-biased swoop
                // (the dive move itself — no W needed), still steerable by the aim. This is what
                // carries it onto the target so the dive_attack HitWindow lands.
                desired = new Vec3(lookX, lookY - POSSESSED_DIVE_DOWN_BIAS, lookZ)
                        .normalize().scale(POSSESSED_DIVE_SPEED);
                accel = POSSESSED_DIVE_ACCEL;
            } else if (OwlEntity.this.controlReturnTicks > 0) {
                // Automatic pull-up return: climb back out with a little forward drift along the
                // aim's heading, so the owl swoops up after the strike. Horizontal part of `look`,
                // re-normalized — pitch is dropped on purpose (the climb speed is its own term).
                Vec3 horiz = new Vec3(look.x, 0.0, look.z);
                Vec3 horizDir = horiz.lengthSqr() > 1.0E-6 ? horiz.normalize() : new Vec3(0.0, 0.0, 1.0);
                desired = new Vec3(horizDir.x * POSSESSED_RETURN_FWD, POSSESSED_RETURN_CLIMB, horizDir.z * POSSESSED_RETURN_FWD);
                accel = POSSESSED_RETURN_ACCEL;
            } else {
                // Elytra-style cruise: the ONLY movement key is W (thrust along the look direction);
                // all steering is the mouse. No back / strafe / vertical keys.
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
        // Release the controller before the corpse logic runs so the player isn't left flagged as
        // possessing (which would keep their body invulnerable forever). No return teleport here —
        // the owl is dying.
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
        // A dying owl never reaches stopPerching(), so its claim on the owner's hand has to be
        // released here too or that hotbar slot stays unusable for the rest of the session.
        releasePerchedHand();
        super.die(source);
    }

    /**
     * Releases the controller when the owl is explicitly removed without dying — {@code kill()} and
     * {@code discard()}, which are the calls that actually route through here.
     *
     * <p>Deliberately <b>not</b> the catch-all it was once described as: chunk unload and world
     * shutdown never reach this override, because they go through {@code Entity.setRemoved} (which
     * is {@code public final}, so it cannot be intercepted) to {@code onRemoval} instead. Those two
     * cases are closed on the library side by
     * {@link net.darkblade.deluxelib.entity.possession.PossessionEvents}, whose sweep clears
     * {@link net.darkblade.deluxelib.entity.possession.PossessionManager} on
     * {@code EntityLeaveLevelEvent}, player logout and server stop — which matters because that map
     * is {@code static}, so an entry left behind would outlive the world and keep granting the
     * player damage immunity.
     */
    @Override
    public void remove(net.minecraft.world.entity.Entity.@NotNull RemovalReason reason) {
        // Covers explicit removal without death (kill/discard) — NOT chunk unload, which bypasses
        // remove() entirely via the final setRemoved -> onRemoval path; PossessionEvents sweeps that
        // case. Either way the controller must not stay flagged as possessing, or their body would
        // stay invulnerable with no owl left to end the session.
        if (!this.level().isClientSide() && this.controller != null) {
            PossessionManager.end(this.controller);
            removeOwlNightVision(this.controller);
            this.controller = null;
        }
        releasePerchedHand();
        super.remove(reason);
    }

    // -----------------------------------------------------------------------
    // Goals — no combat/targeting: this is a companion, not a monster. Always airborne (see
    // tick()): no takeoff/landing/ground-wander goals are registered at all, so every
    // AbstractFlyingEntity config hook tied to that state machine (min/max flight altitude, ground
    // rest ticks, landing speeds, takeoff tilt, etc.) is dead for this entity and deliberately
    // omitted rather than left overriding nothing.
    // -----------------------------------------------------------------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Highest priority: while possessed this claims MOVE+LOOK so nothing else can run.
        this.goalSelector.addGoal(0, new PossessionGoal());
        // Perching also claims MOVE+LOOK, so FollowOwnerGoal stands down while on the arm.
        this.goalSelector.addGoal(0, new PerchGoal());
        // Defending outranks following: while there's a target the owl leaves the owner's side.
        // Both possession and perching sit at priority 0 with MOVE+LOOK, so piloting or being on the
        // arm already beats fighting without this goal needing its own guard for either.
        this.goalSelector.addGoal(1, new DefendOwnerGoal());
        this.goalSelector.addGoal(2, new FollowOwnerGoal());

        // Target acquisition. Both are TARGET-flagged, so they run alongside the movement goals
        // rather than competing with them.
        this.targetSelector.addGoal(1, new OwnerHurtByGoal());
        this.targetSelector.addGoal(2, new OwnerHurtGoal());
    }

    /**
     * Holds the owl glued to its owner's raised right arm while perched, by writing its position
     * directly every tick (no pathfinding, no velocity) — the arm is a moving target and any
     * acceleration-based follow would visibly lag behind it.
     *
     * <p>The owl's body yaw is matched to the owner's so it always faces the same way they do,
     * which is what makes it read as riding on them rather than hovering nearby.
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
            // The owner may have logged out / died rather than dismissing it — make sure the synced
            // flag can't stay stuck on with nobody to perch on.
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

    /**
     * Acquires whoever just hurt the owner. Vanilla's {@code OwnerHurtByTargetGoal} would be the
     * obvious reuse, but it is typed to {@code TamableAnimal} and this owl is an
     * {@link net.darkblade.deluxelib.entity.AbstractFlyingEntity}, so it gets its own — small enough
     * that the type gymnastics to share vanilla's would cost more than the duplication.
     */
    private class OwnerHurtByGoal extends Goal {
        OwnerHurtByGoal() {
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isPossessed() || isPerched() || isAwakening()) {
                return false;
            }
            // Don't yank the owl off a fight it is already committed to: the owner's
            // getLastHurtByMob() lingers for seconds, so without this the goal would keep re-electing
            // the same attacker over whatever the owl is currently diving at.
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
     * The owl's melee defence: an aerial dive-bomb run against its current target.
     *
     * <p>Structure ported from {@code ArpyEntity.DiveAttackGoal}, which is the same manoeuvre already
     * proven in-game: climb to an offset point above the target (REPOSITION), turn to face it
     * (ALIGN), commit to a fast descent (DIVE), then climb back out (PULLUP) and go round again from
     * a fresh angle. Direct velocity control with navigation stopped, like every other goal on this
     * entity.
     *
     * <p>The damage is not applied here. Raising {@link #setDiving(boolean)} plays {@code dive_attack},
     * and the {@link HitWindow} attached to that animation is what deals damage, knockback and the
     * grudge bookkeeping — the exact same path the possessed dive uses. This goal only flies the owl
     * into position and decides when the dive is on.
     *
     * <p>It deliberately does <em>not</em> touch {@code controlAttackCooldown}/{@code setDiving} state
     * owned by {@link PossessionGoal}: that field is only ticked down while a pilot is in control, so
     * borrowing it here would leave {@code isDiving()} stuck true with nothing to clear it.
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
            // the sky between windup and release, exactly as the piloted screech does. The phase
            // machine resumes once the clip is done.
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
            // Lined up. With the screech unlocked and off cooldown, shout from here instead of
            // diving: it is the ranged option, and the owl is already at range having climbed to its
            // attack altitude. The cooldown is what makes this alternate with dives rather than
            // replace them.
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

    /** True while the owl is engaged — it has picked, or been given, something to attack. Used to keep
     * possession out of fights (see {@link #checkPossessionGate}); resolves itself as soon as
     * {@code DefendOwnerGoal} drops the target, after which the owl flies home on its own. */
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
     * Default un-possessed behaviour once awake: a flying companion that stays near its bonded owner
     * (see {@link #wake}), driven by direct velocity control (same idiom as {@link PossessionGoal})
     * rather than pathfinding. While the owner is moving (or just stopped), it chases/holds a point
     * just off their side and above head height; once they've stood still for
     * {@link #ORBIT_AFTER_TICKS}, it switches to slowly orbiting them instead of freezing in place.
     */
    private class FollowOwnerGoal extends Goal {
        private static final double FOLLOW_SPEED = 0.5;
        /** Position-error gain (kp) and velocity damping (kd) of a small critically-damped PD
         * controller (see the comment in {@link #tick()}) — not independent knobs to retune loosely:
         * raising {@code FOLLOW_POS_GAIN} or lowering {@code FOLLOW_DAMPING} reintroduces the ringing
         * this pair was specifically chosen to avoid (verified via the linear recurrence's
         * eigenvalues — these keep them real, i.e. non-oscillating). */
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
                // A point off to the player's side and above head height — flying beside you, not
                // perched anywhere (no shoulder/ground pose exists yet either way).
                float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
                double sideX = Math.cos(yawRad) * FOLLOW_SIDE;
                double sideZ = Math.sin(yawRad) * FOLLOW_SIDE;
                target = curPos.add(sideX, FOLLOW_HEIGHT, sideZ);
            }

            // Critically-damped PD controller: acceleration = kp*error - kd*velocity. NOT the old
            // "distance-proportional target velocity fed through a second smoothing pass" — cascading
            // two first-order lags like that makes the whole thing a lightly-damped 2nd-order system,
            // which rings/overshoots on any large initial gap (right after spawning grounded, or right
            // after un-perching, both start far from the follow point) — the exact "approaches, backs
            // off, approaches again" ping-pong before it eventually settles.
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
    // Animatable — CopperOwlModel geometry, OwlAnimation keyframes. No death animation registered
    // (see registerAnimations()) — ArpyAnimation's death_falling/hit_ground target bones that don't
    // exist on this model and crashed the client; falls back to vanilla death timing for now.
    // -----------------------------------------------------------------------
    @Override
    public @NotNull MobAnimator<OwlEntity> animator() {
        return this.animator;
    }

    @Override
    public void registerAnimations() {
        // The owl's own Blockbench export (see CopperOwlModel) instead of the borrowed arpy set —
        // bone names match 1:1, death included.
        StandardAnimation idleFly = new StandardAnimation("idle_fly",
                new AnimSource(() -> OwlAnimation.FLY_IDLE), Loop.REPEATING, 0, 3, 0.7246F);
        StandardAnimation flySprint = new StandardAnimation("fly_sprint",
                new AnimSource(() -> OwlAnimation.FLY), Loop.REPEATING, 0, 2, 0.5F);
        StandardAnimation diveAttack = new StandardAnimation("dive_attack",
                new AnimSource(() -> OwlAnimation.DIVE_ATTACK), Loop.REPEATING, 0, 0, 0.517F);
        StandardAnimation diveReturn = new StandardAnimation("dive_attack_return",
                new AnimSource(() -> OwlAnimation.DIVE_RECOVER), Loop.PLAY_ONCE, 0, 0, 0.2433F);
        // Two-stage death: FALL loops open-ended while the corpse drops, then chains into HIT the
        // moment it lands. Both are the owl's own clips now — the previous attempt borrowed the arpy's
        // death_falling/hit_ground, which animate bones like "torso" that CopperOwlModel does not
        // have, and that crashed the client with IllegalArgumentException("Cannot animate torso")
        // the instant an owl died. Any future death clip must stick to this model's bone names.
        StandardAnimation deathFalling = new StandardAnimation("death_falling",
                new AnimSource(() -> OwlAnimation.FALL), Loop.REPEATING, 0, 0, 0.5F);
        StandardAnimation hitGround = new StandardAnimation("hit_ground",
                new AnimSource(() -> OwlAnimation.HIT), Loop.PLAY_ONCE, 0, 0, 1.0F);
        // One-shot "waking up" transition played right after spawning from the Owl Statue (see
        // bondTo()/tick()): grounded, eyes-closed/folded → open/extended, then the owl takes flight.
        // Triggered imperatively (animator.play(...)) rather than by play condition — same pattern as
        // dive_attack_return — since it needs to run exactly once, not restart every tick it's eligible.
        StandardAnimation awake = new StandardAnimation("awake",
                new AnimSource(() -> OwlAnimation.AWAKE), Loop.PLAY_ONCE, 0, 0, 2.7917F);
        // Sonic screech — triggered imperatively from performSonicAttack(), same as awake/dive_return.
        // Length is SONIC_TOTAL_TICKS expressed in seconds: the counter and the clip must agree or the
        // clip gets cut short (or the owl hangs braked after it visually ends).
        // TODO: swap the AnimSource for the owl's own SONIC_SCREECH clip once it is exported from
        // Blockbench (bone names must match CopperOwlModel's — that is what crashed death_falling).
        // DIVE_ATTACK stands in only so the mechanic is testable: it is a looping 0.517 s pose, so the
        // owl just holds the dive pose flapping through the screech for now.
        StandardAnimation sonic = new StandardAnimation("sonic_screech",
                new AnimSource(() -> OwlAnimation.DIVE_ATTACK), Loop.PLAY_ONCE, 0, 0, SONIC_TOTAL_TICKS / 20.0F);
        // Pin the exact tick count: BaseAnimation's seconds→ticks conversion truncates (1.3F * 20
        // lands just under 26), and this has to equal SONIC_TOTAL_TICKS or the clip and the state
        // counter drift a tick apart.
        sonic.setDurationTicks(SONIC_TOTAL_TICKS);
        sonic.blendInMs(120).blendOutMs(200);
        // NOTE: OwlAnimation.UNACTIVE (the folded-wings/closed-eyes resting pose AWAKE transitions out
        // of) is NOT registered here — perching on the owner's arm intentionally shows no animation at
        // all (the model's bare rest pose). UNACTIVE is reserved for the Owl Statue block's own static
        // visual (a separate, not-yet-built renderer), not this entity.

        idleFly.blendInMs(350).blendOutMs(250);
        flySprint.playbackSpeed(1.2F);
        flySprint.blendInMs(250).blendOutMs(250);
        diveAttack.blendInMs(100).blendOutMs(250);
        // blockAdditive on both: the rig runs a look-at additive on the head (see CopperOwlModel's
        // Rig), and without this a dead owl keeps turning to track the player all the way down and
        // after it lands. Same reasoning as AthenianEntity's deaths. Note the arpy does NOT do this
        // and has the same latent problem — copying it verbatim would have copied the bug.
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

        // The screech itself: a 3D ray along the pilot's aim, born at the beak, cut short by terrain,
        // firing on ONE tick — an instantaneous boom, not a sweep, so the hit lands exactly when the
        // sound and the particles do. Credit goes to the owl (sonicBoom(attacker)), not its pilot: the
        // bird is what the enemy sees and should aggro onto.
        HitWindow.of(SONIC_RELEASE_TICK, SONIC_RELEASE_TICK)
                .shape(AttackShape.beam(SONIC_RANGE, SONIC_RADIUS))
                .anchor(AttackAnchor.look(SONIC_ORIGIN_FORWARD, 0.0F, 0.0F))
                // NOT aimAlongLook(): that reads attacker.getViewVector(), which depends on getXRot() —
                // and vanilla's default LookControl resets this mob's xRot every tick (see
                // pilotLookVector()'s javadoc). facing() with the pilot's own raw aim is the fix; it's
                // also exactly what already steers the owl's flight, so the beam can't ever point
                // somewhere the owl itself isn't actually looking.
                .facing(e -> this.sonicAimVector())
                .clipToBlocks()
                .damage(SONIC_DAMAGE)
                .poiseDamage(SONIC_POISE_DAMAGE)
                .knockback(0.0F)   // the radial default is wrong for a ray — see onHit below
                .damageSource(e -> e.damageSources().sonicBoom(e))
                // Never the pilot's own defenceless body standing on the ground, and never another owl.
                .filter(t -> !(t instanceof OwlEntity) && t.getId() != this.getControllerId())
                .onSweep((attacker, origin, facing, shape, hits) -> {
                    // Draws the beam that was actually TESTED — already shortened at the wall by
                    // clipToBlocks — so the visual can never claim reach the hitbox doesn't have.
                    if (!(attacker.level() instanceof ServerLevel server)
                            || !(shape instanceof AttackShape.Beam beam)) {
                        return;
                    }
                    Vec3 end = origin.add(facing.normalize().scale(beam.length()));
                    // longDistance: the owl can be sent a long way off with the spyglass, and the
                    // default 32-block particle cut-off would make its screech invisible from exactly
                    // the range you use a spyglass at.
                    ParticleFx.beam(server, MythosMortalsRegistry.OWL_BOOM.get(), origin, end,
                            SONIC_PARTICLE_SPACING, SONIC_PARTICLE_SKIP, true);
                    attacker.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.5F);
                    // Recoil for the pilot only: the camera IS the owl, so this reads as the shout
                    // kicking back. Shaking the whole level would rattle players nowhere near it.
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
                    // Push along the ray's own axis rather than radially away from the owl: a beam
                    // shoves things down its length, and from above that includes driving them into
                    // the ground (hence the smaller vertical term).
                    double resist = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                    // NOT attacker.getViewVector() — same reason as the facing() override above,
                    // and the same aim source so push and beam can never disagree.
                    Vec3 dir = this.sonicAimVector();
                    target.push(dir.x * SONIC_KNOCKBACK_H * resist,
                            dir.y * SONIC_KNOCKBACK_V * resist,
                            dir.z * SONIC_KNOCKBACK_H * resist);
                    // Entity#push alone never reaches a player's client (their movement is
                    // client-authoritative); hurtMarked is what forces the velocity to be sent.
                    target.hurtMarked = true;
                })
                .applyTo(sonic);

        // Charge cue at the start of the windup, so the release is telegraphed rather than instant.
        sonic.onFrame(0, e -> e.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.6F, 1.6F));

        // No ground/takeoff/landing state anymore (always airborne — see tick()), so these two
        // conditions fully cover the cruise pose: idle_fly while roughly stationary (holding
        // position near the player or paused mid-orbit), fly_sprint while actually moving
        // (chasing/orbiting/possessed flight). Both stand down while perched, so nothing plays and
        // the model just sits at its bare rest pose on the owner's arm — by design (see AWAKE above).
        // isScreeching() is in both conditions for the same reason isDiving() is: in this entity the
        // exclusion between clips is done by play condition, NOT by layer priority (idle sits at
        // priority 3 and the attacks at 0, and MobAnimator#play only stops animations at priority
        // <= the incoming one — so idle would happily keep playing underneath the screech).
        idleFly.setPlayCondition(anim -> !this.isDiving() && !this.isScreeching()
                && !this.isPerched() && !this.isFlyingMoving());
        flySprint.setPlayCondition(anim -> !this.isDiving() && !this.isScreeching()
                && !this.isPerched() && this.isFlyingMoving());
        diveAttack.setPlayCondition(anim -> this.isDiving());

        this.animator.register(idleFly, flySprint, diveAttack, diveReturn, awake, sonic);
        // Deliberately NOT gated with a play condition, unlike the arpy's death_falling. The arpy
        // gates on isFlying() because it has two ground-death variants to select instead; the owl
        // has only this one, so gating it would leave deaths on the ground with no animation at all.
        // Ungated costs nothing: MobAnimator checks onGround() every tick, so an owl that dies
        // already standing skips the fall stage on the next tick and goes straight to the impact —
        // which reads as collapsing.
        this.animator.registerFallingDeath(deathFalling, hitGround);
    }
}
