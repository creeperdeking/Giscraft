package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Makes logs require an axe, like stone requires a pickaxe: slow to punch
 * and no drops without the proper tool.
 */
public final class LogHarvestHandler {
    private static final int AXE_HARVEST_LEVEL = 0;
    private static final float BARE_HAND_SPEED_FACTOR = 0.3F;

    public static void applyAxeRequirement() {
        Blocks.log.setHarvestLevel("axe", AXE_HARVEST_LEVEL);
        Blocks.log2.setHarvestLevel("axe", AXE_HARVEST_LEVEL);

        for (ItemStack stack : OreDictionary.getOres("logWood")) {
            if (stack == null) {
                continue;
            }

            Block block = Block.getBlockFromItem(stack.getItem());
            if (block == null || block == Blocks.air) {
                continue;
            }

            int meta = stack.getItemDamage();
            if (meta == OreDictionary.WILDCARD_VALUE) {
                block.setHarvestLevel("axe", AXE_HARVEST_LEVEL);
            } else {
                block.setHarvestLevel("axe", AXE_HARVEST_LEVEL, meta);
            }
        }
    }

    @SubscribeEvent
    public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!isLog(event.block, 0) && !isLog(event.block, OreDictionary.WILDCARD_VALUE)) {
            return;
        }

        event.success = isHoldingAxe(event.entityPlayer);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!isLog(event.block, event.metadata)) {
            return;
        }

        if (isHoldingAxe(event.entityPlayer)) {
            return;
        }

        event.newSpeed = event.originalSpeed * BARE_HAND_SPEED_FACTOR;
    }

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.harvester == null || !isLog(event.block, event.blockMetadata)) {
            return;
        }

        if (!isHoldingAxe(event.harvester)) {
            event.drops.clear();
            event.dropChance = 0.0F;
        }
    }

    private static boolean isHoldingAxe(EntityPlayer player) {
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack == null) {
            return false;
        }

        return stack.getItem().getHarvestLevel(stack, "axe") >= AXE_HARVEST_LEVEL;
    }

    private static boolean isLog(Block block, int meta) {
        if (block instanceof BlockLog) {
            return true;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return false;
        }

        ItemStack stack = new ItemStack(item, 1, meta);
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if ("logWood".equals(OreDictionary.getOreName(oreId))) {
                return true;
            }
        }

        return false;
    }
}
