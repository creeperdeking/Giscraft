package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * Past the overworld void, players take the same damage as a fall out of the
 * world. Past the nether bedrock, they die immediately.
 */
public final class WorldEdge {
    private static final float VOID_DAMAGE = 4.0F;
    private static final double OVERWORLD_MARGIN = 10.0D;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) {
            return;
        }
        if (!OceanBoundaryConfig.enabled || !event.player.isEntityAlive()) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.dimension == -1) {
            if (NetherBoundary.outsideBedrock(player.posX, player.posZ)) {
                player.attackEntityFrom(DamageSource.outOfWorld, Float.MAX_VALUE);
            }
            return;
        }
        if (player.dimension != 0) {
            return;
        }
        double past = OceanBoundaryMath.blocksPastVoid(
                player.posX, player.posZ, OceanBoundaryConfig.settings());
        if (past > OVERWORLD_MARGIN) {
            player.attackEntityFrom(DamageSource.outOfWorld, VOID_DAMAGE);
        }
    }
}
