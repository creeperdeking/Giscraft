package doc.fasterminecarts;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.potion.PotionHelper;
import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = Giscraft.MOD_ID,
        name = "Giscraft",
        version = Giscraft.VERSION)
public final class Giscraft {
    public static final String MOD_ID = "giscraft";
    public static final String VERSION = "1.3.15";

    @Mod.Instance(Giscraft.MOD_ID)
    public static Giscraft instance;

    @SidedProxy(
            clientSide = "doc.fasterminecarts.ClientProxy",
            serverSide = "doc.fasterminecarts.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        OceanBoundaryConfig.load(event);
        ModItems.register();
        LightCarry.register();
        CartPortal.register(this);
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        ModItems.registerRecipes();
        ChestShape.setFullCubeBounds(Blocks.chest);
        ChestShape.setFullCubeBounds(Blocks.trapped_chest);
        MinecraftForge.EVENT_BUS.register(new ExperienceHandler());
        MinecraftForge.EVENT_BUS.register(new FishingHandler());
        MinecraftForge.EVENT_BUS.register(new LeafDropHandler());
        MinecraftForge.EVENT_BUS.register(new LogHarvestHandler());
        MinecraftForge.EVENT_BUS.register(new PigSpeedHandler());
        GoldenToolHandler goldenTools = new GoldenToolHandler();
        MinecraftForge.EVENT_BUS.register(goldenTools);
        FMLCommonHandler.instance().bus().register(goldenTools);
        FMLCommonHandler.instance().bus().register(new SprintHandler());
        MinecraftForge.EVENT_BUS.register(new OceanBoundaryHandler());
        MinecraftForge.EVENT_BUS.register(new NetherBoundaryHandler());
        MinecraftForge.EVENT_BUS.register(new PortalLink());
        OuterLife outerLife = new OuterLife();
        MinecraftForge.EVENT_BUS.register(outerLife);
        FMLCommonHandler.instance().bus().register(outerLife);
        EntityRegistry.registerModEntity(EntityFlowerGolem.class, "FlowerGolem", 1, instance, 80, 3, true);
        EntityRegistry.registerModEntity(EntityGiantSquid.class, "GiantSquid", 2, instance, 160, 3, true);
        EntityRegistry.registerModEntity(EntityBareSnowman.class, "BareSnowman", 3, instance, 64, 3, true);
        proxy.registerClientHandlers();
    }

    @Mod.EventHandler
    public void postInitialize(FMLPostInitializationEvent event) {
        LogHarvestHandler.applyAxeRequirement();
        ChestLootHandler.removeDungeonSeeds();
        GoldenToolHandler.applyGoldPickaxeHarvestLevel();
        Items.golden_carrot.setPotionEffect(PotionHelper.speckledMelonEffect);
        OceanBoundaryHandler.registerIceWall();
        NetherBoundaryHandler.registerPass();
    }
}
