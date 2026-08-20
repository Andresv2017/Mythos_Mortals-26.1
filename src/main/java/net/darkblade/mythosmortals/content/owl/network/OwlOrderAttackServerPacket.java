package net.darkblade.mythosmortals.content.owl.network;

import net.darkblade.deluxelib.network.AbstractNetworkPacket;
import net.darkblade.deluxelib.network.ExtendedFriendlyByteBuf;
import net.darkblade.deluxelib.network.PacketSide;
import net.darkblade.deluxelib.network.ServerPacketContext;
import net.darkblade.deluxelib.network.Side;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * Client → server: the owner pointed the spyglass at something and told their owl to go for it.
 *
 * <p>Carries only the target's entity id, not the owl's — the server finds the player's own bonded
 * owl, the same "you invoke your companion, you don't click it" framing the possession keybind uses.
 *
 * <p>Demo content, so it lives in {@code test/} and is registered on the shared channel by
 * {@link MythosMortalsRegistry#registerPackets()} rather than by the library.
 */
@PacketSide(side = Side.SERVER)
public final class OwlOrderAttackServerPacket extends AbstractNetworkPacket<OwlOrderAttackServerPacket> {

    /** How far from the player the server looks for their owl. The owl follows its owner, so this
     * only needs to cover it being off mid-fight, not the distance to the target. */
    private static final double OWL_SEARCH_RANGE = 64.0;
    /** Furthest a designated target may be from the player. A sanity bound against a client naming
     * any loaded entity, not a gameplay limit — it sits comfortably past the client's own designation
     * reach so it never becomes the thing that stops a legitimate order. */
    private static final double MAX_TARGET_RANGE = 128.0;

    private int targetId;

    /** Required by {@code NetworkCreator.decodeFast}, which instantiates packets reflectively. */
    public OwlOrderAttackServerPacket() {}

    public OwlOrderAttackServerPacket(int targetId) {
        this.targetId = targetId;
    }

    @Override
    protected void read(@NotNull ExtendedFriendlyByteBuf buf) {
        this.targetId = buf.readVarInt();
    }

    @Override
    protected void write(@NotNull ExtendedFriendlyByteBuf buf) {
        buf.writeVarInt(this.targetId);
    }

    @Override
    protected void executeServer(@NotNull ServerPacketContext context) {
        ServerLevel level = context.level;
        ServerPlayer player = context.player;
        if (level == null || player == null) {
            return;
        }
        Entity aimed = level.getEntity(this.targetId);
        // Range-check server-side too: a client could otherwise name any loaded entity id at all.
        if (!(aimed instanceof LivingEntity target)
                || target.distanceToSqr(player) > MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(OWL_SEARCH_RANGE);
        for (OwlEntity owl : level.getEntitiesOfClass(OwlEntity.class, area,
                o -> o.isAlive() && player.getUUID().equals(o.getOwnerUUID()))) {
            if (owl.orderAttack(player, target)) {
                return;   // one owl, one order
            }
        }
    }
}
