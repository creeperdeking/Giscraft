package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Zombies are always adults, and never villagers. A baby that spawned on a
 * chicken is taken off it.
 */
public final class ZombieHandler {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityZombie)) {
            return;
        }
        EntityZombie zombie = (EntityZombie) event.entity;
        if (!zombie.isChild() && !zombie.isVillager()) {
            return;
        }
        if (zombie.isChild()) {
            if (zombie.ridingEntity instanceof EntityChicken) {
                EntityChicken chicken = (EntityChicken) zombie.ridingEntity;
                zombie.mountEntity(null);
                chicken.setDead();
            }
            zombie.setChild(false);
        }
        if (zombie.isVillager()) {
            zombie.setVillager(false);
        }
    }
}
