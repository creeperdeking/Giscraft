package doc.fasterminecarts;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Wraps Old World Gen's beta chunk generator after the world exists.
 * The server calls that generator only when a chunk is not already on disk.
 */
public final class OceanBoundaryHandler {
    private static final Logger LOG = LogManager.getLogger("Giscraft");
    private static final String BETA_GENERATOR = "owg.generator.ChunkGeneratorBeta";

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!OceanBoundaryConfig.enabled || event.world.provider.dimensionId != 0) {
            return;
        }
        Chunk chunk = event.getChunk();
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        if (OceanBoundaryMath.chunkReachesSnow(originX, originZ, settings)
                || OceanBoundaryMath.chunkReachesIcebergs(originX, originZ, settings)) {
            OceanBoundary.stripLooseSnow(chunk);
        }
        if (!event.world.isRemote && !OceanBoundary.chunkIsInside(originX, originZ, settings)) {
            OceanBoundary.braceSeabed(chunk);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.world;
        if (!OceanBoundaryConfig.enabled || world.isRemote || world.provider.dimensionId != 0) {
            return;
        }

        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return;
        }

        ChunkProviderServer server = (ChunkProviderServer) provider;
        IChunkProvider generator = server.currentChunkProvider;
        if (generator == null || generator instanceof OceanBoundaryProvider) {
            return;
        }
        if (!isBetaGenerator(generator)) {
            return;
        }

        server.currentChunkProvider = new OceanBoundaryProvider(generator, world);
        LOG.info("Ocean boundary attached to Old World Gen beta terrain.");
    }

    static void registerIceWall() {
        GameRegistry.registerWorldGenerator(new IWorldGenerator() {
            @Override
            public void generate(
                    Random random,
                    int chunkX,
                    int chunkZ,
                    World world,
                    IChunkProvider chunkGenerator,
                    IChunkProvider chunkProvider) {
                if (world.isRemote || world.provider.dimensionId != 0) {
                    return;
                }
                Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                OceanBoundary.finishEdge(chunk, world.getSeed());
            }
        }, Integer.MAX_VALUE);
    }

    private static boolean isBetaGenerator(IChunkProvider generator) {
        try {
            Class generatorClass = Class.forName(BETA_GENERATOR);
            return generatorClass.isInstance(generator);
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }
}
