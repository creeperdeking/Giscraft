package doc.fasterminecarts;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = Giscraft.MOD_ID,
        name = "Giscraft",
        version = Giscraft.VERSION)
public final class Giscraft {
    public static final String MOD_ID = "giscraft";
    public static final String VERSION = "1.3.1";

    @SidedProxy(
            clientSide = "doc.fasterminecarts.ClientProxy",
            serverSide = "doc.fasterminecarts.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        ModItems.register();
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        ModItems.registerRecipes();
        ChestShape.setFullCubeBounds(Blocks.chest);
        ChestShape.setFullCubeBounds(Blocks.trapped_chest);
        MinecraftForge.EVENT_BUS.register(new ExperienceHandler());
        MinecraftForge.EVENT_BUS.register(new PigSpeedHandler());
        proxy.registerClientHandlers();
    }
}
