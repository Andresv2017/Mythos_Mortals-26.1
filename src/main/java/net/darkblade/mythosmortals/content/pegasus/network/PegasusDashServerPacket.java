package net.darkblade.mythosmortals.content.pegasus.network;

import net.darkblade.deluxelib.network.AbstractNetworkPacket;
import net.darkblade.deluxelib.network.ExtendedFriendlyByteBuf;
import net.darkblade.deluxelib.network.PacketSide;
import net.darkblade.deluxelib.network.ServerPacketContext;
import net.darkblade.deluxelib.network.Side;
import net.darkblade.mythosmortals.content.pegasus.PegasusEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Client → server: the rider asks for a Wind Surge.
 *
 * <p>Carries only the pegasus' id; every condition that makes the dash legal — ownership, being the
 * controlling rider, being airborne, cooldown — is re-checked server-side in
 * {@link PegasusEntity#tryDash}.
 */
@PacketSide(side = Side.SERVER)
public final class PegasusDashServerPacket extends AbstractNetworkPacket<PegasusDashServerPacket> {

    private int pegasusId;

    @Override
    protected void read(@NotNull ExtendedFriendlyByteBuf buf) {
        this.pegasusId = buf.readVarInt();
    }

    @Override
    protected void write(@NotNull ExtendedFriendlyByteBuf buf) {
        buf.writeVarInt(this.pegasusId);
    }

    @Override
    protected void executeServer(@NotNull ServerPacketContext context) {
        if (context.level == null || context.player == null) {
            return;
        }
        Entity entity = context.level.getEntity(this.pegasusId);
        if (entity instanceof PegasusEntity pegasus && context.player.getVehicle() == pegasus) {
            pegasus.tryDash(context.player);
        }
    }

    public PegasusDashServerPacket(int pegasusId) {
        this.pegasusId = pegasusId;
    }

    public PegasusDashServerPacket() {}
}
