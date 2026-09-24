package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Gives ridden pigs predictable acceleration without replacing vanilla
 * steering or vertical movement.
 */
public final class PigSpeedHandler {
    private static final double MAX_SPEED_BLOCKS_PER_TICK = 8.0D / 20.0D;
    private static final double ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.02D;
    private static final String SPEED_TAG = "FasterVanillaMinecartsPigSpeed";
    private static final String STEP_HEIGHT_TAG =
            "FasterVanillaMinecartsPigOriginalStepHeight";
    private static final float RIDING_STEP_HEIGHT = 1.0F;

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
                if (data.hasKey(STEP_HEIGHT_TAG)) {
                    pig.stepHeight = data.getFloat(STEP_HEIGHT_TAG);
                    data.removeTag(STEP_HEIGHT_TAG);
                }
                continue;
            }

            if (!data.hasKey(STEP_HEIGHT_TAG)) {
                data.setFloat(STEP_HEIGHT_TAG, pig.stepHeight);
            }
            pig.stepHeight = RIDING_STEP_HEIGHT;

            EntityPlayer rider = (EntityPlayer) pig.riddenByEntity;
            ItemStack heldItem = rider.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.carrot_on_a_stick) {
                data.removeTag(SPEED_TAG);
                pig.motionX = 0.0D;
                pig.motionZ = 0.0D;
                pig.moveStrafing = 0.0F;
                pig.moveForward = 0.0F;
                pig.velocityChanged = true;
            }
        }
    }

    /**
     * Replaces the final movement call in EntityAIControlledByPlayer. Supplying
     * zero movement input prevents vanilla from adding its own acceleration.
     */
    public static void moveControlledPig(
            EntityLiving entity,
            float strafe,
            float forward) {

        if (!(entity instanceof EntityPig)
                || !(entity.riddenByEntity instanceof EntityPlayer)) {
            entity.moveEntityWithHeading(strafe, forward);
            return;
        }

        EntityPig pig = (EntityPig) entity;
        EntityPlayer rider = (EntityPlayer) pig.riddenByEntity;
        ItemStack heldItem = rider.getHeldItem();

        if (heldItem == null || heldItem.getItem() != Items.carrot_on_a_stick) {
            pig.getEntityData().removeTag(SPEED_TAG);
            pig.motionX = 0.0D;
            pig.motionZ = 0.0D;
            pig.moveStrafing = 0.0F;
            pig.moveForward = 0.0F;
            pig.moveEntityWithHeading(0.0F, 0.0F);
            return;
        }

        NBTTagCompound data = pig.getEntityData();
        pig.stepHeight = RIDING_STEP_HEIGHT;
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

        pig.moveEntityWithHeading(0.0F, 0.0F);
        pig.motionX = 0.0D;
        pig.motionZ = 0.0D;
        pig.moveStrafing = 0.0F;
        pig.moveForward = 0.0F;
        pig.velocityChanged = true;
    }
}
