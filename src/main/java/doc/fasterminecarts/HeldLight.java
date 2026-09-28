package doc.fasterminecarts;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class HeldLight {
    private static Item litTorch;
    private static boolean resolved;

    private HeldLight() {}

    public static int includeSlot(int current, Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return current;
        }
        LightCarry carry = LightCarry.get((EntityPlayer) entity);
        if (carry == null) {
            return current;
        }
        ItemStack stack = carry.getStack();
        if (stack == null || wetLitTorch(stack, entity)) {
            return current;
        }
        int light = Luminous.lightLevel(stack);
        return light > current ? light : current;
    }

    public static boolean wetLitTorch(ItemStack stack, Entity entity) {
        return stack != null
                && stack.getItem() != null
                && entity != null
                && entity.isInsideOfMaterial(Material.water)
                && stack.getItem() == litTorch();
    }

    private static Item litTorch() {
        if (!resolved) {
            resolved = true;
            litTorch = GameRegistry.findItem("RealisticTorches", "TorchLit");
        }
        return litTorch;
    }
}
