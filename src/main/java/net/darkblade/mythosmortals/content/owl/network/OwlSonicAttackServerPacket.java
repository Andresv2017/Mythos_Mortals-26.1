package net.darkblade.mythosmortals.content.owl.network;

import net.darkblade.deluxelib.entity.possession.Possessable;
import net.darkblade.deluxelib.network.AbstractNetworkPacket;
import net.darkblade.deluxelib.network.ExtendedFriendlyByteBuf;
import net.darkblade.deluxelib.network.PacketSide;
import net.darkblade.deluxelib.network.ServerPacketContext;
import net.darkblade.deluxelib.network.Side;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

@PacketSide(side = Side.SERVER)
public final class OwlSonicAttackServerPacket extends AbstractNetworkPacket<OwlSonicAttackServerPacket> {
    private int owlId;

    @Override
    protected void read(@NotNull ExtendedFriendlyByteBuf buf) {
        this.owlId = buf.readVarInt();
    }

    @Override
    protected void write(@NotNull ExtendedFriendlyByteBuf buf) {
        buf.writeVarInt(this.owlId);
    }

    @Override
    protected void executeServer(@NotNull ServerPacketContext context) {
        ServerLevel level = context.level;
        if (level == null || context.player == null) {
            return;
        }
        Possessable possessed = Possessable.controlledBy(level, context.player, this.owlId);
        if (possessed instanceof OwlEntity owl) {
            owl.performSonicAttack();
        }
    }

    public OwlSonicAttackServerPacket(int owlId) {
        this.owlId = owlId;
    }

    public OwlSonicAttackServerPacket() {}
}
