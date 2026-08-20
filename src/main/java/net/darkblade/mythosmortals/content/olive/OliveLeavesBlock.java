package net.darkblade.mythosmortals.content.olive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

/**
 * Las hojas del olivo.
 *
 * <p>Existe esta clase porque en 26.1 {@link LeavesBlock} <b>es abstracta</b>: la novedad de las
 * hojas que caen metió dos métodos sin implementar, {@code codec()} y
 * {@link #spawnFallingLeavesParticle}. Las dos concretas de vanilla no sirven —
 * {@code TintedParticleLeavesBlock} tinta la partícula con el color del bioma y
 * {@code UntintedParticleLeavesBlock} exige un {@code ParticleOptions}, y las partículas de hoja de
 * vanilla vienen tintadas o en color cereza.
 *
 * <p>Así que el olivo no suelta hojas al aire: es un árbol seco y quieto. La probabilidad de
 * partícula va a {@code 0.0F} en el constructor <b>y</b> el método se deja vacío; lo primero ya
 * bastaría, pero un cero suelto se lee como un número por ajustar en vez de como una decisión.
 *
 * <p><b>Ojo con el tinte.</b> Este bloque se dibuja con el color literal de su textura, y eso es
 * deliberado, no un olvido. Las hojas de vanilla llevan la textura en escala de grises y reciben el
 * color del bioma desde un {@code BlockColor} registrado en {@code BlockColors.createDefault()} para
 * una lista fija de bloques. {@code olive_leaves.png} ya trae su verde oliva pintado. Como el olivo
 * genera <b>en llanuras y en sabana</b>, tintarlo lo pintaría de dos colores distintos según dónde
 * cayera y ninguno sería el dibujado. Para conseguirlo no hay que hacer nada — sólo no registrar un
 * color handler. Si alguna vez las hojas se ven verde lima en llanura y pardas en sabana, es que
 * alguien "arregló" esto.
 */
public class OliveLeavesBlock extends LeavesBlock {
    public static final MapCodec<OliveLeavesBlock> CODEC = simpleCodec(OliveLeavesBlock::new);

    public OliveLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.0F, properties);
    }

    /** {@code public}: {@code LeavesBlock#codec()} lo declara público y Java no deja reducir
     * visibilidad al sobrescribir. */
    @Override
    public @NotNull MapCodec<? extends OliveLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(@NotNull Level level, @NotNull BlockPos pos,
                                              @NotNull RandomSource random) {
        // Sin partículas a propósito — ver el javadoc de la clase.
    }
}
