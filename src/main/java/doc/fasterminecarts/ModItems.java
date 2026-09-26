package doc.fasterminecarts;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ModItems {
    public static Item GOLDEN_CARROT_ON_A_STICK;

    private ModItems() {
    }

    public static void register() {
        GOLDEN_CARROT_ON_A_STICK = new ItemGoldenCarrotOnAStick();
        GameRegistry.registerItem(
                GOLDEN_CARROT_ON_A_STICK,
                "golden_carrot_on_a_stick");
    }

    public static void registerRecipes() {
        GameRegistry.addShapelessRecipe(
                new ItemStack(GOLDEN_CARROT_ON_A_STICK),
                Items.fishing_rod,
                Items.golden_carrot);
    }
}
