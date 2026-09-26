package doc.fasterminecarts;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ChestGenHooks;

/**
 * Removes melon and pumpkin seeds from vanilla dungeon chest loot.
 */
public final class ChestLootHandler {
    public static void removeDungeonSeeds() {
        ChestGenHooks.removeItem(ChestGenHooks.DUNGEON_CHEST, new ItemStack(Items.pumpkin_seeds));
        ChestGenHooks.removeItem(ChestGenHooks.DUNGEON_CHEST, new ItemStack(Items.melon_seeds));
    }
}
