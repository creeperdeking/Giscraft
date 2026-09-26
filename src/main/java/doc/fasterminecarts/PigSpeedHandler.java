package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Gives ridden pigs predictable acceleration without replacing vanilla
 * steering or vertical movement.
 */
public final class PigSpeedHandler {
    private static final double NORMAL_MAX_SPEED_BLOCKS_PER_TICK = 4.0D / 20.0D;
    private static final double GOLDEN_MAX_SPEED_BLOCKS_PER_TICK = 8.0D / 20.0D;
    private static final double ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.02D;
    private static final String SPEED_TAG = "GiscraftPigSpeed";
    private static final String STEP_HEIGHT_TAG =
            "GiscraftPigOriginalStepHeight";
    private static final float RIDING_STEP_HEIGHT = 1.0F;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof EntityPig)) {
            return;
        }

        EntityPig pig = (EntityPig) event.entityLiving;
        NBTTagCompound data = pig.getEntityData();

        if (!(pig.riddenByEntity instanceof EntityPlayer)) {
            data.removeTag(SPEED_TAG);
            if (data.hasKey(STEP_HEIGHT_TAG)) {
                pig.stepHeight = data.getFloat(STEP_HEIGHT_TAG);
                data.removeTag(STEP_HEIGHT_TAG);
            }
            return;
        }

        if (!data.hasKey(STEP_HEIGHT_TAG)) {
            data.setFloat(STEP_HEIGHT_TAG, pig.stepHeight);
        }
        pig.stepHeight = RIDING_STEP_HEIGHT;

        EntityPlayer rider = (EntityPlayer) pig.riddenByEntity;
        ItemStack heldItem = rider.getHeldItem();
        if (getMaximumSpeed(heldItem) == 0.0D) {
            data.removeTag(SPEED_TAG);
            boolean wasMoving = pig.motionX != 0.0D || pig.motionZ != 0.0D;
            pig.motionX = 0.0D;
            pig.motionZ = 0.0D;
            pig.moveStrafing = 0.0F;
            pig.moveForward = 0.0F;
            if (wasMoving) {
                pig.velocityChanged = true;
            }
            return;
        }
    }

    /**
     * Replaces EntityAIControlledByPlayer's extra movement call. The velocity
     * prepared here is consumed by the entity's normal movement pass on the
     * following tick and sent to clients for normal entity interpolation.
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
        double maximumSpeed = getMaximumSpeed(heldItem);

        if (maximumSpeed == 0.0D) {
            pig.getEntityData().removeTag(SPEED_TAG);
            pig.motionX = 0.0D;
            pig.motionZ = 0.0D;
            pig.moveStrafing = 0.0F;
            pig.moveForward = 0.0F;
            return;
        }

        NBTTagCompound data = pig.getEntityData();
        pig.stepHeight = RIDING_STEP_HEIGHT;
        double speed = data.hasKey(SPEED_TAG)
                ? data.getDouble(SPEED_TAG)
                : Math.sqrt(pig.motionX * pig.motionX + pig.motionZ * pig.motionZ);
        speed = Math.min(
                maximumSpeed,
                speed + ACCELERATION_BLOCKS_PER_TICK_SQUARED);
        data.setDouble(SPEED_TAG, speed);

        double yawRadians = Math.toRadians(pig.rotationYaw);
        pig.motionX = -Math.sin(yawRadians) * speed;
        pig.motionZ = Math.cos(yawRadians) * speed;
        pig.moveStrafing = 0.0F;
        pig.moveForward = 0.0F;
        pig.velocityChanged = true;
    }

    public static boolean isHoldingGoldenControlItem(EntityPig pig) {
        if (!(pig.riddenByEntity instanceof EntityPlayer)) {
            return false;
        }

        ItemStack heldItem = ((EntityPlayer) pig.riddenByEntity).getHeldItem();
        return heldItem != null
                && heldItem.getItem() == ModItems.GOLDEN_CARROT_ON_A_STICK;
    }

    private static double getMaximumSpeed(ItemStack heldItem) {
        if (heldItem == null) {
            return 0.0D;
        }

        if (heldItem.getItem() == ModItems.GOLDEN_CARROT_ON_A_STICK) {
            return GOLDEN_MAX_SPEED_BLOCKS_PER_TICK;
        }

        if (heldItem.getItem() == Items.carrot_on_a_stick) {
            return NORMAL_MAX_SPEED_BLOCKS_PER_TICK;
        }

        return 0.0D;
    }
}
