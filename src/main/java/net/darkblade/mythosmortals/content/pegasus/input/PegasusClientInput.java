package net.darkblade.mythosmortals.content.pegasus.input;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

/**
 * Reads the local player's key state.
 *
 * <p>Deliberately a separate class from anything common: it references {@link LocalPlayer}, so it
 * must never be loaded on a dedicated server. The only caller reaches it after already ruling out
 * {@code ServerPlayer}, which cannot happen server-side.
 */
public final class PegasusClientInput {

    public static Input of(Player rider) {
        return rider instanceof LocalPlayer local ? local.input.keyPresses : Input.EMPTY;
    }

    private PegasusClientInput() {}
}
