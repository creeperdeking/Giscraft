package doc.fasterminecarts;

import net.minecraft.item.ItemStack;

public final class LightSlotSet extends LightSlotMessage {
    public LightSlotSet() {}

    public LightSlotSet(ItemStack stack) {
        this.stack = stack == null ? null : stack.copy();
    }
}
