package doc.fasterminecarts;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;

public final class LightSlotSync extends LightSlotMessage {
    public LightSlotSync() {}

    public LightSlotSync(int entityId, ItemStack stack) {
        this.entityId = entityId;
        this.stack = stack == null ? null : stack.copy();
    }
}

class LightSlotMessage implements cpw.mods.fml.common.network.simpleimpl.IMessage {
    public int entityId;
    public ItemStack stack;

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeItemStack(buf, stack);
    }
}
