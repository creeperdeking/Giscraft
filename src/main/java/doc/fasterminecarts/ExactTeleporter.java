package doc.fasterminecarts;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/**
 * Builds the exit on the scaled column, at the nearest ground or lava surface,
 * facing the same way as the portal that was entered.
 */
final class ExactTeleporter extends Teleporter {
    private final WorldServer world;

    ExactTeleporter(WorldServer world) {
        super(world);
        this.world = world;
    }

    @Override
    public void placeInPortal(Entity entity, double srcX, double srcY, double srcZ, float yaw) {
        if (world.provider.dimensionId == 1) {
            super.placeInPortal(entity, srcX, srcY, srcZ, yaw);
            return;
        }
        int[] target = PortalLink.destination(world, entity, srcX, srcY, srcZ);
        if (target == null) {
            super.placeInPortal(entity, srcX, srcY, srcZ, yaw);
            return;
        }
        boolean nether = world.provider.dimensionId == -1;
        boolean alongX = target[3] != 0;
        int span = nether ? 1 : PortalLink.SCALE;
        int[] spot = find(target[0], target[2], target[1], span);
        if (spot == null) {
            int[] height = chooseHeight(target[0], target[2], target[1], nether);
            if (height[1] != 0) {
                platform(target[0], height[0], target[2], alongX);
            }
            build(target[0], height[0], target[2], alongX);
            spot = find(target[0], target[2], height[0], span);
        }
        if (spot == null) {
            entity.setLocationAndAngles(
                    target[0] + 0.5D, target[1], target[2] + 0.5D, entity.rotationYaw, entity.rotationPitch);
        } else {
            entity.setLocationAndAngles(
                    spot[0] + 0.5D, spot[1] + 0.5D, spot[2] + 0.5D, entity.rotationYaw, entity.rotationPitch);
        }
        entity.motionX = entity.motionY = entity.motionZ = 0.0D;
    }

    /**
     * @return portal-bottom Y, and 1 when the spot is open air or a lava lake
     */
    private int[] chooseHeight(int x, int z, int preferred, boolean nether) {
        int top = world.getActualHeight() - 4;
        if (preferred < 1) {
            preferred = 1;
        }
        if (preferred > top) {
            preferred = top;
        }
        int bestY = Integer.MIN_VALUE;
        int bestScore = Integer.MAX_VALUE;
        boolean bestLava = false;
        for (int y = 1; y <= top; y++) {
            if (!open(x, y, z) || !open(x, y + 1, z) || !open(x, y + 2, z)) {
                continue;
            }
            boolean lavaFloor = lava(x, y - 1, z);
            boolean ground = solid(x, y - 1, z);
            if (!lavaFloor && !ground) {
                continue;
            }
            int score = Math.abs(y - preferred) * 2 + (lavaFloor && !ground ? 1 : 0);
            if (score < bestScore) {
                bestScore = score;
                bestY = y;
                bestLava = lavaFloor && !ground;
            }
        }
        if (bestY != Integer.MIN_VALUE) {
            int place = bestY;
            if (bestLava && bestY < top) {
                place = bestY + 1;
            }
            return new int[] {place, nether && bestLava ? 1 : 0};
        }
        if (nether) {
            int embed = closestNetherrack(x, z, preferred, top);
            if (embed != Integer.MIN_VALUE) {
                return new int[] {embed, 0};
            }
        }
        return new int[] {preferred, nether ? 1 : 0};
    }

    private int closestNetherrack(int x, int z, int preferred, int top) {
        int best = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = 1; y <= top; y++) {
            if (world.getBlock(x, y, z) != Blocks.netherrack) {
                continue;
            }
            int distance = Math.abs(y - preferred);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = y;
            }
        }
        return best;
    }

    private void platform(int x, int y, int z, boolean alongX) {
        int floor = y - 1;
        for (int along = -1; along <= 2; along++) {
            for (int side = -2; side <= 2; side++) {
                int blockX = alongX ? x + along : x + side;
                int blockZ = alongX ? z + side : z + along;
                world.setBlock(blockX, floor, blockZ, Blocks.netherrack, 0, 2);
            }
        }
    }

    private int[] find(int originX, int originZ, int preferredY, int span) {
        int height = world.getActualHeight();
        int bestX = 0;
        int bestY = 0;
        int bestZ = 0;
        double best = Double.MAX_VALUE;
        boolean found = false;
        for (int dx = 0; dx < span; dx++) {
            for (int dz = 0; dz < span; dz++) {
                int x = originX + dx;
                int z = originZ + dz;
                int y = height - 1;
                while (y >= 0) {
                    if (world.getBlock(x, y, z) != Blocks.portal) {
                        y--;
                        continue;
                    }
                    int bottom = y;
                    while (bottom > 0 && world.getBlock(x, bottom - 1, z) == Blocks.portal) {
                        bottom--;
                    }
                    double dy = bottom + 0.5D - preferredY;
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance < best) {
                        best = distance;
                        bestX = x;
                        bestY = bottom;
                        bestZ = z;
                        found = true;
                    }
                    y = bottom - 1;
                }
            }
        }
        return found ? new int[] {bestX, bestY, bestZ} : null;
    }

    private void build(int x, int y, int z, boolean alongX) {
        for (int across = 0; across < 4; across++) {
            for (int height = -1; height < 4; height++) {
                int blockX = alongX ? x + across - 1 : x;
                int blockZ = alongX ? z : z + across - 1;
                int blockY = y + height;
                boolean frame = across == 0 || across == 3 || height == -1 || height == 3;
                world.setBlock(blockX, blockY, blockZ, frame ? Blocks.obsidian : Blocks.portal, 0, 2);
            }
        }
        for (int across = 0; across < 4; across++) {
            for (int height = -1; height < 4; height++) {
                int blockX = alongX ? x + across - 1 : x;
                int blockZ = alongX ? z : z + across - 1;
                int blockY = y + height;
                world.notifyBlocksOfNeighborChange(blockX, blockY, blockZ, world.getBlock(blockX, blockY, blockZ));
            }
        }
    }

    private boolean solid(int x, int y, int z) {
        return world.getBlock(x, y, z).getMaterial().isSolid();
    }

    private boolean lava(int x, int y, int z) {
        return world.getBlock(x, y, z).getMaterial() == Material.lava;
    }

    private boolean open(int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block.getMaterial() == Material.air || block == Blocks.fire || block == Blocks.portal;
    }
}
