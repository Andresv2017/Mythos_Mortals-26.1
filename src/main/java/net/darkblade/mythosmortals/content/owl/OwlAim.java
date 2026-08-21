package net.darkblade.mythosmortals.content.owl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public final class OwlAim {

    private static final double BASE_AIM_MARGIN = 0.75;
    private static final double AIM_MARGIN_PER_BLOCK = 0.02;


    public static @Nullable Entity findAimedLiving(Minecraft mc, Entity from, double reach) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        Vec3 eye = from.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        AABB search = from.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0);

        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity e : mc.level.getEntities(from, search,
                ent -> ent instanceof LivingEntity && ent.isAlive()
                        && ent != from && ent != player && ent.isPickable())) {
            double margin = BASE_AIM_MARGIN + Math.sqrt(eye.distanceToSqr(e.position())) * AIM_MARGIN_PER_BLOCK;
            Optional<Vec3> clip = e.getBoundingBox().inflate(margin).clip(eye, end);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = e;
                }
            }
        }
        return best;
    }

    private OwlAim() {}
}
