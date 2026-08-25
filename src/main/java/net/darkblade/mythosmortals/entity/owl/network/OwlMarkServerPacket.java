package net.darkblade.mythosmortals.entity.owl.network;

import net.darkblade.deluxelib.entity.possession.Possessable;
import net.darkblade.deluxelib.network.AbstractNetworkPacket;
import net.darkblade.deluxelib.network.ExtendedFriendlyByteBuf;
import net.darkblade.deluxelib.network.PacketSide;
import net.darkblade.deluxelib.network.ServerPacketContext;
import net.darkblade.deluxelib.network.Side;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;


@PacketSide(side = Side.SERVER)
public final class OwlMarkServerPacket extends AbstractNetworkPacket<OwlMarkServerPacket> {

    private static final int GLOW_DURATION_TICKS = 300;   // 15 s

    private int owlId;
    private int targetId;

    @Override
    protected void read(@NotNull ExtendedFriendlyByteBuf buf) {
        this.owlId = buf.readVarInt();
        this.targetId = buf.readVarInt();
    }

    @Override
    protected void write(@NotNull ExtendedFriendlyByteBuf buf) {
        buf.writeVarInt(this.owlId);
        buf.writeVarInt(this.targetId);
    }

    @Override
    protected void executeServer(@NotNull ServerPacketContext context) {
        ServerLevel level = context.level;
        if (level == null || context.player == null) {
            return;
        }
        Possessable possessed = Possessable.controlledBy(level, context.player, this.owlId);
        Entity targetEntity = level.getEntity(this.targetId);
        if (possessed instanceof OwlEntity owl
                && targetEntity instanceof LivingEntity target
                && target.isAlive()
                && target != owl) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false, false));
        }
    }

    public OwlMarkServerPacket(int owlId, int targetId) {
        this.owlId = owlId;
        this.targetId = targetId;
    }

    public OwlMarkServerPacket() {}
}
