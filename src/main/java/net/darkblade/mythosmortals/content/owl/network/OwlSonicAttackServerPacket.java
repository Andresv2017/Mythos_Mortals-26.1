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

/**
 * Client → server: the piloting player clicked the mouse wheel. The server fires the owl's sonic
 * screech — see {@link OwlEntity#performSonicAttack}. Sibling of {@link OwlAttackServerPacket} (the
 * left-click dive); kept as its own packet rather than an attack-id field so each input stays one
 * explicit message, same as the rest of the possession packets.
 *
 * <p>No aim or target is sent: the owl already mirrors the pilot's look every tick (see
 * {@code PossessedInputServerPacket}), so the beam's own HitWindow resolves where it points server-side.
 * Accepted only from the actual controller.
 */
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
