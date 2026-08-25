package net.darkblade.mythosmortals.effect;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class BorealCourageEffect extends MobEffect {

    public static final int DURATION_TICKS = 45 * 20;

    public static final float MELEE_DAMAGE_BONUS = 0.15F;

    private static final double SPEED_I_BONUS = 0.20;

    private static final Identifier SPEED_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "effect.boreal_courage");

    public BorealCourageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FE3F5);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, SPEED_I_BONUS,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void apply(@NotNull LivingEntity target) {
        target.addEffect(new MobEffectInstance(MythosMortalsRegistry.BOREAL_COURAGE, DURATION_TICKS));
    }
}
