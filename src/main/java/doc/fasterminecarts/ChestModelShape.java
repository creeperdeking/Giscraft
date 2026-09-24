package doc.fasterminecarts;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelLargeChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.util.Vec3;

import java.lang.reflect.Field;

/**
 * Expands vanilla chest model vertices without issuing additional OpenGL
 * commands. This keeps the renderer compatible with Angelica's cached TESRs.
 */
public final class ChestModelShape {
    private static final double SINGLE_HORIZONTAL_SCALE = 8.0D / 7.0D;
    private static final double DOUBLE_HORIZONTAL_SCALE = 16.0D / 15.0D;
    private static final double VERTICAL_SCALE = 8.0D / 7.0D;
    private static final double SINGLE_CENTER_X = 8.0D;
    private static final double DOUBLE_CENTER_X = 16.0D;
    private static final double CENTER_Y = 16.0D;
    private static final double CENTER_Z = 8.0D;

    private static final Field VERTEX_POSITIONS_FIELD = findVertexPositionsField();

    private ChestModelShape() {
    }

    public static void expand(ModelChest model) {
        boolean large = model.getClass() == ModelLargeChest.class;

        if (!large && model.getClass() != ModelChest.class) {
            return;
        }

        double scaleX = large
                ? DOUBLE_HORIZONTAL_SCALE
                : SINGLE_HORIZONTAL_SCALE;
        double centerX = large ? DOUBLE_CENTER_X : SINGLE_CENTER_X;

        expandPart(model.chestLid, scaleX, VERTICAL_SCALE,
                SINGLE_HORIZONTAL_SCALE, centerX);
        expandPart(model.chestBelow, scaleX, VERTICAL_SCALE,
                SINGLE_HORIZONTAL_SCALE, centerX);

        model.chestLid = new LimitedChestLid(model, model.chestLid);

        // Remove the lock geometry entirely. Some optimized renderers ignore
        // ModelRenderer visibility flags but still respect an empty cube list.
        model.chestKnob.cubeList.clear();
        model.chestKnob.showModel = false;
        model.chestKnob.isHidden = true;
    }

    private static void expandPart(
            ModelRenderer part,
            double scaleX,
            double scaleY,
            double scaleZ,
            double centerX) {

        part.rotationPointX = transformPoint(
                part.rotationPointX, centerX, scaleX);
        part.rotationPointY = transformPoint(
                part.rotationPointY, CENTER_Y, scaleY);
        part.rotationPointZ = transformPoint(
                part.rotationPointZ, CENTER_Z, scaleZ);

        for (Object entry : part.cubeList) {
            ModelBox box = (ModelBox) entry;

            try {
                PositionTextureVertex[] vertices =
                        (PositionTextureVertex[]) VERTEX_POSITIONS_FIELD.get(box);

                for (PositionTextureVertex vertex : vertices) {
                    Vec3 position = vertex.vector3D;
                    position.xCoord *= scaleX;
                    position.yCoord *= scaleY;
                    position.zCoord *= scaleZ;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "Could not expand the vanilla chest model.", e);
            }
        }
    }

    private static float transformPoint(
            float point,
            double center,
            double scale) {
        return (float) (center + (point - center) * scale);
    }

    private static Field findVertexPositionsField() {
        for (Field field : ModelBox.class.getDeclaredFields()) {
            if (field.getType() == PositionTextureVertex[].class) {
                field.setAccessible(true);
                return field;
            }
        }

        throw new IllegalStateException(
                "Could not find ModelBox vertex positions.");
    }
}
