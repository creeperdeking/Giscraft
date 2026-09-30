package doc.fasterminecarts;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.gen.structure.MapGenNetherBridge;

/**
 * Wraps the real nether generator so the bedrock wall can replace chunks past
 * the radius. It still has to look like {@link ChunkProviderHell}: fortress
 * spawns look the generator up by that class, then read the bridge field off
 * the object they found, without searching a parent class.
 */
final class NetherBoundaryProvider extends ChunkProviderHell {
    /**
     * Same bridge the wrapped generator uses. The name is the one the spawner
     * asks for at runtime.
     */
    public MapGenNetherBridge field_73172_c;

    private final IChunkProvider delegate;
    private final World world;

    NetherBoundaryProvider(IChunkProvider delegate, World world) {
        super(world, world.getSeed());
        this.delegate = delegate;
        this.world = world;
        if (delegate instanceof ChunkProviderHell) {
            this.field_73172_c = ((ChunkProviderHell) delegate).genNetherBridge;
        }
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return delegate.chunkExists(x, z);
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        if (!OceanBoundaryConfig.enabled) {
            return delegate.provideChunk(x, z);
        }
        if (NetherBoundary.isChunkFullyVoid(x, z)) {
            return NetherBoundary.voidChunk(world, x, z);
        }
        Chunk chunk = delegate.provideChunk(x, z);
        NetherBoundary.carve(chunk);
        return chunk;
    }

    @Override
    public Chunk loadChunk(int x, int z) {
        return delegate.loadChunk(x, z);
    }

    @Override
    public void populate(IChunkProvider provider, int x, int z) {
        if (!OceanBoundaryConfig.enabled) {
            delegate.populate(provider, x, z);
            return;
        }
        if (NetherBoundary.isChunkFullyVoid(x, z)) {
            Chunk outside = provider.provideChunk(x, z);
            outside.isTerrainPopulated = true;
            outside.isLightPopulated = true;
            return;
        }
        delegate.populate(provider, x, z);
        NetherBoundary.carveAround(world, x, z);
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
        if (OceanBoundaryConfig.enabled && NetherBoundary.isChunkFullyVoid(x >> 4, z >> 4)) {
            return Collections.emptyList();
        }
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
        if (OceanBoundaryConfig.enabled && NetherBoundary.isChunkFullyVoid(x, z)) {
            return;
        }
        delegate.recreateStructures(x, z);
    }

    @Override
    public void saveExtraData() {
        delegate.saveExtraData();
    }
}
