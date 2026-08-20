package net.darkblade.mythosmortals.content.owl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Client-side "what living thing is the player pointing at" raycast, shared by the two places the owl
 * needs it: marking a target while piloting (origin = the owl's eyes, since that is what you are
 * looking through) and ordering an attack with the spyglass (origin = the player's own eyes).
 *
 * <p>Forgiving on purpose, and <b>proportionally so</b>: each candidate's box is inflated by an amount
 * that grows with how far away it is, and the nearest hit along the ray wins. A fixed inflation looks
 * generous up close and turns into pixel-hunting at range — the angular error a fixed 1-block margin
 * tolerates halves every time the distance doubles, which is exactly backwards for a tool whose whole
 * purpose is picking things out far away.
 *
 * <p>Client-only by reachability, not by annotation: it touches {@link Minecraft}, and the only
 * callers are {@code Dist.CLIENT} event subscribers, so a server never loads it.
 */
public final class OwlAim {

    /** Aim forgiveness at zero distance, in blocks. */
    private static final double BASE_AIM_MARGIN = 0.75;
    /** Extra forgiveness per block of distance — {@code 0.02} keeps the tolerated angular error
     * roughly constant (~1.1°), so a mob at 90 blocks is about as easy to pick as one at 10. */
    private static final double AIM_MARGIN_PER_BLOCK = 0.02;

    /**
     * The living entity the local player is aiming at, or {@code null}.
     *
     * @param from  entity the ray starts at — the owl while piloting it, the player otherwise. Always
     *              excluded from the results, as is the player.
     * @param reach how far the ray travels, in blocks.
     */
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
            // Keep the tolerated aim error roughly constant in ANGLE rather than in blocks: a margin
            // that works at arm's length is invisible at ninety.
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
