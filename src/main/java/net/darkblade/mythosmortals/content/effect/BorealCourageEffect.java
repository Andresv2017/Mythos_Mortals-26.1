package net.darkblade.mythosmortals.content.effect;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * Valentía Boreal — el buff táctico del demo: movimiento a ritmo de Velocidad I más un +15% de daño
 * cuerpo a cuerpo con la Espada Xifos y la Lanza Dori (ese lado vive en {@link BorealCourageEvents}).
 *
 * <p><b>La velocidad es un modificador de atributo, no el efecto Speed anidado.</b> El requisito era
 * que en el HUD sólo se vea Valentía Boreal, y hay dos formas de conseguirlo:
 * <ul>
 *   <li>aplicar {@code MobEffects.SPEED} con {@code showIcon = false} desde aquí, o</li>
 *   <li>declarar el mismo modificador que declara el propio efecto Speed de vanilla
 *       (+20% {@code MOVEMENT_SPEED}, {@code ADD_MULTIPLIED_TOTAL}) sobre este efecto.</li>
 * </ul>
 * La segunda es la elegida porque la primera arrastra dos problemas reales: hay que re-sincronizar la
 * duración del Speed cada tick para que no sobreviva al buff, y al expirar habría que retirarlo —
 * borrando de paso la Velocidad legítima que el jugador tuviera de una poción. Con el modificador no
 * existe ningún segundo efecto: nada que ocultar, nada que limpiar, y el bonus desaparece solo cuando
 * el efecto termina. El precio del cambio es que un {@code hasEffect(MobEffects.SPEED)} de terceros
 * no ve este buff (sí lo ve cualquier lectura del atributo, que es lo que gobierna el movimiento).
 *
 * <p>El icono del HUD sale de {@code assets/deluxelib/textures/mob_effect/boreal_courage.png}: el
 * juego lo resuelve por el id de registro, así que ese nombre de archivo y el id deben coincidir.
 */
public class BorealCourageEffect extends MobEffect {

    /** Los 45 segundos de la spec, en ticks. Aplica siempre vía {@link #apply} para no repartir la
     * duración por el código. */
    public static final int DURATION_TICKS = 45 * 20;

    /** +15% al daño de estocada/embestida — lo consume {@link BorealCourageEvents}. */
    public static final float MELEE_DAMAGE_BONUS = 0.15F;

    /** El mismo número que usa el efecto Speed de vanilla para su nivel I. */
    private static final double SPEED_I_BONUS = 0.20;

    private static final Identifier SPEED_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "effect.boreal_courage");

    public BorealCourageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FE3F5);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, SPEED_I_BONUS,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Concede el buff completo durante los {@link #DURATION_TICKS} de la spec. */
    public static void apply(@NotNull LivingEntity target) {
        target.addEffect(new MobEffectInstance(MythosMortalsRegistry.BOREAL_COURAGE, DURATION_TICKS));
    }
}
