package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/**
 * Golden tools always have Silk Touch, including crafted, found, and existing
 * tools.
 */
public final class GoldenToolHandler {

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.entityLiving;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            applySilkTouch(player.inventory.getStackInSlot(i));
        }
    }

    @SubscribeEvent
    public void onItemCrafted(ItemCraftedEvent event) {
        applySilkTouch(event.crafting);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityItem) {
            applySilkTouch(((EntityItem) event.entity).getEntityItem());
        }
    }

    private static void applySilkTouch(ItemStack stack) {
        if (!isGoldTool(stack)) {
            return;
        }

        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) > 0) {
            return;
        }

        stack.addEnchantment(Enchantment.silkTouch, 1);
    }

    private static boolean isGoldTool(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            return Item.ToolMaterial.GOLD.name().equals(((ItemTool) item).getToolMaterialName());
        }

        if (item instanceof ItemHoe) {
            return Item.ToolMaterial.GOLD.name().equals(((ItemHoe) item).getToolMaterialName());
        }

        return false;
    }
}
