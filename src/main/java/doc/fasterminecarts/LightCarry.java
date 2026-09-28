package doc.fasterminecarts;

import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class LightCarry implements IExtendedEntityProperties, IInventory {
    public static final String ID = "giscraft_light";
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(Giscraft.MOD_ID);

    private static final Queue<Runnable> CLIENT_TASKS = new ConcurrentLinkedQueue<Runnable>();
    private static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<Runnable>();
    private static final Method ADD_SLOT = findAddSlot();

    private EntityPlayer player;
    private ItemStack stack;

    public static void register() {
        CHANNEL.registerMessage(SyncHandler.class, LightSlotSync.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(SetHandler.class, LightSlotSet.class, 1, Side.SERVER);
        LightCarry handler = new LightCarry();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    public static LightCarry get(EntityPlayer player) {
        if (player == null) {
            return null;
        }
        return (LightCarry) player.getExtendedProperties(ID);
    }

    public static void ensureSlot(EntityPlayer player) {
        LightCarry carry = get(player);
        Container container = player.inventoryContainer;
        if (carry == null || container == null) {
            return;
        }
        for (Object slot : container.inventorySlots) {
            if (slot instanceof LightSlot) {
                return;
            }
        }
        try {
            ADD_SLOT.invoke(container, new LightSlot(carry));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Method findAddSlot() {
        Method found = null;
        for (Method method : Container.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1
                    || parameters[0] != Slot.class
                    || method.getReturnType() != Slot.class) {
                continue;
            }
            found = method;
            if ("addSlotToContainer".equals(method.getName()) || "func_75146_a".equals(method.getName())) {
                break;
            }
        }
        if (found == null) {
            throw new IllegalStateException("Giscraft could not add the light slot.");
        }
        found.setAccessible(true);
        return found;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setSilent(ItemStack replacement) {
        stack = replacement;
    }

    public static void scheduleClient(Runnable task) {
        CLIENT_TASKS.add(task);
    }

    public static void scheduleServer(Runnable task) {
        SERVER_TASKS.add(task);
    }

    @SubscribeEvent
    public void construct(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer
                && event.entity.getExtendedProperties(ID) == null) {
            event.entity.registerExtendedProperties(ID, new LightCarry());
        }
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer) {
            ensureSlot((EntityPlayer) event.entity);
        }
    }

    @SubscribeEvent
    public void drops(PlayerDropsEvent event) {
        LightCarry carry = get(event.entityPlayer);
        if (carry == null || carry.stack == null) {
            return;
        }
        event.drops.add(new EntityItem(
                event.entityPlayer.worldObj,
                event.entityPlayer.posX,
                event.entityPlayer.posY + 0.5D,
                event.entityPlayer.posZ,
                carry.stack.copy()));
        carry.stack = null;
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone event) {
        boolean keep = event.entityPlayer.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");
        if (event.wasDeath && !keep) {
            return;
        }
        LightCarry previous = get(event.original);
        LightCarry next = get(event.entityPlayer);
        if (previous == null || next == null || previous.stack == null) {
            return;
        }
        next.stack = previous.stack.copy();
    }

    @SubscribeEvent
    public void loggedIn(PlayerLoggedInEvent event) {
        ensureSlot(event.player);
        sync(event.player);
    }

    @SubscribeEvent
    public void respawn(PlayerRespawnEvent event) {
        ensureSlot(event.player);
        sync(event.player);
    }

    @SubscribeEvent
    public void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.target instanceof EntityPlayer)) {
            return;
        }
        LightCarry carry = get((EntityPlayer) event.target);
        if (carry == null || !(event.entityPlayer instanceof EntityPlayerMP)) {
            return;
        }
        CHANNEL.sendTo(
                new LightSlotSync(event.target.getEntityId(), carry.stack),
                (EntityPlayerMP) event.entityPlayer);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            drain(CLIENT_TASKS);
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            drain(SERVER_TASKS);
        }
    }

    private static void drain(Queue<Runnable> tasks) {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }
    }

    static void sync(EntityPlayer player) {
        if (player.worldObj == null || player.worldObj.isRemote || !(player instanceof EntityPlayerMP)) {
            return;
        }
        LightSlotSync message = new LightSlotSync(player.getEntityId(), get(player).stack);
        for (Object other : player.worldObj.playerEntities) {
            if (other instanceof EntityPlayerMP) {
                CHANNEL.sendTo(message, (EntityPlayerMP) other);
            }
        }
    }

    static void acceptClientRequest(EntityPlayerMP player, ItemStack requested) {
        ItemStack stored = requested == null ? null : requested.copy();
        if (stored != null && !Luminous.canHold(stored)) {
            stored = null;
        }
        if (stored != null && stored.stackSize > 64) {
            stored.stackSize = 64;
        }
        LightCarry carry = get(player);
        if (carry == null) {
            return;
        }
        carry.stack = stored;
        sync(player);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        if (stack != null) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            compound.setTag("GiscraftLightSlot", tag);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey("GiscraftLightSlot")) {
            stack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("GiscraftLightSlot"));
        }
    }

    @Override
    public void init(Entity entity, World world) {
        player = (EntityPlayer) entity;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stack;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (stack == null) {
            return null;
        }
        ItemStack result;
        if (stack.stackSize <= amount) {
            result = stack;
            stack = null;
        } else {
            result = stack.splitStack(amount);
            if (stack.stackSize <= 0) {
                stack = null;
            }
        }
        markDirty();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack replacement) {
        stack = replacement;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "Light";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (player == null || player.worldObj == null) {
            return;
        }
        if (player.worldObj.isRemote) {
            CHANNEL.sendToServer(new LightSlotSet(stack));
            return;
        }
        sync(player);
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer user) {
        return user == player;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack candidate) {
        return Luminous.canHold(candidate);
    }

    public static final class SyncHandler implements IMessageHandler<LightSlotSync, IMessage> {
        @Override
        public IMessage onMessage(final LightSlotSync message, MessageContext context) {
            scheduleClient(new Runnable() {
                @Override
                public void run() {
                    Giscraft.proxy.applyLightSlot(message.entityId, message.stack);
                }
            });
            return null;
        }
    }

    public static final class SetHandler implements IMessageHandler<LightSlotSet, IMessage> {
        @Override
        public IMessage onMessage(final LightSlotSet message, final MessageContext context) {
            scheduleServer(new Runnable() {
                @Override
                public void run() {
                    acceptClientRequest(context.getServerHandler().playerEntity, message.stack);
                }
            });
            return null;
        }
    }
}
