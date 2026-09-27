package doc.fasterminecarts;

import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class EntityGiantSquid extends EntitySquid {
    public EntityGiantSquid(World world) {
        super(world);
        this.setSize(9.5F, 9.5F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100.0D);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        this.entityDropItem(new ItemStack(Items.ender_pearl, 10), 0.0F);
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean isInWater() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY + (double) this.height * 0.5D);
        int z = MathHelper.floor_double(this.posZ);
        return this.worldObj.getBlock(x, y, z).getMaterial() == Material.water
                || this.worldObj.getBlock(x, MathHelper.floor_double(this.posY), z).getMaterial() == Material.water;
    }
}
