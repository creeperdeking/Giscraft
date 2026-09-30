package doc.fasterminecarts;

import java.lang.reflect.Method;

import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;

import org.lwjgl.opengl.GL11;

/**
 * Vanilla hangs the pumpkin with a negative Y scale so the carved face stays
 * upright after the living renderer flips the model. Those GL calls have to go
 * through Angelica's matrix or the block never follows the head.
 */
public class RenderFixedSnowman extends RenderSnowMan {
    @Override
    protected void renderEquippedItems(EntityLivingBase entity, float partial) {
        Gl.pushMatrix();
        ((ModelSnowMan) this.mainModel).head.postRender(0.0625F);
        Gl.translate(0.0F, -0.34375F, 0.0F);
        Gl.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        Gl.scale(0.625F, -0.625F, 0.625F);
        int light = entity.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                (float) (light & 65535),
                (float) (light >> 16));
        Gl.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(TextureMap.locationBlocksTexture);
        this.field_147909_c.renderBlockAsItem(Blocks.pumpkin, 0, 1.0F);
        Gl.popMatrix();
    }

    /**
     * Angelica draws entities through its own matrix stack. GL11 calls in this mod are not
     * rewritten onto that stack, so a pumpkin built with them never sits on the head.
     */
    private static final class Gl {
        private static final Method PUSH = find("glPushMatrix");
        private static final Method POP = find("glPopMatrix");
        private static final Method TRANSLATE = find("glTranslatef", Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method ROTATE = find("glRotatef", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method SCALE = find("glScalef", Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method COLOR = find("glColor4f", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);

        private static Method find(String name, Class<?>... params) {
            try {
                return Class.forName("com.gtnewhorizons.angelica.glsm.GLStateManager").getMethod(name, params);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        static void pushMatrix() {
            call(PUSH, GL11::glPushMatrix);
        }

        static void popMatrix() {
            call(POP, GL11::glPopMatrix);
        }

        static void translate(float x, float y, float z) {
            call(TRANSLATE, () -> GL11.glTranslatef(x, y, z), x, y, z);
        }

        static void rotate(float angle, float x, float y, float z) {
            call(ROTATE, () -> GL11.glRotatef(angle, x, y, z), angle, x, y, z);
        }

        static void scale(float x, float y, float z) {
            call(SCALE, () -> GL11.glScalef(x, y, z), x, y, z);
        }

        static void color(float r, float g, float b, float a) {
            call(COLOR, () -> GL11.glColor4f(r, g, b, a), r, g, b, a);
        }

        private static void call(Method method, Runnable fallback, Object... args) {
            if (method == null) {
                fallback.run();
                return;
            }
            try {
                method.invoke(null, args);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
        }
    }
}
