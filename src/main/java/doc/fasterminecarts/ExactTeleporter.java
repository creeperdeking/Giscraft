package doc.fasterminecarts;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/**
 * Builds the exit on the scaled block and links only to a portal already standing there.
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
        int span = world.provider.dimensionId == -1 ? 1 : PortalLink.SPACING;
        int[] spot = find(target[0], target[2], target[1], span);
        if (spot == null) {
            build(target[0], target[1], target[2]);
            spot = find(target[0], target[2], target[1], span);
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

    private void build(int x, int y, int z) {
        for (int across = 0; across < 4; across++) {
            for (int height = -1; height < 4; height++) {
                int blockZ = z + across - 1;
                boolean frame = across == 0 || across == 3 || height == -1 || height == 3;
                world.setBlock(x, y + height, blockZ, frame ? Blocks.obsidian : Blocks.portal, 0, 2);
            }
        }
        for (int across = 0; across < 4; across++) {
            for (int height = -1; height < 4; height++) {
                int blockZ = z + across - 1;
                int blockY = y + height;
                world.notifyBlocksOfNeighborChange(x, blockY, blockZ, world.getBlock(x, blockY, blockZ));
            }
        }
    }
}
