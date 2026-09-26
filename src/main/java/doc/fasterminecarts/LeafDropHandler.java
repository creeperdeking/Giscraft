package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;

import java.util.Iterator;

/**
 * Stops leaves from dropping apples through Forge's harvest drop list.
 */
public final class LeafDropHandler {

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        Iterator<ItemStack> drops = event.drops.iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next();
            if (stack == null) {
                continue;
            }

            if (stack.getItem() == Items.poisonous_potato) {
                drops.remove();
                continue;
            }

            if (event.block instanceof BlockLeavesBase
                    && stack.getItem() == Items.apple) {
                drops.remove();
            }
        }
    }
}
