package doc.fasterminecarts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

final class OceanBoundary {
    private static final Logger LOG = LogManager.getLogger("Giscraft");

    private OceanBoundary() {
    }

    static void apply(Chunk chunk, long seed) {
        if (!OceanBoundaryConfig.enabled || chunk == null) {
            return;
        }

        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        if (chunkIsInside(originX, originZ, settings)) {
            return;
        }

        byte[] biomes = chunk.getBiomeArray();
        boolean changed = false;
        int oceanBiome = BiomeGenBase.ocean.biomeID;
        int deepBiome = BiomeGenBase.deepOcean == null
                ? oceanBiome
                : BiomeGenBase.deepOcean.biomeID;
        int snowBiome = BiomeGenBase.icePlains == null
                ? oceanBiome
                : BiomeGenBase.icePlains.biomeID;
        int plainsBiome = BiomeGenBase.plains.biomeID;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = originX + localX;
                int worldZ = originZ + localZ;
                int band = OceanBoundaryMath.bandAt(worldX, worldZ, seed, settings);
                if (band != OceanBoundaryMath.BAND_OCEAN) {
                    writeOuterColumn(chunk, localX, localZ, worldX, worldZ, seed, settings, band);
                    int biome = band == OceanBoundaryMath.BAND_BEDROCK ? plainsBiome : snowBiome;
                    biomes[(localZ << 4) | localX] = (byte) biome;
                    changed = true;
                    continue;
                }

                double amount = OceanBoundaryMath.transitionAt(worldX, worldZ, seed, settings);
                if (amount <= 0.0D) {
                    continue;
                }

                int surface = findSurface(chunk, localX, localZ);
                int floor = OceanBoundaryMath.columnFloor(worldX, worldZ, seed, settings);
                int target = OceanBoundaryMath.blendHeight(surface, floor, amount);
                if (amount >= 1.0D || target != surface) {
                    reshapeColumn(chunk, localX, localZ, worldX, worldZ, seed, surface, target);
                    changed = true;
                }
                if (writeIceberg(chunk, localX, localZ, worldX, worldZ, seed, settings, floor)) {
                    changed = true;
                }
                if (writePyramid(chunk, localX, localZ, worldX, worldZ, settings)) {
                    changed = true;
                }

                if (target < settings.seaLevel) {
                    int biome = oceanBiome;
                    if (settings.useDeepOcean
                            && OceanBoundaryMath.distanceAt(worldX, worldZ, settings) >= settings.deepOceanStart) {
                        biome = deepBiome;
                    }
                    biomes[(localZ << 4) | localX] = (byte) biome;
                    changed = true;
                }
            }
        }

        if (!changed) {
            return;
        }

        removeOrphanTileEntities(chunk);
        discardEmptySections(chunk);
        chunk.generateSkylightMap();
        chunk.isModified = true;

        if (OceanBoundaryConfig.debugLogging) {
            LOG.info(
                    "Ocean boundary modified chunk {}, {}",
                    Integer.valueOf(chunk.xPosition),
                    Integer.valueOf(chunk.zPosition));
        }
    }

    static void stripOres(Chunk chunk, long seed) {
        if (OceanBoundaryConfig.affectOres || chunk == null) {
            return;
        }

        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        boolean changed = false;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (OceanBoundaryMath.transitionAt(originX + localX, originZ + localZ, seed, settings) < 1.0D) {
                    continue;
                }
                for (int y = 1; y < OceanBoundaryConfig.seaLevel; y++) {
                    Block block = chunk.getBlock(localX, y, localZ);
                    if (!isOre(block)) {
                        continue;
                    }
                    setBlock(chunk, localX, y, localZ, Blocks.stone);
                    changed = true;
                }
            }
        }
        if (changed) {
            chunk.isModified = true;
        }
    }

    static void finishEdge(Chunk chunk, long seed) {
        if (!OceanBoundaryConfig.enabled || chunk == null) {
            return;
        }
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        boolean snow = OceanBoundaryMath.chunkReachesSnow(originX, originZ, settings);
        boolean bergs = OceanBoundaryMath.chunkReachesIcebergs(originX, originZ, settings);
        boolean pyramids = OceanBoundaryMath.chunkReachesPyramid(originX, originZ, settings);
        if (!snow && !bergs && !pyramids) {
            return;
        }
        byte[] biomes = chunk.getBiomeArray();
        int snowBiome = BiomeGenBase.icePlains == null
                ? BiomeGenBase.ocean.biomeID
                : BiomeGenBase.icePlains.biomeID;
        int plainsBiome = BiomeGenBase.plains.biomeID;
        boolean changed = false;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = originX + localX;
                int worldZ = originZ + localZ;
                int band = OceanBoundaryMath.bandAt(worldX, worldZ, seed, settings);
                if (band == OceanBoundaryMath.BAND_OCEAN) {
                    if (bergs && writeIceberg(
                            chunk,
                            localX,
                            localZ,
                            worldX,
                            worldZ,
                            seed,
                            settings,
                            OceanBoundaryMath.seabedAt(worldX, worldZ, seed, settings))) {
                        changed = true;
                    }
                    if (pyramids && writePyramid(chunk, localX, localZ, worldX, worldZ, settings)) {
                        changed = true;
                    }
                    continue;
                }
                if (!snow) {
                    continue;
                }
                writeOuterColumn(chunk, localX, localZ, worldX, worldZ, seed, settings, band);
                int biome = band == OceanBoundaryMath.BAND_BEDROCK ? plainsBiome : snowBiome;
                biomes[(localZ << 4) | localX] = (byte) biome;
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        removeOrphanTileEntities(chunk);
        discardEmptySections(chunk);
        chunk.generateSkylightMap();
        chunk.isModified = true;
    }

    static Chunk voidChunk(World world, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        byte[] biomes = chunk.getBiomeArray();
        int snowBiome = BiomeGenBase.icePlains == null
                ? BiomeGenBase.ocean.biomeID
                : BiomeGenBase.icePlains.biomeID;
        for (int index = 0; index < biomes.length; index++) {
            biomes[index] = (byte) snowBiome;
        }
        chunk.generateSkylightMap();
        chunk.isTerrainPopulated = true;
        chunk.isLightPopulated = true;
        chunk.isModified = true;
        return chunk;
    }

    private static boolean chunkIsInside(int originX, int originZ, OceanBoundaryMath.Settings settings) {
        double margin = settings.coastlineNoise ? settings.coastlineAmplitude : 0.0D;
        double limit = settings.transitionStart - margin;
        if (limit <= 0.0D) {
            return false;
        }
        double farthest = 0.0D;
        for (int cornerX = 0; cornerX <= 15; cornerX += 15) {
            for (int cornerZ = 0; cornerZ <= 15; cornerZ += 15) {
                farthest = Math.max(farthest, OceanBoundaryMath.distanceAt(originX + cornerX, originZ + cornerZ, settings));
            }
        }
        return farthest < limit;
    }

    private static int findSurface(Chunk chunk, int x, int z) {
        for (int y = 255; y >= 1; y--) {
            Block block = chunk.getBlock(x, y, z);
            if (block == Blocks.air || block.getMaterial().isLiquid()) {
                continue;
            }
            return y;
        }
        return 0;
    }

    private static void reshapeColumn(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            long seed,
            int surface,
            int target) {
        boolean shore = target < OceanBoundaryConfig.seaLevel + 2;
        Block top = shore ? floorBlock(worldX, worldZ, seed) : Blocks.grass;
        Block fill = shore ? Blocks.sand : Blocks.dirt;
        int capBottom = Math.max(1, target - 3);

        for (int y = 255; y >= 1; y--) {
            Block desired = null;
            if (y > target) {
                desired = y < OceanBoundaryConfig.seaLevel ? Blocks.water : Blocks.air;
            } else if (y >= capBottom) {
                desired = y == target ? top : fill;
            } else if (target > surface && y > surface) {
                Block existing = chunk.getBlock(x, y, z);
                if (existing == Blocks.air || existing.getMaterial().isLiquid()) {
                    desired = Blocks.stone;
                }
            }
            if (desired == null) {
                continue;
            }
            Block existing = chunk.getBlock(x, y, z);
            if (existing == desired || (desired == Blocks.water && isWater(existing))) {
                continue;
            }
            setBlock(chunk, x, y, z, desired);
        }
    }

    private static Block floorBlock(int x, int z, long seed) {
        double gravel = OceanBoundaryMath.noise(x * 0.05D, z * 0.05D, seed + 17L);
        return gravel > 0.35D ? Blocks.gravel : Blocks.sand;
    }

    private static void writeOuterColumn(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            long seed,
            OceanBoundaryMath.Settings settings,
            int band) {
        if (band == OceanBoundaryMath.BAND_VOID) {
            fill(chunk, x, z, Blocks.air, 0, 255);
            return;
        }
        if (band == OceanBoundaryMath.BAND_FROZEN) {
            int surface = findSurface(chunk, x, z);
            int target = OceanBoundaryMath.seabedAt(worldX, worldZ, seed, settings);
            reshapeColumn(chunk, x, z, worldX, worldZ, seed, surface, target);
            freezeSurface(chunk, x, z, target, settings.seaLevel);
            writeIceberg(chunk, x, z, worldX, worldZ, seed, settings, target);
            return;
        }
        if (band == OceanBoundaryMath.BAND_BEDROCK) {
            writeMeadow(chunk, x, z, worldX, worldZ, seed, settings);
            return;
        }
        writeIceShelf(chunk, x, z, worldX, worldZ, seed, settings);
    }

    private static void writeIceShelf(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            long seed,
            OceanBoundaryMath.Settings settings) {
        int seabed = OceanBoundaryMath.seabedAt(worldX, worldZ, seed, settings);
        int crest = OceanBoundaryMath.iceCrest(worldX, worldZ, seed, settings);
        int bedrockTop = OceanBoundaryMath.bedrockTop(
                OceanBoundaryMath.intoWall(worldX, worldZ, seed, settings), settings);
        for (int y = 0; y <= 255; y++) {
            Block desired = Blocks.air;
            if (y <= bedrockTop) {
                desired = Blocks.bedrock;
            } else if (bedrockTop < seabed && y <= seabed) {
                desired = y == seabed ? floorBlock(worldX, worldZ, seed) : Blocks.stone;
            } else if (y <= crest) {
                desired = Blocks.packed_ice;
            } else if (y == bedrockTop + 1 && bedrockTop >= crest) {
                desired = Blocks.snow_layer;
            }
            if (chunk.getBlock(x, y, z) != desired) {
                setBlock(chunk, x, y, z, desired);
            }
        }
    }

    private static boolean writeIceberg(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            long seed,
            OceanBoundaryMath.Settings settings,
            int seabed) {
        int height = OceanBoundaryMath.icebergHeight(worldX, worldZ, seed, settings);
        if (height < 1) {
            return false;
        }
        int draft = (height * 2) / 3;
        int above = height - draft;
        if (draft < 1) {
            draft = 1;
            above = height - 1;
        }
        int below = draft * 3;
        int bottom = settings.seaLevel - below;
        int top = settings.seaLevel + above - 1;
        if (bottom <= seabed) {
            bottom = seabed + 1;
        }
        if (top > 240) {
            top = 240;
        }
        if (top < bottom) {
            return false;
        }
        for (int y = bottom; y <= top; y++) {
            setBlock(chunk, x, y, z, Blocks.packed_ice);
        }
        return true;
    }

    private static boolean writePyramid(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            OceanBoundaryMath.Settings settings) {
        int top = OceanBoundaryMath.pyramidTop(worldX, worldZ, settings);
        if (top < 1) {
            return false;
        }
        int ground = 0;
        for (int y = top; y >= 1; y--) {
            Block block = chunk.getBlock(x, y, z);
            if (block == Blocks.air
                    || block == Blocks.brick_block
                    || block == Blocks.ice
                    || block == Blocks.packed_ice
                    || isWater(block)) {
                continue;
            }
            ground = y;
            break;
        }
        if (top <= ground) {
            return false;
        }
        for (int y = ground + 1; y <= top; y++) {
            if (chunk.getBlock(x, y, z) != Blocks.brick_block) {
                setBlock(chunk, x, y, z, Blocks.brick_block);
            }
        }
        return true;
    }

    private static void freezeSurface(Chunk chunk, int x, int z, int seabed, int seaLevel) {
        int surface = seaLevel - 1;
        if (surface <= seabed) {
            return;
        }
        setBlock(chunk, x, surface, z, Blocks.ice);
    }

    static void stripLooseSnow(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        boolean changed = false;
        for (int pass = 0; pass < 8; pass++) {
            boolean removed = false;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 255; y >= 1; y--) {
                        if (chunk.getBlock(x, y, z) != Blocks.snow_layer) {
                            continue;
                        }
                        Block below = chunk.getBlock(x, y - 1, z);
                        if (below != Blocks.ice && below != Blocks.packed_ice && below != Blocks.air) {
                            continue;
                        }
                        setBlock(chunk, x, y, z, Blocks.air);
                        removed = true;
                        changed = true;
                    }
                }
            }
            if (!removed) {
                break;
            }
        }
        if (changed) {
            chunk.isModified = true;
        }
    }

    private static void fill(Chunk chunk, int x, int z, Block block, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            if (chunk.getBlock(x, y, z) != block) {
                setBlock(chunk, x, y, z, block);
            }
        }
    }

    private static boolean isWater(Block block) {
        return block == Blocks.water || block == Blocks.flowing_water;
    }

    private static boolean isOre(Block block) {
        if (block instanceof BlockOre) {
            return true;
        }
        String name = block.getUnlocalizedName();
        return name != null && name.toLowerCase().contains("ore");
    }

    private static void writeTomb(Chunk chunk, int x, int z, int worldX, int worldZ, int ground, int tomb) {
        Block brick = Blocks.stonebrick;
        fill(chunk, x, z, Blocks.bedrock, 0, ground - 1);
        if (tomb == OceanBoundaryMath.TOMB_PORCH) {
            setBlock(chunk, x, ground, z, Blocks.stone_slab, 5);
            Flowers.Kind flower = Flowers.kind(Math.floorMod(worldX * 31 + worldZ * 17, Flowers.count()));
            setBlock(chunk, x, ground + 1, z, flower.block, flower.meta);
            int airFrom = ground + 2;
            if (flower.tall && ground + 2 <= 254) {
                setBlock(chunk, x, ground + 2, z, flower.block, flower.meta | 8);
                airFrom = ground + 3;
            }
            if (airFrom <= 255) {
                fill(chunk, x, z, Blocks.air, airFrom, 255);
            }
            return;
        }

        int floorMeta = 1;
        if (tomb == OceanBoundaryMath.TOMB_DOOR || tomb == OceanBoundaryMath.TOMB_CORNER) {
            floorMeta = 3;
        } else if (tomb == OceanBoundaryMath.TOMB_WALL || tomb == OceanBoundaryMath.TOMB_WINDOW) {
            floorMeta = 0;
        }
        setBlock(chunk, x, ground, z, brick, floorMeta);
        if (tomb == OceanBoundaryMath.TOMB_CORNER) {
            setBlock(chunk, x, ground + 1, z, brick, 3);
            setBlock(chunk, x, ground + 2, z, brick, 3);
            setBlock(chunk, x, ground + 3, z, brick, 3);
        } else if (tomb == OceanBoundaryMath.TOMB_WALL) {
            setBlock(chunk, x, ground + 1, z, brick, 0);
            setBlock(chunk, x, ground + 2, z, brick, 2);
            setBlock(chunk, x, ground + 3, z, brick, 0);
        } else if (tomb == OceanBoundaryMath.TOMB_WINDOW) {
            setBlock(chunk, x, ground + 1, z, brick, 0);
            setBlock(chunk, x, ground + 2, z, Blocks.iron_bars, 0);
            setBlock(chunk, x, ground + 3, z, brick, 0);
        } else if (tomb == OceanBoundaryMath.TOMB_DOOR) {
            fill(chunk, x, z, Blocks.air, ground + 1, ground + 2);
            setBlock(chunk, x, ground + 3, z, brick, 3);
        } else if (tomb == OceanBoundaryMath.TOMB_COFFIN) {
            setBlock(chunk, x, ground + 1, z, Blocks.stone_slab, 5);
            fill(chunk, x, z, Blocks.air, ground + 2, ground + 3);
        } else {
            fill(chunk, x, z, Blocks.air, ground + 1, ground + 3);
        }
        setBlock(chunk, x, ground + 4, z, Blocks.stone_slab, 5);
        if (ground + 5 <= 255) {
            fill(chunk, x, z, Blocks.air, ground + 5, 255);
        }
    }

    private static void writeMeadow(
            Chunk chunk,
            int x,
            int z,
            int worldX,
            int worldZ,
            long seed,
            OceanBoundaryMath.Settings settings) {
        int ground = OceanBoundaryMath.bedrockPlateau(settings);
        if (ground < 2) {
            ground = 2;
        }
        if (ground > 250) {
            ground = 250;
        }
        int tomb = OceanBoundaryMath.tombPart(worldX, worldZ, seed);
        if (tomb != OceanBoundaryMath.TOMB_NONE) {
            writeTomb(chunk, x, z, worldX, worldZ, ground, tomb);
            return;
        }

        int pond = OceanBoundaryMath.pondDepth(worldX, worldZ, seed);
        if (pond > 0) {
            int waterBottom = pond > 1 ? ground - 1 : ground;
            fill(chunk, x, z, Blocks.bedrock, 0, waterBottom - 1);
            fill(chunk, x, z, Blocks.water, waterBottom, ground);
            if (ground + 1 <= 255) {
                fill(chunk, x, z, Blocks.air, ground + 1, 255);
            }
            return;
        }

        fill(chunk, x, z, Blocks.bedrock, 0, ground - 1);
        setBlock(chunk, x, ground, z, Blocks.grass);
        int kind = OceanBoundaryMath.flowerKind(worldX, worldZ, seed);
        int airFrom = ground + 1;
        if (kind >= 0 && ground + 2 <= 255) {
            Flowers.Kind flower = Flowers.kind(kind);
            setBlock(chunk, x, ground + 1, z, flower.block, flower.meta);
            if (flower.tall && ground + 2 <= 255) {
                setBlock(chunk, x, ground + 2, z, flower.block, flower.meta | 8);
                airFrom = ground + 3;
            } else {
                airFrom = ground + 2;
            }
        }
        if (airFrom <= 255) {
            fill(chunk, x, z, Blocks.air, airFrom, 255);
        }
    }

    private static void setBlock(Chunk chunk, int x, int y, int z, Block block) {
        setBlock(chunk, x, y, z, block, 0);
    }

    private static void setBlock(Chunk chunk, int x, int y, int z, Block block, int meta) {
        if (y < 0 || y > 255) {
            return;
        }
        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        int section = y >> 4;
        ExtendedBlockStorage storage = storages[section];
        if (storage == null) {
            if (block == Blocks.air) {
                return;
            }
            storage = new ExtendedBlockStorage(
                    section << 4,
                    !chunk.worldObj.provider.hasNoSky);
            storages[section] = storage;
        }
        storage.func_150818_a(x, y & 15, z, block);
        storage.setExtBlockMetadata(x, y & 15, z, meta);
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
        if (tileEntities.isEmpty()) {
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
                removeTileEntity(tileEntities, entity);
            }
        }
    }

    private static void removeTileEntity(Map tileEntities, TileEntity entity) {
        Iterator keys = new ArrayList(tileEntities.keySet()).iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            if (tileEntities.get(key) == entity) {
                tileEntities.remove(key);
                return;
            }
        }
    }
}
