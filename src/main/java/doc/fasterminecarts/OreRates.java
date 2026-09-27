package doc.fasterminecarts;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

/**
 * Vein bases for world generation. IC2 copper, tin, and uranium on land are
 * two thirds of the previous half-rate, so deep ocean — whose bases already
 * place twice that half-rate — averages three times the land amount. Vanilla
 * ores are halved in deep ocean only.
 */
public final class OreRates {
    private OreRates() {
    }

    public static int scaleIc2Base(int base, World world, int chunkX, int chunkZ, int kind) {
        if (base <= 0 || !isOverworld(world)) {
            return base;
        }
        if (!isDeepOceanChunk(world, chunkX, chunkZ)) {
            return landBase(base, chunkX, chunkZ);
        }
        int scaled = deepBase(base, kind);
        if (scaled < 1) {
            return 1;
        }
        return scaled;
    }

    public static int scaleVanillaVeins(int count, World world, int chunkX, int chunkZ) {
        if (count <= 0 || !isOverworld(world) || !isDeepOceanChunk(world, chunkX, chunkZ)) {
            return count;
        }
        if (count == 1) {
            return (((chunkX * 73428767) ^ (chunkZ * 912931)) & 1) == 0 ? 1 : 0;
        }
        return count / 2;
    }

    private static int landBase(int base, int chunkX, int chunkZ) {
        int halved = base / 2;
        int doubled = halved * 2;
        int low = doubled / 3;
        int extra = doubled % 3;
        if (extra > 0 && Math.floorMod((chunkX * 73428767) ^ (chunkZ * 912931), 3) < extra) {
            low++;
        }
        return low;
    }

    private static int deepBase(int base, int kind) {
        if (kind == 15) {
            return (base * 32 + 7) / 15;
        }
        if (kind == 25) {
            return (base * 28 + 12) / 25;
        }
        if (kind == 20) {
            return (base * 37 + 10) / 20;
        }
        return base * 2;
    }

    private static boolean isOverworld(World world) {
        return world != null && world.provider != null && world.provider.dimensionId == 0;
    }

    private static boolean isDeepOceanChunk(World world, int chunkX, int chunkZ) {
        if (BiomeGenBase.deepOcean == null) {
            return false;
        }
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }
        byte[] biomes = chunk.getBiomeArray();
        if (biomes == null || biomes.length == 0) {
            return false;
        }
        int deep = BiomeGenBase.deepOcean.biomeID;
        int count = 0;
        for (int index = 0; index < biomes.length; index++) {
            if ((biomes[index] & 255) == deep) {
                count++;
            }
        }
        return count * 2 >= biomes.length;
    }
}
