package doc.fasterminecarts;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

final class Flowers {
    static final class Kind {
        final Block block;
        final int meta;
        final boolean tall;

        Kind(Block block, int meta, boolean tall) {
            this.block = block;
            this.meta = meta;
            this.tall = tall;
        }
    }

    private static final Kind[] ALL = new Kind[] {
            new Kind(Blocks.yellow_flower, 0, false),
            new Kind(Blocks.red_flower, 0, false),
            new Kind(Blocks.red_flower, 1, false),
            new Kind(Blocks.red_flower, 2, false),
            new Kind(Blocks.red_flower, 3, false),
            new Kind(Blocks.red_flower, 4, false),
            new Kind(Blocks.red_flower, 5, false),
            new Kind(Blocks.red_flower, 6, false),
            new Kind(Blocks.red_flower, 7, false),
            new Kind(Blocks.red_flower, 8, false),
            new Kind(Blocks.double_plant, 0, true),
            new Kind(Blocks.double_plant, 1, true),
            new Kind(Blocks.double_plant, 4, true),
            new Kind(Blocks.double_plant, 5, true)
    };

    private Flowers() {
    }

    static Kind kind(int index) {
        if (index < 0 || index >= ALL.length) {
            return ALL[0];
        }
        return ALL[index];
    }

    static int count() {
        return ALL.length;
    }
}
