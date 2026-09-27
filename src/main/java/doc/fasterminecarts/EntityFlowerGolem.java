package doc.fasterminecarts;

import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityFlowerGolem extends EntityIronGolem {
    public EntityFlowerGolem(World world) {
        super(world);
        this.setPlayerCreated(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(17, Short.valueOf((short) 0));
        this.dataWatcher.addObject(18, Byte.valueOf((byte) 0));
        this.dataWatcher.addObject(19, Byte.valueOf((byte) 0));
    }

    void setHoldsFlower(boolean holds) {
        this.dataWatcher.updateObject(19, Byte.valueOf((byte) (holds ? 1 : 0)));
    }

    @Override
    public int getHoldRoseTick() {
        return this.dataWatcher.getWatchableObjectByte(19) != 0 ? 1 : 0;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("HoldsFlower", getHoldRoseTick() != 0);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("HoldsFlower")) {
            setHoldsFlower(tag.getBoolean("HoldsFlower"));
        } else {
            setHoldsFlower(this.rand.nextBoolean());
        }
    }
}
