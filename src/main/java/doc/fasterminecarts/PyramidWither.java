package doc.fasterminecarts;

import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.stats.AchievementList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.World;

/**
 * The pyramid keeps a bedrock copy of the wither base. Ordinary skulls sit on
 * it until they are swapped for wither-skeleton skulls, which calls a wither
 * and clears the skulls. The bedrock stays, so the base can be used again.
 */
public final class PyramidWither {
    private PyramidWither() {}

    public static void trySummon(World world, int x, int y, int z) {
        if (world == null || world.isRemote) {
            return;
        }
        if (summonIfReady(world, x, y, z, 1, 0)) {
            return;
        }
        summonIfReady(world, x, y, z, 0, 1);
    }

    private static boolean summonIfReady(World world, int x, int y, int z, int stepX, int stepZ) {
        for (int start = -2; start <= 0; start++) {
            int originX = x + start * stepX;
            int originZ = z + start * stepZ;
            if (!rowReady(world, originX, y, originZ, stepX, stepZ)) {
                continue;
            }
            clearSkulls(world, originX, y, originZ, stepX, stepZ);
            spawn(world, originX + stepX, y, originZ + stepZ);
            return true;
        }
        return false;
    }

    private static boolean rowReady(World world, int x, int y, int z, int stepX, int stepZ) {
        for (int index = 0; index < 3; index++) {
            int skullX = x + index * stepX;
            int skullZ = z + index * stepZ;
            if (!witherSkull(world, skullX, y, skullZ)) {
                return false;
            }
            if (world.getBlock(skullX, y - 1, skullZ) != Blocks.bedrock) {
                return false;
            }
        }
        return world.getBlock(x + stepX, y - 2, z + stepZ) == Blocks.bedrock;
    }

    private static boolean witherSkull(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != Blocks.skull) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntitySkull && ((TileEntitySkull) tile).func_145904_a() == 1;
    }

    private static void clearSkulls(World world, int x, int y, int z, int stepX, int stepZ) {
        for (int index = 0; index < 3; index++) {
            world.setBlockToAir(x + index * stepX, y, z + index * stepZ);
        }
    }

    private static void spawn(World world, int x, int y, int z) {
        EntityWither wither = new EntityWither(world);
        wither.setLocationAndAngles(x + 0.5D, y - 1.45D, z + 0.5D, wither.rotationYaw, 0.0F);
        wither.func_82206_m();
        if (!world.spawnEntityInWorld(wither)) {
            return;
        }
        List nearby = world.getEntitiesWithinAABB(
                EntityPlayer.class, wither.boundingBox.expand(50.0D, 50.0D, 50.0D));
        Iterator iterator = nearby.iterator();
        while (iterator.hasNext()) {
            EntityPlayer player = (EntityPlayer) iterator.next();
            player.triggerAchievement(AchievementList.field_150963_I);
        }
        world.playAuxSFXAtEntity(null, 1016, x, y, z, 0);
        for (int particle = 0; particle < 120; particle++) {
            world.spawnParticle(
                    "snowballpoof",
                    x + world.rand.nextDouble(),
                    (y - 2) + world.rand.nextDouble() * 3.9D,
                    z + world.rand.nextDouble(),
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }
}
