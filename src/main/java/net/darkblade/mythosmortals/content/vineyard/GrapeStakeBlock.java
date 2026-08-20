package net.darkblade.mythosmortals.content.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * El poste del viñedo: el tutor de palo sobre el que crece la vid de uvas.
 *
 * <p>No tiene BlockItem. La única forma de colocarlo es click derecho con un {@link Items#STICK}
 * de vanilla: sobre tierra lo pone {@link VineyardInteractions} (la tierra es de vanilla y no
 * podemos meterle código), y sobre otro poste lo pone {@link #useItemOn} aquí abajo. Al romperlo
 * devuelve el palo, así que colocarlo nunca cuesta material de verdad.
 *
 * <p>Dos postes apilados sobre {@code minecraft:farmland} son la condición para plantar. Se permite
 * apilar un tercero y más: no sirven para plantar, pero prohibirlos obligaría a explicar por qué
 * justo ahí no se puede poner un palo, y como decoración (una empalizada, un emparrado) no estorban
 * a nadie.
 */
public class GrapeStakeBlock extends Block {
    public static final MapCodec<GrapeStakeBlock> CODEC = simpleCodec(GrapeStakeBlock::new);

    /**
     * Suelo válido para el poste: {@code #minecraft:dirt} + {@code #minecraft:grass_blocks} +
     * {@code minecraft:farmland}. La vid exige además que sea farmland al plantar, pero una vez
     * plantada le basta con esta etiqueta — así pisotear el farmland no mata el viñedo.
     *
     * <p>Hacen falta las <b>dos</b> etiquetas de vanilla: en 26.1 {@code #minecraft:dirt} es sólo
     * {@code dirt / coarse_dirt / rooted_dirt}, y el bloque de hierba vive aparte en
     * {@code #minecraft:grass_blocks} (con podzol y micelio). Pedir sólo la primera dejaba el poste
     * sin poder colocarse en hierba, que es el caso normal.
     */
    public static final TagKey<Block> STAKE_PLACEABLE =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "stake_placeable"));

    /** Postecillo de 4×4 centrado, del suelo al techo del bloque: lo que dibuja la textura. */
    private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 16.0);

    /** Altura exacta de columna que la uva convierte en vid. La vid mide dos bloques. */
    private static final int VINE_STAKES = 2;

    /**
     * Cuántos bloques como mucho se baja buscando el pie de la columna. Cualquier columna más alta
     * ya es inválida para plantar, así que no hace falta recorrerla entera: esto acota el coste del
     * click derecho sobre una empalizada decorativa de cien palos.
     */
    private static final int MAX_COLUMN_SCAN = VINE_STAKES + 1;

    public GrapeStakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends GrapeStakeBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                           @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(STAKE_PLACEABLE);
    }

    /**
     * Lo que hace que quitar el suelo tire el poste. {@link #canSurvive} por sí solo <b>no</b>
     * basta: el {@code updateShape} de {@link Block} devuelve el estado tal cual y no consulta
     * {@code canSurvive} nunca, así que sin este override el poste se queda flotando. Es el mismo
     * override que hace {@code VegetationBlock} para las plantas de vanilla.
     *
     * <p>Devolver aire aquí no es sólo borrar: {@code Block.updateOrDestroy} ve el aire y llama a
     * {@code destroyBlock} con drops, así que el palo cae. Y como el poste que se va es a su vez el
     * soporte del de arriba, la columna entera se desmonta en cascada, cada uno soltando su palo.
     */
    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull LevelReader level,
                                              @NotNull ScheduledTickAccess ticks, @NotNull BlockPos pos,
                                              @NotNull Direction directionToNeighbour, @NotNull BlockPos neighbourPos,
                                              @NotNull BlockState neighbourState, @NotNull RandomSource random) {
        return !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    /** Pick-block sobre un poste da un palo, que es con lo que se coloca. */
    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                   @NotNull BlockState state, boolean includeData) {
        return new ItemStack(Items.STICK);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                   @NotNull Level level, @NotNull BlockPos pos,
                                                   @NotNull Player player, @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hitResult) {
        if (itemStack.is(Items.STICK)) {
            return tryStackStake(itemStack, level, pos, player);
        }
        if (itemStack.is(MythosMortalsItems.GRAPES.get())) {
            return tryPlantVine(itemStack, level, pos, player);
        }
        // TRY_WITH_EMPTY_HAND y no PASS: deja que la cadena siga hasta useWithoutItem y, si nadie
        // consume, hasta el useOn del ítem en mano. Es el valor por defecto de BlockBehaviour.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * Convierte una columna de <b>exactamente dos</b> postes sobre farmland en una
     * {@link GrapeVineBlock} de edad 0. Funciona clicando cualquiera de los dos.
     *
     * <p>Si la columna no mide dos, o no hay farmland debajo, devuelve {@code PASS} sin gastar la
     * uva: plantar mal no debe costar semilla. Tampoco se avisa por chat — el viñedo se explica
     * solo probando, y un mensaje de error por cada click fallido sería ruido.
     */
    private InteractionResult tryPlantVine(ItemStack itemStack, Level level, BlockPos pos, Player player) {
        BlockPos bottom = findColumnBottom(level, pos);
        if (bottom == null || columnHeight(level, bottom) != VINE_STAKES) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(bottom.below()).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState vine = MythosMortalsRegistry.GRAPE_VINE.get().defaultBlockState();

        // Vaciar el poste de arriba ANTES, y con UPDATE_CLIENTS (sin avisos a vecinos), no es un
        // detalle de limpieza: es lo que hace que plantar funcione.
        //
        // placeAt escribe la mitad inferior primero. Si el poste de arriba sigue puesto en ese
        // momento y el cambio va con UPDATE_ALL, ese poste recibe el aviso, mira debajo, ya no
        // encuentra ni poste ni suelo válido y se autodestruye soltando su palo. Y su destrucción
        // avisa de vuelta a la mitad inferior recién puesta, que mira arriba, ve aire en vez de su
        // otra mitad, y DoublePlantBlock.updateShape la borra también. La vid acababa existiendo
        // sólo de cintura para arriba.
        //
        // Vaciando primero, el estado intermedio que ve la lógica de vecinos es "aire arriba", que
        // no le molesta a nadie, y placeAt puede escribir sus dos mitades en paz.
        level.setBlock(bottom.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        DoublePlantBlock.placeAt(level, vine, bottom, Block.UPDATE_ALL);

        itemStack.consume(1, player);
        announcePlacement(level, bottom, level.getBlockState(bottom), player);
        return InteractionResult.SUCCESS;
    }

    /**
     * Baja hasta el poste más bajo de la columna que contiene {@code pos}. Devuelve {@code null} si
     * la columna es más alta de lo que cualquier viñedo válido puede ser: así un jugador que apile
     * una empalizada de 200 palos no hace que cada click derecho recorra 200 bloques.
     */
    private @Nullable BlockPos findColumnBottom(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        for (int step = 0; step < MAX_COLUMN_SCAN; step++) {
            if (!level.getBlockState(bottom.below()).is(this)) {
                return bottom;
            }
            bottom = bottom.below();
        }
        return null;
    }

    /** Cuenta postes hacia arriba desde {@code bottom}, parando en cuanto se pasa de lo válido. */
    private int columnHeight(Level level, BlockPos bottom) {
        int height = 0;
        BlockPos cursor = bottom;
        while (height <= VINE_STAKES && level.getBlockState(cursor).is(this)) {
            height++;
            cursor = cursor.above();
        }
        return height;
    }

    /** Coloca otro poste justo encima de éste. */
    private InteractionResult tryStackStake(ItemStack itemStack, Level level, BlockPos pos, Player player) {
        BlockPos above = pos.above();
        if (!level.isInsideBuildHeight(above) || !level.getBlockState(above).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState placed = this.defaultBlockState();
        level.setBlock(above, placed, Block.UPDATE_ALL);
        itemStack.consume(1, player);
        announcePlacement(level, above, placed, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * Sonido de colocación y {@code GameEvent} para un bloque del viñedo recién puesto: lo que hace
     * vanilla al final de {@code BlockItem#place}, que aquí no pasa porque ninguno de estos bloques
     * se coloca con un BlockItem. Los mismos números que usa vanilla, para que suene igual que
     * poner cualquier otro bloque.
     *
     * <p>Compartido por los tres sitios que colocan algo: el primer poste desde
     * {@link VineyardInteractions}, el apilado desde {@link #tryStackStake} y la vid desde
     * {@link #tryPlantVine}. Cada uno saca el sonido del {@code SoundType} del bloque que puso, así
     * que el poste suena a madera y la vid a cultivo sin que este método sepa cuál es cuál.
     */
    static void announcePlacement(Level level, BlockPos pos, BlockState placed, @Nullable Player player) {
        SoundType sound = placed.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
    }
}
