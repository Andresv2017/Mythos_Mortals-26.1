package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;

/**
 * Render state carrying the pegasus' tack, so {@link PegasusEquipmentLayer} knows which texture
 * layers to draw without reaching back into the entity.
 */
public class PegasusRenderState extends DeluxeEntityRenderState {
    public boolean hasSaddle;
    public boolean hasBridle;
    public int armorTier;
}
