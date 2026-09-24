package doc.fasterminecarts;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * A chest lid that retains vanilla rendering but caps its opening angle.
 */
public final class LimitedChestLid extends ModelRenderer {
    private static final float MAXIMUM_OPEN_ANGLE =
            (float) Math.toRadians(85.0D);

    public LimitedChestLid(ModelBase model, ModelRenderer original) {
        super(model);

        textureWidth = original.textureWidth;
        textureHeight = original.textureHeight;
        cubeList.addAll(original.cubeList);

        rotationPointX = original.rotationPointX;
        rotationPointY = original.rotationPointY;
        rotationPointZ = original.rotationPointZ;
        offsetX = original.offsetX;
        offsetY = original.offsetY;
        offsetZ = original.offsetZ;
        mirror = original.mirror;
    }

    @Override
    public void render(float scale) {
        if (rotateAngleX < -MAXIMUM_OPEN_ANGLE) {
            rotateAngleX = -MAXIMUM_OPEN_ANGLE;
        } else if (rotateAngleX > MAXIMUM_OPEN_ANGLE) {
            rotateAngleX = MAXIMUM_OPEN_ANGLE;
        }

        super.render(scale);
    }
}
