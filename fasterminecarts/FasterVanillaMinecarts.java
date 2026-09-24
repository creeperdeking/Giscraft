package doc.fasterminecarts;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = FasterVanillaMinecarts.MOD_ID,
        name = "Faster Vanilla Minecarts",
        version = FasterVanillaMinecarts.VERSION)
public final class FasterVanillaMinecarts {
    public static final String MOD_ID = "fastervanillaminecarts";
    public static final String VERSION = "1.2.6";

    @SidedProxy(
            clientSide = "doc.fasterminecarts.ClientProxy",
            serverSide = "doc.fasterminecarts.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        ChestShape.setFullCubeBounds(Blocks.chest);
        ChestShape.setFullCubeBounds(Blocks.trapped_chest);
        MinecraftForge.EVENT_BUS.register(new ExperienceHandler());
        proxy.registerClientHandlers();
    }
}
