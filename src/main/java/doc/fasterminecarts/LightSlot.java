package doc.fasterminecarts;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class LightSlot extends Slot {
    public static final int X = 61;
    public static final int Y = 61;

    public LightSlot(IInventory inventory) {
        super(inventory, 0, X, Y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return Luminous.canHold(stack);
    }
}
