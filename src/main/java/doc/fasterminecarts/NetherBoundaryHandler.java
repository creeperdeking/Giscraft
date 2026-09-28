package doc.fasterminecarts;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Cuts the Nether at half the Overworld ice-wall radius. The ring is bedrock,
 * and everything past it is left empty.
 */
public final class NetherBoundaryHandler {
    private static final Logger LOG = LogManager.getLogger("Giscraft");

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!OceanBoundaryConfig.enabled || event.world.isRemote || event.world.provider.dimensionId != -1) {
            return;
        }
        NetherBoundary.carve(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.world;
        if (!OceanBoundaryConfig.enabled || world.isRemote || world.provider.dimensionId != -1) {
            return;
        }
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return;
        }
        ChunkProviderServer server = (ChunkProviderServer) provider;
        IChunkProvider generator = server.currentChunkProvider;
        if (generator == null || generator instanceof NetherBoundaryProvider) {
            return;
        }
        server.currentChunkProvider = new NetherBoundaryProvider(generator, world);
        LOG.info("Nether bedrock wall attached at half the overworld radius.");
    }

    static void registerPass() {
        GameRegistry.registerWorldGenerator(new IWorldGenerator() {
            @Override
            public void generate(
                    Random random,
                    int chunkX,
                    int chunkZ,
                    World world,
                    IChunkProvider chunkGenerator,
                    IChunkProvider chunkProvider) {
                if (!OceanBoundaryConfig.enabled || world.isRemote || world.provider.dimensionId != -1) {
                    return;
                }
                NetherBoundary.carveAround(world, chunkX, chunkZ);
            }
        }, Integer.MAX_VALUE);
    }
}
