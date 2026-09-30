package doc.fasterminecarts;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;

public final class OuterLife {
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.world;
        if (!OceanBoundaryConfig.enabled || world.isRemote || world.provider.dimensionId != 0) {
            return;
        }
        Chunk chunk = event.getChunk();
        LifeData data = LifeData.get(world);
        long key = LifeData.key(chunk.xPosition, chunk.zPosition);
        cullSquids(chunk);
        if (data.seeded(key)) {
            return;
        }
        seedChunk(world, chunk);
        data.mark(key);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.dimensionId != 0) {
            return;
        }
        if (!OceanBoundaryConfig.enabled || (event.world.getTotalWorldTime() % 400L) != 0L) {
            return;
        }
        Iterator players = event.world.playerEntities.iterator();
        while (players.hasNext()) {
            net.minecraft.entity.player.EntityPlayer player =
                    (net.minecraft.entity.player.EntityPlayer) players.next();
            refillNear(event.world, player.posX, player.posY, player.posZ);
        }
    }

    private static void seedChunk(World world, Chunk chunk) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        long seed = world.getSeed();
        Random random = new Random(seed ^ (chunk.xPosition * 341873128712L) ^ (chunk.zPosition * 132897987541L));
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        boolean meadow = false;
        boolean ice = false;
        boolean deep = false;
        for (int localX = 0; localX < 16; localX += 4) {
            for (int localZ = 0; localZ < 16; localZ += 4) {
                int worldX = originX + localX;
                int worldZ = originZ + localZ;
                int band = OceanBoundaryMath.bandAt(worldX, worldZ, seed, settings);
                if (band == OceanBoundaryMath.BAND_BEDROCK) {
                    meadow = true;
                } else if (band == OceanBoundaryMath.BAND_ICE) {
                    ice = true;
                } else if (band == OceanBoundaryMath.BAND_OCEAN
                        && settings.useDeepOcean
                        && OceanBoundaryMath.distanceAt(worldX, worldZ, settings) >= settings.deepOceanStart) {
                    deep = true;
                }
            }
        }
        if (meadow) {
            if (random.nextFloat() < 0.22F) {
                trySpawnGolem(world, chunk, random);
            }
            if (random.nextFloat() < 0.7F) {
                trySpawnOcelot(world, chunk, random);
            }
            if (random.nextFloat() < 0.35F) {
                trySpawnOcelot(world, chunk, random);
            }
        }
        if (ice && random.nextFloat() < 0.28F) {
            trySpawnSnowman(world, chunk, random);
        }
        if (deep && squidKeeper(chunk.xPosition, chunk.zPosition) && !chunkHasSquid(chunk)) {
            trySpawnSquid(world, chunk, random, settings);
        }
    }

    private static void refillNear(World world, double x, double y, double z) {
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        Random random = world.rand;
        AxisAlignedBB near = AxisAlignedBB.getBoundingBox(x - 80.0D, y - 32.0D, z - 80.0D, x + 80.0D, y + 32.0D, z + 80.0D);
        if (world.getEntitiesWithinAABB(EntityFlowerGolem.class, near).size() < 2) {
            trySpawnGolem(world, randomColumn(world, random, x, z, 28, 72), random);
        }
        if (world.getEntitiesWithinAABB(EntityOcelot.class, near).size() < 6) {
            trySpawnOcelot(world, randomColumn(world, random, x, z, 24, 64), random);
        }
        if (world.getEntitiesWithinAABB(EntitySnowman.class, near).size() < 3) {
            trySpawnSnowman(world, randomColumn(world, random, x, z, 24, 64), random);
        }
        AxisAlignedBB wide = AxisAlignedBB.getBoundingBox(x - 96.0D, 0.0D, z - 96.0D, x + 96.0D, 255.0D, z + 96.0D);
        if (world.getEntitiesWithinAABB(EntityGiantSquid.class, wide).size() < 1 && random.nextBoolean()) {
            trySpawnSquid(world, randomColumn(world, random, x, z, 32, 90), random, settings);
        }
    }

    private static Chunk randomColumn(World world, Random random, double x, double z, int min, int max) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = min + random.nextDouble() * (max - min);
        int blockX = (int) Math.floor(x + Math.cos(angle) * distance);
        int blockZ = (int) Math.floor(z + Math.sin(angle) * distance);
        if (!world.getChunkProvider().chunkExists(blockX >> 4, blockZ >> 4)) {
            return null;
        }
        return world.getChunkFromBlockCoords(blockX, blockZ);
    }

    private static void trySpawnGolem(World world, Chunk chunk, Random random) {
        if (chunk == null) {
            return;
        }
        int[] spot = findColumn(chunk, random, Blocks.grass, 3, OceanBoundaryMath.BAND_BEDROCK);
        if (spot == null) {
            return;
        }
        EntityFlowerGolem golem = new EntityFlowerGolem(world);
        golem.setHoldsFlower(random.nextBoolean());
        golem.setPosition(spot[0] + 0.5D, spot[1], spot[2] + 0.5D);
        if (world.getCollidingBoundingBoxes(golem, golem.boundingBox).isEmpty()) {
            world.spawnEntityInWorld(golem);
        }
    }

    private static void trySpawnOcelot(World world, Chunk chunk, Random random) {
        if (chunk == null) {
            return;
        }
        int[] spot = findColumn(chunk, random, Blocks.grass, 2, OceanBoundaryMath.BAND_BEDROCK);
        if (spot == null) {
            return;
        }
        EntityOcelot ocelot = new EntityOcelot(world);
        ocelot.setTamed(false);
        ocelot.setPosition(spot[0] + 0.5D, spot[1], spot[2] + 0.5D);
        if (world.getCollidingBoundingBoxes(ocelot, ocelot.boundingBox).isEmpty()) {
            world.spawnEntityInWorld(ocelot);
        }
    }

    private static boolean squidKeeper(int chunkX, int chunkZ) {
        if (Math.floorMod(chunkX, 8) != 0 || Math.floorMod(chunkZ, 8) != 0) {
            return false;
        }
        int cellX = Math.floorDiv(chunkX, 8);
        int cellZ = Math.floorDiv(chunkZ, 8);
        return Math.floorMod(cellX + cellZ, 2) == 0;
    }

    private static boolean chunkHasSquid(Chunk chunk) {
        List[] lists = chunk.entityLists;
        for (int section = 0; section < lists.length; section++) {
            if (lists[section] == null || lists[section].isEmpty()) {
                continue;
            }
            for (int index = 0; index < lists[section].size(); index++) {
                if (lists[section].get(index) instanceof EntityGiantSquid) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void cullSquids(Chunk chunk) {
        boolean keep = squidKeeper(chunk.xPosition, chunk.zPosition);
        boolean kept = false;
        List[] lists = chunk.entityLists;
        for (int section = 0; section < lists.length; section++) {
            if (lists[section] == null || lists[section].isEmpty()) {
                continue;
            }
            Object[] copy = lists[section].toArray();
            for (int index = 0; index < copy.length; index++) {
                if (!(copy[index] instanceof EntityGiantSquid)) {
                    continue;
                }
                EntityGiantSquid squid = (EntityGiantSquid) copy[index];
                if (keep && !kept) {
                    kept = true;
                    double surface = OceanBoundaryConfig.settings().seaLevel - 10.0D;
                    if (squid.posY < surface - 1.0D) {
                        squid.setPosition(squid.posX, surface, squid.posZ);
                    }
                    continue;
                }
                squid.setDead();
            }
        }
    }

    private static void trySpawnSnowman(World world, Chunk chunk, Random random) {
        if (chunk == null) {
            return;
        }
        int[] spot = findColumn(chunk, random, Blocks.packed_ice, 2, OceanBoundaryMath.BAND_ICE);
        if (spot == null) {
            return;
        }
        EntityBareSnowman snowman = new EntityBareSnowman(world);
        snowman.setPosition(spot[0] + 0.5D, spot[1], spot[2] + 0.5D);
        if (world.getCollidingBoundingBoxes(snowman, snowman.boundingBox).isEmpty()) {
            world.spawnEntityInWorld(snowman);
        }
    }

    private static void trySpawnSquid(World world, Chunk chunk, Random random, OceanBoundaryMath.Settings settings) {
        if (chunk == null) {
            return;
        }
        int[] spot = findSquidColumn(chunk, random, world.getSeed(), settings);
        if (spot == null) {
            return;
        }
        EntityGiantSquid squid = new EntityGiantSquid(world);
        squid.setPosition(spot[0] + 0.5D, spot[1], spot[2] + 0.5D);
        world.spawnEntityInWorld(squid);
    }

    private static int[] findColumn(Chunk chunk, Random random, Block ground, int airNeeded, int band) {
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        long seed = chunk.worldObj.getSeed();
        for (int attempt = 0; attempt < 12; attempt++) {
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            if (OceanBoundaryMath.bandAt(originX + localX, originZ + localZ, seed, settings) != band) {
                continue;
            }
            int surface = surfaceAbove(chunk, localX, localZ);
            if (surface < 2 || surface + airNeeded > 255) {
                continue;
            }
            if (chunk.getBlock(localX, surface - 1, localZ) != ground) {
                continue;
            }
            boolean open = true;
            for (int rise = 0; rise < airNeeded; rise++) {
                Block above = chunk.getBlock(localX, surface + rise, localZ);
                if (above.getMaterial().blocksMovement()) {
                    open = false;
                    break;
                }
            }
            if (open) {
                return new int[] { originX + localX, surface, originZ + localZ };
            }
        }
        return null;
    }

    private static int[] findSquidColumn(
            Chunk chunk,
            Random random,
            long seed,
            OceanBoundaryMath.Settings settings) {
        int originX = chunk.xPosition << 4;
        int originZ = chunk.zPosition << 4;
        for (int attempt = 0; attempt < 8; attempt++) {
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            int worldX = originX + localX;
            int worldZ = originZ + localZ;
            if (OceanBoundaryMath.bandAt(worldX, worldZ, seed, settings) != OceanBoundaryMath.BAND_OCEAN) {
                continue;
            }
            if (!settings.useDeepOcean
                    || OceanBoundaryMath.distanceAt(worldX, worldZ, settings) < settings.deepOceanStart) {
                continue;
            }
            int floor = surfaceAbove(chunk, localX, localZ);
            if (floor < 2) {
                continue;
            }
            int depth = 0;
            for (int y = floor; y < settings.seaLevel && y < 255; y++) {
                Block block = chunk.getBlock(localX, y, localZ);
                if (block != Blocks.water && block != Blocks.flowing_water) {
                    break;
                }
                depth++;
            }
            if (depth < 16) {
                continue;
            }
            int nearSurface = settings.seaLevel - 10;
            if (nearSurface < floor + 2) {
                nearSurface = floor + 2;
            }
            return new int[] { worldX, nearSurface, worldZ };
        }
        return null;
    }

    private static int surfaceAbove(Chunk chunk, int x, int z) {
        for (int y = 255; y >= 1; y--) {
            if (chunk.getBlock(x, y, z).getMaterial().blocksMovement()) {
                return y + 1;
            }
        }
        return 1;
    }

    public static final class LifeData extends WorldSavedData {
        private static final String NAME = "giscraft_outer_life";
        private final Set seeded = new HashSet();

        public LifeData(String name) {
            super(name);
        }

        static LifeData get(World world) {
            LifeData data = (LifeData) world.loadItemData(LifeData.class, NAME);
            if (data == null) {
                data = new LifeData(NAME);
                world.setItemData(NAME, data);
            }
            return data;
        }

        boolean seeded(long key) {
            return seeded.contains(Long.valueOf(key));
        }

        void mark(long key) {
            seeded.add(Long.valueOf(key));
            markDirty();
        }

        static long key(int chunkX, int chunkZ) {
            return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            seeded.clear();
            int[] values = tag.getIntArray("Seeded");
            for (int index = 0; index + 1 < values.length; index += 2) {
                seeded.add(Long.valueOf(key(values[index], values[index + 1])));
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            int[] values = new int[seeded.size() * 2];
            int index = 0;
            Iterator iterator = seeded.iterator();
            while (iterator.hasNext() && index + 1 < values.length) {
                long key = ((Long) iterator.next()).longValue();
                values[index++] = (int) key;
                values[index++] = (int) (key >> 32);
            }
            tag.setIntArray("Seeded", values);
        }
    }
}
