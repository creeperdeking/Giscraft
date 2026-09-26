package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Vanilla sprinting is disabled unless the player is in creative mode or
 * wearing IC2 nano or quantum leggings. Those pants also prevent movement
 * from draining hunger.
 */
public final class SprintHandler {
    private static final int LEGS_SLOT = 1;
    private static final String NANO_LEGS_CLASS =
            "ic2.core.item.armor.ItemArmorNanoSuit";
    private static final String QUANTUM_LEGS_CLASS =
            "ic2.core.item.armor.ItemArmorQuantumSuit";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        restrictSprint(event.player);
    }

    private static void restrictSprint(EntityPlayer player) {
        if (!player.isSprinting()
                || player.capabilities.isCreativeMode
                || wearingMovementSuit(player)) {
            return;
        }

        player.setSprinting(false);
    }

    public static boolean wearingMovementSuit(EntityPlayer player) {
        ItemStack legs = player.getCurrentArmor(LEGS_SLOT);
        if (legs == null) {
            return false;
        }

        Item item = legs.getItem();
        if (!(item instanceof ItemArmor) || ((ItemArmor) item).armorType != 2) {
            return false;
        }

        String className = item.getClass().getName();
        return NANO_LEGS_CLASS.equals(className)
                || QUANTUM_LEGS_CLASS.equals(className);
    }
}
