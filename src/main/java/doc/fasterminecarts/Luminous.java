package doc.fasterminecarts;

import java.lang.reflect.Method;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class Luminous {
    private static Item unlitTorch;
    private static boolean resolvedTorch;
    private static Method dynamicLight;
    private static boolean resolvedDynamic;

    private Luminous() {}

    public static boolean canHold(ItemStack stack) {
        return lightLevel(stack) > 0 || isUnlitTorch(stack);
    }

    public static boolean isUnlitTorch(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.getItem() == unlitTorch();
    }

    public static int lightLevel(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return 0;
        }
        Item item = stack.getItem();
        int produced = dynamicLight(item, stack);
        if (produced > 0) {
            return produced;
        }
        if (item instanceof ItemBlock) {
            net.minecraft.block.Block block = ((ItemBlock) item).field_150939_a;
            if (block != null && block.getLightValue() > 0) {
                return block.getLightValue();
            }
        }
        if (item == Items.lava_bucket) {
            return Blocks.lava.getLightValue();
        }
        return 0;
    }

    private static Item unlitTorch() {
        if (!resolvedTorch) {
            resolvedTorch = true;
            unlitTorch = GameRegistry.findItem("RealisticTorches", "TorchUnlit");
        }
        return unlitTorch;
    }

    private static int dynamicLight(Item item, ItemStack stack) {
        Method method = dynamicLightMethod();
        if (method == null || !method.getDeclaringClass().isInstance(item)) {
            return 0;
        }
        try {
            return ((Integer) method.invoke(item, stack)).intValue();
        } catch (ReflectiveOperationException failure) {
            return 0;
        }
    }

    private static Method dynamicLightMethod() {
        if (!resolvedDynamic) {
            resolvedDynamic = true;
            try {
                Class<?> type = Class.forName("com.gtnewhorizons.angelica.api.IDynamicLightProducer");
                dynamicLight = type.getMethod("getLuminance", ItemStack.class);
            } catch (ReflectiveOperationException ignored) {
                dynamicLight = null;
            }
        }
        return dynamicLight;
    }
}
