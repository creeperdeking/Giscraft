package doc.fasterminecarts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

final class NetherBoundary {
    static final int THICKNESS = 8;
    private static final int WALL_TOP = 127;

    private NetherBoundary() {}

    static double radius() {
        return OceanBoundaryMath.iceWallRadius(OceanBoundaryConfig.settings()) * 0.5D;
    }

    static boolean isChunkFullyInside(int chunkX, int chunkZ) {
        return farthest(chunkX, chunkZ) < radius();
    }

    static boolean isChunkFullyVoid(int chunkX, int chunkZ) {
        return closest(chunkX, chunkZ) >= radius() + THICKNESS;
    }

    static Chunk voidChunk(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        byte[] biomes = chunk.getBiomeArray();
        byte hell = (byte) BiomeGenBase.hell.biomeID;
        for (int index = 0; index < biomes.length; index++) {
            biomes[index] = hell;
        }
        chunk.isTerrainPopulated = true;
        chunk.isLightPopulated = true;
        chunk.isModified = true;
        return chunk;
    }

    static void carveAround(World world, int chunkX, int chunkZ) {
        IChunkProvider provider = world.getChunkProvider();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                if ((dx != 0 || dz != 0) && !provider.chunkExists(x, z)) {
                    continue;
                }
                carve(world.getChunkFromChunkCoords(x, z));
            }
        }
    }

    static void carve(Chunk chunk) {
        if (chunk == null || isChunkFullyInside(chunk.xPosition, chunk.zPosition)) {
            return;
        }
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        double wall = radius();
        boolean changed = false;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                double distance = blockDistance(originX + localX, originZ + localZ);
                if (distance < wall) {
                    continue;
                }
                if (distance < wall + THICKNESS) {
                    changed |= fill(chunk, localX, localZ, Blocks.bedrock, 0, WALL_TOP);
                    changed |= fill(chunk, localX, localZ, Blocks.air, WALL_TOP + 1, 255);
                } else {
                    changed |= fill(chunk, localX, localZ, Blocks.air, 0, 255);
                }
            }
        }
        if (!changed) {
            return;
        }
        dropVoidEntities(chunk, originX, originZ, wall + THICKNESS);
        removeOrphanTileEntities(chunk);
        discardEmptySections(chunk);
        chunk.generateSkylightMap();
        chunk.isModified = true;
    }

    private static void dropVoidEntities(Chunk chunk, int originX, int originZ, double voidStart) {
        List[] lists = chunk.entityLists;
        for (int section = 0; section < lists.length; section++) {
            List list = lists[section];
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (int index = list.size() - 1; index >= 0; index--) {
                Entity entity = (Entity) list.get(index);
                if (entity instanceof EntityPlayer) {
                    continue;
                }
                if (distance(entity.posX, entity.posZ) >= voidStart) {
                    entity.setDead();
                }
            }
        }
    }

    private static double farthest(int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        double farthest = 0.0D;
        farthest = Math.max(farthest, blockDistance(originX, originZ));
        farthest = Math.max(farthest, blockDistance(originX + 15, originZ));
        farthest = Math.max(farthest, blockDistance(originX, originZ + 15));
        farthest = Math.max(farthest, blockDistance(originX + 15, originZ + 15));
        return farthest;
    }

    private static double closest(int chunkX, int chunkZ) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        double dx = axisGap(settings.centerX, originX, originX + 15);
        double dz = axisGap(settings.centerZ, originZ, originZ + 15);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double axisGap(int center, int min, int max) {
        if (center < min) {
            return min - center;
        }
        if (center > max) {
            return center - max;
        }
        return 0.0D;
    }

    private static double blockDistance(int x, int z) {
        return distance(x + 0.5D, z + 0.5D);
    }

    private static double distance(double x, double z) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        double dx = x - settings.centerX;
        double dz = z - settings.centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean fill(Chunk chunk, int x, int z, Block block, int fromY, int toY) {
        boolean changed = false;
        for (int y = fromY; y <= toY; y++) {
            changed |= setBlock(chunk, x, y, z, block);
        }
        return changed;
    }

    private static boolean setBlock(Chunk chunk, int x, int y, int z, Block block) {
        if (y < 0 || y > 255) {
            return false;
        }
        if (chunk.getBlock(x, y, z) == block && chunk.getBlockMetadata(x, y, z) == 0) {
            return false;
        }
        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        int section = y >> 4;
        ExtendedBlockStorage storage = storages[section];
        if (storage == null) {
            if (block == Blocks.air) {
                return false;
            }
            storage = new ExtendedBlockStorage(section << 4, !chunk.worldObj.provider.hasNoSky);
            storages[section] = storage;
        }
        storage.func_150818_a(x, y & 15, z, block);
        storage.setExtBlockMetadata(x, y & 15, z, 0);
        return true;
    }

    private static void discardEmptySections(Chunk chunk) {
        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        for (int section = 0; section < storages.length; section++) {
            ExtendedBlockStorage storage = storages[section];
            if (storage == null) {
                continue;
            }
            storage.removeInvalidBlocks();
            if (storage.isEmpty()) {
                storages[section] = null;
            }
        }
    }

    private static void removeOrphanTileEntities(Chunk chunk) {
        Map tileEntities = chunk.chunkTileEntityMap;
        if (tileEntities == null || tileEntities.isEmpty()) {
            return;
        }
        ArrayList values = new ArrayList(tileEntities.values());
        Iterator iterator = values.iterator();
        while (iterator.hasNext()) {
            TileEntity entity = (TileEntity) iterator.next();
            int x = entity.xCoord & 15;
            int y = entity.yCoord;
            int z = entity.zCoord & 15;
            Block block = chunk.getBlock(x, y, z);
            if (!block.hasTileEntity(chunk.getBlockMetadata(x, y, z))) {
                Iterator keys = new ArrayList(tileEntities.keySet()).iterator();
                while (keys.hasNext()) {
                    Object key = keys.next();
                    if (tileEntities.get(key) == entity) {
                        tileEntities.remove(key);
                        break;
                    }
                }
            }
        }
    }
}
