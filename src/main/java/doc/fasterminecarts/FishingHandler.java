package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Makes every fished fish the original vanilla fish instead of salmon,
 * clownfish, or pufferfish.
 */
public final class FishingHandler {

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !(event.entity instanceof EntityItem)) {
            return;
        }

        EntityItem entityItem = (EntityItem) event.entity;
        ItemStack stack = entityItem.getEntityItem();
        if (stack == null || stack.getItem() != Items.fish) {
            return;
        }

        if (stack.getItemDamage() == 0) {
            return;
        }

        entityItem.setEntityItemStack(new ItemStack(Items.fish, stack.stackSize, 0));
    }
}
