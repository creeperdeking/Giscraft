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
 * Links a nether portal to one overworld coordinate. Overworld columns eight
 * blocks apart land on neighbouring nether blocks, and a new overworld portal
 * closer than that is not allowed to light.
 */
public final class PortalLink {
    static final int SPACING = 8;

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
        int cellX = floorDiv(anchorX, SPACING) * SPACING;
        int cellZ = floorDiv(anchorZ, SPACING) * SPACING;
        int minX = Math.min(cellX, anchorX - (SPACING - 1));
        int maxX = Math.max(cellX + SPACING - 1, anchorX + (SPACING - 1));
        int minZ = Math.min(cellZ, anchorZ - (SPACING - 1));
        int maxZ = Math.max(cellZ + SPACING - 1, anchorZ + (SPACING - 1));
        int height = world.getActualHeight();
        int limit = SPACING * SPACING;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean sameCell = x >= cellX && x < cellX + SPACING && z >= cellZ && z < cellZ + SPACING;
                int dx = x - anchorX;
                int dz = z - anchorZ;
                if (!sameCell && dx * dx + dz * dz >= limit) {
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

    static boolean exitInsideWall(World nether, int[] columns) {
        if (!OceanBoundaryConfig.enabled) {
            return true;
        }
        int anchorX = columns[0];
        int anchorZ = columns[1];
        for (int index = 2; index < columns.length; index += 2) {
            anchorX = Math.min(anchorX, columns[index]);
            anchorZ = Math.min(anchorZ, columns[index + 1]);
        }
        int overworldX = anchorX * SPACING;
        int overworldZ = anchorZ * SPACING;
        long seed = nether.getSeed();
        OceanBoundaryMath.Settings settings = OceanBoundaryConfig.settings();
        for (int dz = -1; dz <= 2; dz++) {
            if (OceanBoundaryMath.intoWall(overworldX, overworldZ + dz, seed, settings) >= 0.0D) {
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
        World source = entity.worldObj;
        if (source != null && source != destination && source.provider != null
                && source.provider.dimensionId != dimension) {
            int[] corner = corner(source, x, y, z);
            x = corner[0];
            z = corner[2];
        }
        y = clamp(destination, y);
        if (dimension == -1) {
            return new int[] {floorDiv(x, SPACING), y, floorDiv(z, SPACING)};
        }
        return new int[] {x * SPACING, y, z * SPACING};
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
                return new int[] {x, y, z};
            }
            x = near[0];
            y = near[1];
            z = near[2];
        }
        while (y > 0 && portal(world, x, y - 1, z)) {
            y--;
        }
        int[] at = move(world, x, y, z, -1, 0);
        return move(world, at[0], at[1], at[2], 0, -1);
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
