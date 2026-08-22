package net.darkblade.mythosmortals.content.pegasus;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Seated pose for a player riding the pegasus — legs straddling the barrel, hands forward on the
 * reins. Authored on the same player-replica rig as {@code MinotaurRiderPose} (bones {@code torso},
 * {@code head}, {@code right_arm}, {@code left_arm}, {@code right_leg}, {@code left_leg}) and applied
 * to the real {@code HumanoidModel} by {@code HumanoidPoseApplier} from {@link PegasusRenderer}.
 *
 * <p>To change it, pose the rig in Blockbench, re-export, and replace the definition below — or nudge
 * it live in-game with {@code /deluxelib debug riderpose mythosmortals:pegasus} and paste the printed
 * values here.
 */
public final class PegasusRiderPose {

    public static final AnimationDefinition RIDER_POSE = AnimationDefinition.Builder.withLength(0.0F)
            .addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-62.5F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-62.5F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(-2.0F, 1.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(2.0F, 1.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();

    private PegasusRiderPose() {}
}
