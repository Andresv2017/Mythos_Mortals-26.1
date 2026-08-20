package net.darkblade.mythosmortals.content.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

/**
 * La vid de uvas: dos bloques de alto, plantada sobre una columna de exactamente dos
 * {@link GrapeStakeBlock} que estén sobre {@code minecraft:farmland} — ver
 * {@code GrapeStakeBlock#tryPlantVine}.
 *
 * <p>Hereda de {@link DoublePlantBlock} y con eso se lleva gratis todo el emparejado de las dos
 * mitades: {@code updateShape} borra la otra mitad al romper una, {@code canSurvive} exige que la
 * inferior siga ahí, y {@code playerWillDestroy} suelta el botín una sola vez en lugar de dos. Nada
 * de eso hace falta reimplementarlo.
 *
 * <p>Lo único que añade sobre esa base es la edad ({@link #AGE}, 0..2) y el suelo válido
 * ({@link #mayPlaceOn}). El crecimiento, el hueso y la cosecha llegan aparte.
 *
 * <p><b>Las dos mitades siempre comparten edad.</b> Es una invariante del bloque, no una
 * coincidencia: quien cambie la edad tiene que escribir las dos posiciones en la misma operación.
 * El blockstate mapea las dos mitades al mismo modelo, así que una desincronización se vería como
 * una vid con la parte de arriba de otro color.
 */
public class GrapeVineBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<GrapeVineBlock> CODEC = simpleCodec(GrapeVineBlock::new);

    /** 0 = sólo hojas, 1 = racimos verdes, 2 = racimos morados y lista para cosechar. */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;

    public static final int MAX_AGE = 2;

    /** Mismo umbral que los cultivos de vanilla: por debajo de esto la vid no avanza. */
    private static final int MIN_GROW_LIGHT = 9;

    /** Uvas por cosecha, ambos extremos incluidos. */
    private static final int MIN_YIELD = 2;
    private static final int MAX_YIELD = 3;

    public GrapeVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(AGE, 0));
    }

    /** {@code public} y no {@code protected}: {@link DoublePlantBlock} ya lo declara público y Java
     * no deja reducir visibilidad al sobrescribir. En {@link GrapeStakeBlock}, que hereda de
     * {@code Block} a secas, sí va {@code protected}. */
    @Override
    public @NotNull MapCodec<? extends GrapeVineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    /**
     * Suelo válido para la mitad inferior. {@link DoublePlantBlock#canSurvive} ya se encarga de la
     * superior (exige que la inferior siga debajo) y delega la inferior aquí a través de
     * {@code VegetationBlock#canSurvive}.
     *
     * <p>Acepta toda la etiqueta {@code #deluxelib:stake_placeable}, no sólo farmland: el farmland
     * se exige al <b>plantar</b>, pero una vez plantada la vid sobrevive aunque alguien pisotee la
     * tierra de cultivo y la vuelva tierra normal. Perder la cosecha por caminar por encima sería
     * un castigo que nadie pidió.
     */
    @Override
    protected boolean mayPlaceOn(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return state.is(GrapeStakeBlock.STAKE_PLACEABLE);
    }

    /** Pick-block da uvas, que es con lo que se planta. */
    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                   @NotNull BlockState state, boolean includeData) {
        return new ItemStack(MythosMortalsItems.GRAPES.get());
    }

    public int getAge(BlockState state) {
        return state.getValue(AGE);
    }

    // ---------------------------------------------------------------- crecimiento

    /**
     * Sólo la mitad inferior tickea. La superior no tiene nada que decidir: cuando la inferior sube
     * de edad escribe las dos, así que darle su propio tick sería tirar la moneda dos veces por la
     * misma planta y duplicarle la velocidad de crecimiento sin querer.
     */
    @Override
    protected boolean isRandomlyTicking(@NotNull BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && getAge(state) < MAX_AGE;
    }

    /**
     * Mismo criterio que {@code CropBlock#randomTick}: luz cruda ≥ 9, una tirada cuyo peso depende
     * del suelo de alrededor, y los dos ganchos de NeoForge para que el gamerule
     * {@code randomTickSpeed} y los mods que escuchan el crecimiento sigan mandando.
     */
    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level,
                              @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < MIN_GROW_LIGHT) {
            return;
        }
        int age = getAge(state);
        if (age >= MAX_AGE) {
            return;
        }
        float speed = growthSpeed(level, pos);
        boolean rolled = random.nextInt((int) (25.0F / speed) + 1) == 0;
        if (CommonHooks.canCropGrow(level, pos, state, rolled)) {
            setAge(level, pos, state, age + 1);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    /**
     * Copia de {@code CropBlock#getGrowthSpeed}, no una llamada a ella: allí es
     * {@code protected static}, o sea accesible sólo por herencia, y esta clase desciende de
     * {@link DoublePlantBlock}, no de {@code CropBlock}. Replicar veinte líneas sale más barato que
     * un access transformer para toda la instalación.
     *
     * <p>Lo que mide: el bloque de debajo suma 1 (3 si está hidratado), y cada uno de los ocho
     * vecinos de ese bloque suma un cuarto de lo mismo. Encima, plantas iguales pegadas en cruz o en
     * diagonal parten la velocidad por dos — es lo que premia plantar en hileras separadas en vez de
     * en bloque macizo, igual que con el trigo.
     */
    private float growthSpeed(BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.below();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos soilPos = below.offset(dx, 0, dz);
                BlockState soil = level.getBlockState(soilPos);
                float soilSpeed = 0.0F;

                TriState decision = soil.canSustainPlant(level, soilPos, Direction.UP, soil);
                if (decision.isDefault() ? soil.is(BlockTags.GROWS_CROPS) : decision.isTrue()) {
                    soilSpeed = soil.isFertile(level, soilPos) ? 3.0F : 1.0F;
                }
                if (dx != 0 || dz != 0) {
                    soilSpeed /= 4.0F;
                }
                speed += soilSpeed;
            }
        }

        boolean alongX = isSameVine(level, pos.west()) || isSameVine(level, pos.east());
        boolean alongZ = isSameVine(level, pos.north()) || isSameVine(level, pos.south());
        if (alongX && alongZ) {
            return speed / 2.0F;
        }
        boolean diagonal = isSameVine(level, pos.west().north())
            || isSameVine(level, pos.east().north())
            || isSameVine(level, pos.east().south())
            || isSameVine(level, pos.west().south());
        return diagonal ? speed / 2.0F : speed;
    }

    private boolean isSameVine(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    // ---------------------------------------------------------------- hueso

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return getAge(state) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random,
                                     @NotNull BlockPos pos, @NotNull BlockState state) {
        return true;
    }

    /**
     * Un stage por hueso, no el salto de 2..5 que da {@code CropBlock}: con sólo tres estados, ese
     * salto llevaría casi siempre de 0 a maduro de un golpe y el crecimiento dejaría de verse.
     *
     * <p>Funciona clicando cualquiera de las dos mitades: {@link #setAge} normaliza a la inferior.
     */
    @Override
    public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random,
                                @NotNull BlockPos pos, @NotNull BlockState state) {
        setAge(level, pos, state, Math.min(MAX_AGE, getAge(state) + 1));
    }

    // ---------------------------------------------------------------- cosecha

    /**
     * Cosecha a mano: click derecho sobre cualquiera de las dos mitades madura suelta uvas y
     * devuelve la planta a edad 0. La cepa se queda — es un cultivo perenne, no hay que replantar.
     *
     * <p>Con la vid sin madurar devuelve {@code PASS} a propósito, y eso es justo lo que deja que el
     * hueso funcione: {@code useItemOn} encadena aquí, y al no consumir la acción la cadena sigue
     * hasta el {@code useOn} del ítem en mano. Por eso no hace falta interceptar el hueso a mano,
     * como sí hace {@code SweetBerryBushBlock}.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult hitResult) {
        if (getAge(state) < MAX_AGE) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockPos lowerPos = lowerPosOf(pos, state);
            int yield = MIN_YIELD + serverLevel.getRandom().nextInt(MAX_YIELD - MIN_YIELD + 1);
            Block.popResource(serverLevel, lowerPos, new ItemStack(MythosMortalsItems.GRAPES.get(), yield));
            serverLevel.playSound(null, lowerPos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            setAge(serverLevel, pos, state, 0);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, lowerPos,
                GameEvent.Context.of(player, serverLevel.getBlockState(lowerPos)));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Escribe una edad nueva en las <b>dos</b> mitades, dada la posición de cualquiera de ellas.
     * Único punto por el que debe pasar cualquier cambio de edad — ver la invariante en el javadoc
     * de la clase.
     */
    public void setAge(Level level, BlockPos pos, BlockState state, int age) {
        BlockPos lowerPos = lowerPosOf(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(lowerPos.above());
        if (!lower.is(this) || !upper.is(this)) {
            // Media vid: alguien la rompió entre el click y esto. DoublePlantBlock ya se encargará
            // de limpiar la mitad huérfana, así que aquí no hay nada que escribir.
            return;
        }
        level.setBlock(lowerPos, lower.setValue(AGE, age), Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos.above(), upper.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    /** La posición de la mitad inferior, se haya clicado la que se haya clicado. */
    public static BlockPos lowerPosOf(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
