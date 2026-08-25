package net.darkblade.mythosmortals.entity.owl.network;

import net.darkblade.deluxelib.network.AbstractNetworkPacket;
import net.darkblade.deluxelib.network.ExtendedFriendlyByteBuf;
import net.darkblade.deluxelib.network.PacketSide;
import net.darkblade.deluxelib.network.ServerPacketContext;
import net.darkblade.deluxelib.network.Side;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;


@PacketSide(side = Side.SERVER)
public final class OwlOrderAttackServerPacket extends AbstractNetworkPacket<OwlOrderAttackServerPacket> {


    private static final double OWL_SEARCH_RANGE = 64.0;
    private static final double MAX_TARGET_RANGE = 128.0;

    private int targetId;

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
        if (!(aimed instanceof LivingEntity target)
                || target.distanceToSqr(player) > MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(OWL_SEARCH_RANGE);
        for (OwlEntity owl : level.getEntitiesOfClass(OwlEntity.class, area,
                o -> o.isAlive() && player.getUUID().equals(o.getOwnerUUID()))) {
            if (owl.orderAttack(player, target)) {
                return;
            }
        }
    }
}
