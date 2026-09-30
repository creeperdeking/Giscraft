package doc.fasterminecarts;

import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public final class ClientProxy extends CommonProxy {

    @Override
    public void registerClientHandlers() {
        MinecraftForge.EVENT_BUS.register(new ExperienceHudHandler());
        MinecraftForge.EVENT_BUS.register(new PauseMenuHandler());
        MinecraftForge.EVENT_BUS.register(new OptionsMenuHandler());
        RenderingRegistry.registerEntityRenderingHandler(EntityFlowerGolem.class, new RenderFlowerGolem());
        RenderingRegistry.registerEntityRenderingHandler(EntityGiantSquid.class, new RenderGiantSquid());
        RenderingRegistry.registerEntityRenderingHandler(EntitySnowman.class, new RenderFixedSnowman());
        RenderingRegistry.registerEntityRenderingHandler(EntityBareSnowman.class, new RenderBareSnowman());
    }

    @Override
    public void applyLightSlot(int entityId, ItemStack stack) {
        LightSlotGui.apply(entityId, stack);
    }
}
