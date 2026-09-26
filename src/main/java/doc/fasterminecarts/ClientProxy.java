package doc.fasterminecarts;

import net.minecraftforge.common.MinecraftForge;

public final class ClientProxy extends CommonProxy {

    @Override
    public void registerClientHandlers() {
        MinecraftForge.EVENT_BUS.register(new ExperienceHudHandler());
        MinecraftForge.EVENT_BUS.register(new PauseMenuHandler());
        MinecraftForge.EVENT_BUS.register(new OptionsMenuHandler());
    }
}
