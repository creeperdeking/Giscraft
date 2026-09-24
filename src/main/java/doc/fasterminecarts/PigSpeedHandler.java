package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Gives ridden pigs predictable acceleration without replacing vanilla
 * steering or vertical movement.
 */
public final class PigSpeedHandler {
    private static final double MAX_SPEED_BLOCKS_PER_TICK = 8.0D / 20.0D;
    private static final double ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.02D;
    private static final String SPEED_TAG = "FasterVanillaMinecartsPigSpeed";

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (Object entity : event.world.loadedEntityList) {
            if (!(entity instanceof EntityPig)) {
                continue;
            }

            EntityPig pig = (EntityPig) entity;
            NBTTagCompound data = pig.getEntityData();

            if (!(pig.riddenByEntity instanceof EntityPlayer)) {
                data.removeTag(SPEED_TAG);
                continue;
            }

            double speed = data.hasKey(SPEED_TAG)
                    ? data.getDouble(SPEED_TAG)
                    : Math.sqrt(pig.motionX * pig.motionX + pig.motionZ * pig.motionZ);
            speed = Math.min(
                    MAX_SPEED_BLOCKS_PER_TICK,
                    speed + ACCELERATION_BLOCKS_PER_TICK_SQUARED);
            data.setDouble(SPEED_TAG, speed);

            double yawRadians = Math.toRadians(pig.rotationYaw);
            pig.motionX = -Math.sin(yawRadians) * speed;
            pig.motionZ = Math.cos(yawRadians) * speed;
            pig.velocityChanged = true;
        }
    }
}
