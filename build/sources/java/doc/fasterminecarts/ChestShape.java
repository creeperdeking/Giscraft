package doc.fasterminecarts;

import net.minecraft.block.Block;

public final class ChestShape {
    private ChestShape() {
    }

    public static void setFullCubeBounds(Block block) {
        block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }
}
