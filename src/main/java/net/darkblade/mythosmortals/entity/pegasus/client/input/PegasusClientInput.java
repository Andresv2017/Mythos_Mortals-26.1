package net.darkblade.mythosmortals.entity.pegasus.client.input;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

public final class PegasusClientInput {

    public static Input of(Player rider) {
        return rider instanceof LocalPlayer local ? local.input.keyPresses : Input.EMPTY;
    }

    private PegasusClientInput() {}
}
