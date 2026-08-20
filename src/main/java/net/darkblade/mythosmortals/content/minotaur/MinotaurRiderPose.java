package net.darkblade.mythosmortals.content.minotaur;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Blockbench-exported "rider_pose" for a player mounted on the minotaur — authored on a
 * player-replica rig (bones {@code torso}, {@code head}, {@code right_arm}, {@code left_arm},
 * {@code right_leg}, {@code left_leg}). Applied to the real player {@code HumanoidModel} by
 * {@code HumanoidPoseApplier} from {@code MinotaurRenderer.applyRiderPose}; the rig model class
 * itself is never needed at runtime. To change the pose, re-pose the rig in Blockbench, re-export,
 * and replace this definition.
 */
public final class MinotaurRiderPose {

    public static final AnimationDefinition RIDER_POSE = AnimationDefinition.Builder.withLength(0.0F)
        .addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-77.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-77.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 17.5F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("right_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-2.0F, 1.0F, -2.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.5F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("left_leg", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(2.0F, 1.0F, -2.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    private MinotaurRiderPose() {}
}
