package doc.fasterminecarts;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class ItemGoldenCarrotOnAStick extends Item {
    public ItemGoldenCarrotOnAStick() {
        setUnlocalizedName("goldenCarrotOnAStick");
        setTextureName("giscraft:golden_carrot_on_a_stick");
        setCreativeTab(CreativeTabs.tabTransport);
        setMaxStackSize(1);
        setMaxDamage(25);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }
}
