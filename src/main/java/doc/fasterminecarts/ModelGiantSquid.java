package doc.fasterminecarts;

import net.minecraft.client.model.ModelSquid;
import net.minecraft.entity.Entity;

public class ModelGiantSquid extends ModelSquid {
    @Override
    public void render(
            Entity entity,
            float limbSwing,
            float limbAmount,
            float age,
            float headYaw,
            float headPitch,
            float scale) {
        super.render(entity, limbSwing, limbAmount, age, headYaw, headPitch, scale * 10.0F);
    }
}
