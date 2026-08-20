package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * El Ánfora Griega con producto dentro — vino o aceite de oliva — servida a botellas.
 *
 * <p><b>Una sola clase para los dos productos.</b> Lo único que los distingue es qué botella
 * reparten, así que eso es lo que se pasa al constructor. Un {@link Supplier} y no el {@link Item}
 * directo porque el bloque se construye durante el {@code RegisterEvent} de bloques, cuando los
 * ítems todavía no existen: resolverlo ahí sería un {@code null}.
 *
 * <p>Cada click derecho con una botella de cristal vacía entrega una botella llena y gasta una
 * ración de {@link #SERVINGS}. Al gastar la última, el bloque vuelve a ser un
 * {@link GreekAmphoraBlock} vacío en vez de desaparecer: <b>la vasija se conserva</b>. El ánfora es
 * cerámica reutilizable; lo que se consume es el contenido, no el barro.
 *
 * <p><b>Del bloque colocado no se bebe.</b> Embotellar es lo único que hace aquí. Para beber, el
 * ánfora se coge y se bebe en la mano como una botella de agua — con su animación y su aguantar el
 * click — y eso vive en las propiedades del {@code BlockItem} más
 * {@link WineEvents#onUseItemFinish}, no en este bloque. Hubo una versión que bebía por
 * {@code useWithoutItem} y se retiró: no había animación, y como {@code useWithoutItem} se llama
 * lleves lo que lleves, obligaba a una guarda de mano vacía para no beberse el vino cada vez que
 * intentabas colocar un bloque apoyado en el ánfora.
 *
 * <p><b>Las raciones sobreviven al pico.</b> No por código de aquí, sino por la loot table, que usa
 * {@code minecraft:copy_state} sobre {@link #SERVINGS} para grabarlas en el componente
 * {@code block_state} del ítem que cae; el {@code BlockItem} las restaura al recolocar. Sin eso,
 * romper un ánfora a medio servir y volver a ponerla la devolvería llena, que es vino infinito.
 */
public class FilledAmphoraBlock extends Block {

    public static final MapCodec<FilledAmphoraBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("product").forGetter(block -> block.product.get()),
        propertiesCodec()
    ).apply(i, (product, properties) -> new FilledAmphoraBlock(() -> product, properties)));

    /** Cuántas botellas quedan por servir. Empieza en {@link #MAX_SERVINGS} y nunca llega a 0: al
     * servir la última, el bloque se sustituye por el ánfora vacía. */
    public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 1, 4);

    public static final int MAX_SERVINGS = 4;

    private final Supplier<Item> product;

    public FilledAmphoraBlock(Supplier<Item> product, BlockBehaviour.Properties properties) {
        super(properties);
        this.product = product;
        registerDefaultState(getStateDefinition().any().setValue(SERVINGS, MAX_SERVINGS));
    }

    @Override
    public @NotNull MapCodec<? extends FilledAmphoraBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(SERVINGS);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                   @NotNull Level level, @NotNull BlockPos pos,
                                                   @NotNull Player player, @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hitResult) {
        if (!itemStack.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // createFilledResult resuelve de una vez los tres casos molestos: en creativo no gasta la
        // botella vacía y sólo añade la llena si no la tienes ya; con un stack de una sola botella
        // devuelve la llena para ponerla en la mano; y con un stack mayor la mete al inventario o la
        // tira al suelo si no cabe. Es el mismo helper que usan la botella de miel y el cubo.
        player.setItemInHand(hand,
            ItemUtils.createFilledResult(itemStack, player, new ItemStack(product.get())));

        takeServing(state, level, pos);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    /** Descuenta una ración. Al gastar la última deja el ánfora vacía en su sitio — la vasija se
     * conserva, lo que se acaba es el contenido. */
    private static void takeServing(BlockState state, Level level, BlockPos pos) {
        int left = state.getValue(SERVINGS);
        if (left > 1) {
            level.setBlock(pos, state.setValue(SERVINGS, left - 1), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, MythosMortalsRegistry.GREEK_AMPHORA.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
