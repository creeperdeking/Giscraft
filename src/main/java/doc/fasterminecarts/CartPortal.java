package doc.fasterminecarts;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cartload keeps a moving minecart's own chunk loaded, but a portal crossing
 * recreates the cart in the other dimension. The chunks around that exit are
 * loaded ahead of the cart so it can arrive, after which Cartload follows it there.
 */
public final class CartPortal implements ForgeChunkManager.LoadingCallback {
    private static final double SPEED_EPSILON = 1.0E-4D;
    private static final int LOOK_AHEAD = 16;
    private static final int HOLD_TICKS = 200;
    private static final int CHUNK_DEPTH = 9;

    private final Map<UUID, Crossing> crossings = new HashMap<UUID, Crossing>();
    private int tick;

    private CartPortal() {}

    static void register(Giscraft mod) {
        CartPortal portal = new CartPortal();
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, portal);
        MinecraftForge.EVENT_BUS.register(portal);
        FMLCommonHandler.instance().bus().register(portal);
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (int index = 0; index < tickets.size(); index++) {
            ForgeChunkManager.releaseTicket(tickets.get(index));
        }
    }

    @SubscribeEvent
    public void onMinecart(MinecartUpdateEvent event) {
        EntityMinecart cart = event.minecart;
        if (cart == null || cart.isDead || cart.worldObj == null || cart.worldObj.isRemote) {
            return;
        }
        if (cart.timeUntilPortal > 0) {
            return;
        }
        int destination = otherSide(cart.dimension);
        if (destination == Integer.MIN_VALUE) {
            return;
        }
        if (!moving(cart) && !insidePortal(cart)) {
            return;
        }
        int[] portal = portalAhead(cart);
        if (portal == null) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || !server.getAllowNether()) {
            return;
        }
        WorldServer target = server.worldServerForDimension(destination);
        if (target == null) {
            return;
        }
        double scale = cart.worldObj.provider.getMovementFactor() / target.provider.getMovementFactor();
        int destX = MathHelper.floor_double((portal[0] + 0.5D) * scale);
        int destZ = MathHelper.floor_double((portal[1] + 0.5D) * scale);
        hold(cart.getUniqueID(), target, destX >> 4, destZ >> 4);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || crossings.isEmpty()) {
            return;
        }
        tick++;
        Iterator<Crossing> pending = crossings.values().iterator();
        while (pending.hasNext()) {
            Crossing crossing = pending.next();
            if (tick - crossing.lastSeen <= HOLD_TICKS) {
                continue;
            }
            ForgeChunkManager.releaseTicket(crossing.ticket);
            pending.remove();
        }
    }

    private void hold(UUID cartId, WorldServer target, int chunkX, int chunkZ) {
        Crossing crossing = crossings.get(cartId);
        if (crossing == null) {
            crossing = new Crossing();
            crossings.put(cartId, crossing);
        }
        if (crossing.ticket != null && crossing.ticket.world != target) {
            ForgeChunkManager.releaseTicket(crossing.ticket);
            crossing.ticket = null;
        }
        if (crossing.ticket == null) {
            crossing.ticket = ForgeChunkManager.requestTicket(
                    Giscraft.instance, target, ForgeChunkManager.Type.NORMAL);
            if (crossing.ticket == null) {
                crossings.remove(cartId);
                return;
            }
            crossing.ticket.setChunkListDepth(CHUNK_DEPTH);
        }
        crossing.lastSeen = tick;
        Set<ChunkCoordIntPair> wanted = neighborhood(chunkX, chunkZ);
        Set<ChunkCoordIntPair> held = crossing.ticket.getChunkList();
        for (ChunkCoordIntPair chunk : held) {
            if (!wanted.contains(chunk)) {
                ForgeChunkManager.unforceChunk(crossing.ticket, chunk);
            }
        }
        for (ChunkCoordIntPair chunk : wanted) {
            if (held.contains(chunk)) {
                continue;
            }
            target.getChunkFromChunkCoords(chunk.chunkXPos, chunk.chunkZPos);
            ForgeChunkManager.forceChunk(crossing.ticket, chunk);
        }
    }

    private static Set<ChunkCoordIntPair> neighborhood(int chunkX, int chunkZ) {
        Set<ChunkCoordIntPair> chunks = new HashSet<ChunkCoordIntPair>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunks.add(new ChunkCoordIntPair(chunkX + dx, chunkZ + dz));
            }
        }
        return chunks;
    }

    private static int otherSide(int dimension) {
        if (dimension == 0) {
            return -1;
        }
        if (dimension == -1) {
            return 0;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean moving(EntityMinecart cart) {
        return Math.abs(cart.motionX) + Math.abs(cart.motionY) + Math.abs(cart.motionZ) >= SPEED_EPSILON;
    }

    private static boolean insidePortal(EntityMinecart cart) {
        int x = MathHelper.floor_double(cart.posX);
        int y = MathHelper.floor_double(cart.posY);
        int z = MathHelper.floor_double(cart.posZ);
        return cart.worldObj.getBlock(x, y, z) == Blocks.portal
                || cart.worldObj.getBlock(x, y + 1, z) == Blocks.portal;
    }

    private static int[] portalAhead(EntityMinecart cart) {
        double motionX = cart.motionX;
        double motionZ = cart.motionZ;
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        int steps = horizontal < SPEED_EPSILON ? 0 : LOOK_AHEAD;
        double stepX = steps == 0 ? 0.0D : motionX / horizontal;
        double stepZ = steps == 0 ? 0.0D : motionZ / horizontal;
        double sideX = -stepZ;
        double sideZ = stepX;
        for (int step = 0; step <= steps; step++) {
            for (int side = -1; side <= 1; side++) {
                double x = cart.posX + stepX * step + sideX * side;
                double z = cart.posZ + stepZ * step + sideZ * side;
                int blockX = MathHelper.floor_double(x);
                int blockZ = MathHelper.floor_double(z);
                int blockY = MathHelper.floor_double(cart.posY);
                for (int dy = -1; dy <= 1; dy++) {
                    if (cart.worldObj.getBlock(blockX, blockY + dy, blockZ) == Blocks.portal) {
                        return new int[] {blockX, blockZ};
                    }
                }
            }
        }
        return null;
    }

    private static final class Crossing {
        private ForgeChunkManager.Ticket ticket;
        private int lastSeen;
    }
}
