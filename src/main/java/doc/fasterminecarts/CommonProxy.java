package doc.fasterminecarts;

import net.minecraft.item.ItemStack;

public class CommonProxy {

    public void registerClientHandlers() {
        // Client-only handlers are registered by ClientProxy.
    }

    public void applyLightSlot(int entityId, ItemStack stack) {}
}
