package doc.fasterminecarts;

import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

final class OceanBoundaryProvider implements IChunkProvider {
    private final IChunkProvider delegate;
    private final World world;
    private final long seed;

    OceanBoundaryProvider(IChunkProvider delegate, World world) {
        this.delegate = delegate;
        this.world = world;
        this.seed = world.getSeed();
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return delegate.chunkExists(x, z);
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        if (OceanBoundaryMath.isChunkFullyVoid(x, z, settings)) {
            return OceanBoundary.voidChunk(world, x, z);
        }
        Chunk chunk = delegate.provideChunk(x, z);
        OceanBoundary.apply(chunk, seed);
        if (OceanBoundaryMath.isChunkPastSnow(x, z, settings)) {
            chunk.isTerrainPopulated = true;
            chunk.isLightPopulated = true;
        }
        return chunk;
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return delegate.loadChunk(x, z);
    }

    @Override
    public void populate(IChunkProvider provider, int x, int z) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        if (OceanBoundaryMath.isChunkFullyVoid(x, z, settings) || OceanBoundaryMath.isChunkPastSnow(x, z, settings)) {
            Chunk outside = provider.provideChunk(x, z);
            outside.isTerrainPopulated = true;
            outside.isLightPopulated = true;
            return;
        }
        delegate.populate(provider, x, z);
        Chunk chunk = provider.provideChunk(x, z);
        OceanBoundary.stripOres(chunk, seed);
        OceanBoundary.finishEdge(chunk, seed);
        OceanBoundary.sealSpilledLakes(chunk, seed);
    }

    @Override
    public boolean saveChunks(boolean all, IProgressUpdate progress) {
        return delegate.saveChunks(all, progress);
    }

    @Override
    public boolean unloadQueuedChunks() {
        return delegate.unloadQueuedChunks();
    }

    @Override
    public boolean canSave() {
        return delegate.canSave();
    }

    @Override
    public String makeString() {
        return delegate.makeString();
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        return delegate.getPossibleCreatures(type, x, y, z);
    }

    @Override
    public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
        return delegate.func_147416_a(world, structure, x, y, z);
    }

    @Override
    public int getLoadedChunkCount() {
        return delegate.getLoadedChunkCount();
    }

    @Override
    public void recreateStructures(int x, int z) {
        delegate.recreateStructures(x, z);
    }

    @Override
    public void saveExtraData() {
        delegate.saveExtraData();
    }
}
