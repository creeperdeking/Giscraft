package doc.fasterminecarts;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

/**
 * Refuses a nether portal whose fixed overworld exit would sit on or past the
 * ice wall, and refuses an overworld portal lit within eight blocks of another.
 */
public final class NetherLink {
    private static final int NETHER = -1;

    private NetherLink() {}

    public static boolean canLight(World world, int x, int y, int z) {
        if (world == null || world.provider == null) {
            return true;
        }
        int dimension = world.provider.dimensionId;
        if (dimension != 0 && dimension != NETHER) {
            return true;
        }
        int[] columns = columns(world, x, y, z, 1);
        if (columns == null) {
            columns = columns(world, x, y, z, 2);
        }
        if (columns == null) {
            return true;
        }
        if (dimension == 0) {
            return PortalLink.separated(world, columns);
        }
        return PortalLink.exitInsideWall(world, columns);
    }

    /**
     * Interior columns of an unlit obsidian frame, or null when this axis would not light.
     * Matches {@code BlockPortal.Size} for axis 1 (X) and axis 2 (Z).
     */
    private static int[] columns(World world, int x, int y, int z, int axis) {
        int[] facing = BlockPortal.field_150001_a[axis];
        int backward = facing[0];
        int across = facing[1];
        int bottom = y;
        for (int start = y; bottom > start - 21 && bottom > 0 && opens(world, x, bottom - 1, z); bottom--) {
        }
        int behind = span(world, x, bottom, z, backward) - 1;
        if (behind < 0) {
            return null;
        }
        int originX = x + behind * Direction.offsetX[backward];
        int originZ = z + behind * Direction.offsetZ[backward];
        int width = span(world, originX, bottom, originZ, across);
        if (width < 2 || width > 21) {
            return null;
        }
        int height = 0;
        int portalBlocks = 0;
        while (height < 21) {
            if (!rowOpens(world, originX, originZ, bottom + height, width, across, backward)) {
                break;
            }
            portalBlocks += portalsInRow(world, originX, originZ, bottom + height, width, across);
            height++;
        }
        if (!capped(world, originX, originZ, bottom + height, width, across)) {
            height = 0;
        }
        if (height < 3 || height > 21 || portalBlocks != 0) {
            return null;
        }
        int[] found = new int[width * 2];
        for (int column = 0; column < width; column++) {
            found[column * 2] = originX + column * Direction.offsetX[across];
            found[column * 2 + 1] = originZ + column * Direction.offsetZ[across];
        }
        return found;
    }

    private static boolean rowOpens(
            World world,
            int originX,
            int originZ,
            int y,
            int width,
            int across,
            int backward) {
        for (int column = 0; column < width; column++) {
            int columnX = originX + column * Direction.offsetX[across];
            int columnZ = originZ + column * Direction.offsetZ[across];
            if (!opens(world.getBlock(columnX, y, columnZ))) {
                return false;
            }
            int side = column == 0 ? backward : column == width - 1 ? across : -1;
            if (side >= 0) {
                Block edge = world.getBlock(
                        columnX + Direction.offsetX[side],
                        y,
                        columnZ + Direction.offsetZ[side]);
                if (edge != Blocks.obsidian) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int portalsInRow(
            World world, int originX, int originZ, int y, int width, int across) {
        int count = 0;
        for (int column = 0; column < width; column++) {
            int columnX = originX + column * Direction.offsetX[across];
            int columnZ = originZ + column * Direction.offsetZ[across];
            if (world.getBlock(columnX, y, columnZ) == Blocks.portal) {
                count++;
            }
        }
        return count;
    }

    private static boolean capped(
            World world, int originX, int originZ, int y, int width, int across) {
        for (int column = 0; column < width; column++) {
            int columnX = originX + column * Direction.offsetX[across];
            int columnZ = originZ + column * Direction.offsetZ[across];
            if (world.getBlock(columnX, y, columnZ) != Blocks.obsidian) {
                return false;
            }
        }
        return true;
    }

    private static int span(World world, int x, int y, int z, int direction) {
        int stepX = Direction.offsetX[direction];
        int stepZ = Direction.offsetZ[direction];
        int steps = 0;
        while (steps < 22) {
            if (!opens(world, x + stepX * steps, y, z + stepZ * steps)) {
                break;
            }
            if (world.getBlock(x + stepX * steps, y - 1, z + stepZ * steps) != Blocks.obsidian) {
                break;
            }
            steps++;
        }
        Block end = world.getBlock(x + stepX * steps, y, z + stepZ * steps);
        return end == Blocks.obsidian ? steps : 0;
    }

    private static boolean opens(World world, int x, int y, int z) {
        return opens(world.getBlock(x, y, z));
    }

    private static boolean opens(Block block) {
        return block.getMaterial() == Material.air
                || block == Blocks.fire
                || block == Blocks.portal;
    }
}
