package doc.fasterminecarts;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.world.World;

public class EntityBareSnowman extends EntitySnowman {
    public EntityBareSnowman(World world) {
        super(world);
    }

    @Override
    public void setAttackTarget(EntityLivingBase target) {
        super.setAttackTarget(null);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distance) {
    }
}
