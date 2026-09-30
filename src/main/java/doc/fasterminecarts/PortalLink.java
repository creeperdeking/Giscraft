package doc.fasterminecarts;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Links a nether portal to one overworld column. The horizontal spot is fixed.
 * A new overworld portal closer than 32 blocks to another is not allowed to
 * light, so the nether frames cannot occupy the same blocks. A portal is also
 * refused when it would share its exit with a portal that is already lit.
 */
public final class PortalLink {
    static final int SCALE = 8;
    static final int SEPARATION = 32;
    /**
     * A lit portal can be 21 blocks wide, so it can sit in link cells three
     * steps apart. Nether anchors closer than four blocks can therefore both
     * arrive through that one overworld portal.
     */
    private static final int NETHER_GAP = 4;

    private static final Logger LOG = LogManager.getLogger("Giscraft");
    private static Field teleporterField;
    private static boolean logged;

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote || !(event.world instanceof WorldServer)) {
            return;
        }
        install((WorldServer) event.world);
    }

    static boolean separated(World world, int[] columns) {
        int anchorX = columns[0];
        int anchorZ = columns[1];
        for (int index = 2; index < columns.length; index += 2) {
            anchorX = Math.min(anchorX, columns[index]);
            anchorZ = Math.min(anchorZ, columns[index + 1]);
        }
        int reach = SEPARATION - 1;
        int height = world.getActualHeight();
        int limit = SEPARATION * SEPARATION;
        for (int x = anchorX - reach; x <= anchorX + reach; x++) {
            for (int z = anchorZ - reach; z <= anchorZ + reach; z++) {
                int dx = x - anchorX;
                int dz = z - anchorZ;
                if (dx * dx + dz * dz >= limit) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    if (world.getBlock(x, y, z) == Blocks.portal) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * True when this unlit frame would be the only portal for its exit.
     * Overworld frames that reach into a link cell which already holds a portal
     * are refused. Nether anchors within {@link #NETHER_GAP} blocks of another
     * anchor are refused, including another portal in the same column.
     */
    static boolean onePartner(World world, int[] columns) {
        if (world.provider.dimensionId == -1) {
            int[] origin = anchor(columns);
            return netherAnchorsClear(world, origin[0], origin[1]);
        }
        return overworldCellsClear(world, columns);
    }

    private static int[] anchor(int[] columns) {
        int x = columns[0];
        int z = columns[1];
        for (int index = 2; index < columns.length; index += 2) {
            x = Math.min(x, columns[index]);
            z = Math.min(z, columns[index + 1]);
        }
        return new int[] {x, z};
    }

    private static boolean netherAnchorsClear(World world, int x, int z) {
        int reach = NETHER_GAP - 1;
        int height = world.getActualHeight();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int y = 0; y < height; y++) {
                    if (!portal(world, x + dx, y, z + dz)) {
                        continue;
                    }
                    int[] at = corner(world, x + dx, y, z + dz);
                    int apart = Math.max(Math.abs(at[0] - x), Math.abs(at[2] - z));
                    if (apart < NETHER_GAP) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean overworldCellsClear(World world, int[] columns) {
        int seen = 0;
        int[] seenX = new int[columns.length / 2];
        int[] seenZ = new int[columns.length / 2];
        int height = world.getActualHeight();
        for (int index = 0; index < columns.length; index += 2) {
            int cellX = floorDiv(columns[index], SCALE);
            int cellZ = floorDiv(columns[index + 1], SCALE);
            boolean repeat = false;
            for (int prior = 0; prior < seen; prior++) {
                if (seenX[prior] == cellX && seenZ[prior] == cellZ) {
                    repeat = true;
                    break;
                }
            }
            if (repeat) {
                continue;
            }
            seenX[seen] = cellX;
            seenZ[seen] = cellZ;
            seen++;
            int originX = cellX * SCALE;
            int originZ = cellZ * SCALE;
            for (int x = originX; x < originX + SCALE; x++) {
                for (int z = originZ; z < originZ + SCALE; z++) {
                    for (int y = 0; y < height; y++) {
                        if (portal(world, x, y, z)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    static boolean exitInsideWall(World nether, int[] columns) {
        if (!OceanBoundaryConfig.enabled) {
            return true;
        }
        int anchorX = columns[0];
        int anchorZ = columns[1];
        boolean alongX = false;
        for (int index = 2; index < columns.length; index += 2) {
            anchorX = Math.min(anchorX, columns[index]);
            anchorZ = Math.min(anchorZ, columns[index + 1]);
            if (columns[index] != columns[0]) {
                alongX = true;
            }
        }
        int overworldX = anchorX * SCALE;
        int overworldZ = anchorZ * SCALE;
        long seed = nether.getSeed();
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        for (int along = -1; along <= 2; along++) {
            int x = alongX ? overworldX + along : overworldX;
            int z = alongX ? overworldZ : overworldZ + along;
            if (OceanBoundaryMath.intoWall(x, z, seed, settings) >= 0.0D) {
                return false;
            }
        }
        return true;
    }

    static int[] destination(WorldServer destination, Entity entity, double srcX, double srcY, double srcZ) {
        int dimension = destination.provider.dimensionId;
        if (dimension != 0 && dimension != -1) {
            return null;
        }
        int x = MathHelper.floor_double(srcX);
        int y = MathHelper.floor_double(srcY);
        int z = MathHelper.floor_double(srcZ);
        int alongX = 0;
        World source = entity.worldObj;
        if (source != null && source != destination && source.provider != null
                && source.provider.dimensionId != dimension) {
            int[] corner = corner(source, x, y, z);
            x = corner[0];
            z = corner[2];
            alongX = corner[3];
        }
        y = clamp(destination, y);
        if (dimension == -1) {
            return new int[] {floorDiv(x, SCALE), y, floorDiv(z, SCALE), alongX};
        }
        return new int[] {x * SCALE, y, z * SCALE, alongX};
    }

    private static void install(WorldServer world) {
        try {
            Field field = teleporterField();
            Object current = field.get(world);
            if (current instanceof ExactTeleporter) {
                return;
            }
            field.set(world, new ExactTeleporter(world));
        } catch (Exception error) {
            throw new RuntimeException("Giscraft could not fix nether portal placement.", error);
        }
        if (!logged) {
            logged = true;
            LOG.info("Nether portals link at a fixed coordinate.");
        }
    }

    private static Field teleporterField() throws Exception {
        if (teleporterField != null) {
            return teleporterField;
        }
        Field found = null;
        for (Field candidate : WorldServer.class.getDeclaredFields()) {
            if (candidate.getType() != net.minecraft.world.Teleporter.class) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException("more than one teleporter field");
            }
            found = candidate;
        }
        if (found == null) {
            throw new IllegalStateException("missing teleporter field");
        }
        found.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(found, found.getModifiers() & ~Modifier.FINAL);
        teleporterField = found;
        return found;
    }

    private static int[] corner(World world, int x, int y, int z) {
        if (!portal(world, x, y, z)) {
            int[] near = nearby(world, x, y, z);
            if (near == null) {
                return new int[] {x, y, z, 0};
            }
            x = near[0];
            y = near[1];
            z = near[2];
        }
        while (y > 0 && portal(world, x, y - 1, z)) {
            y--;
        }
        int alongX = portal(world, x + 1, y, z) || portal(world, x - 1, y, z) ? 1 : 0;
        int[] at = alongX == 1 ? move(world, x, y, z, -1, 0) : move(world, x, y, z, 0, -1);
        return new int[] {at[0], at[1], at[2], alongX};
    }

    private static int[] nearby(World world, int x, int y, int z) {
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (portal(world, x + dx, y + dy, z + dz)) {
                        return new int[] {x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    private static int[] move(World world, int x, int y, int z, int stepX, int stepZ) {
        for (int step = 0; step < 21; step++) {
            int nextX = x + stepX;
            int nextZ = z + stepZ;
            if (!portal(world, nextX, y, nextZ)) {
                break;
            }
            x = nextX;
            z = nextZ;
        }
        return new int[] {x, y, z};
    }

    private static boolean portal(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == Blocks.portal;
    }

    private static int clamp(World world, int y) {
        int height = world.getActualHeight();
        if (y < 1) {
            return 1;
        }
        int top = height - 4;
        return y > top ? top : y;
    }

    static int floorDiv(int value, int divisor) {
        int quotient = value / divisor;
        if (value < 0 && value % divisor != 0) {
            quotient--;
        }
        return quotient;
    }
}
