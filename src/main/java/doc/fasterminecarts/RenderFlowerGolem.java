package doc.fasterminecarts;

import java.lang.reflect.Method;

import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderIronGolem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

public class RenderFlowerGolem extends RenderIronGolem {
    @Override
    protected void renderEquippedItems(EntityIronGolem golem, float partial) {
        if (!(golem instanceof EntityFlowerGolem) || golem.getHoldRoseTick() == 0) {
            return;
        }
        Gl.pushMatrix();
        float arm = ((ModelIronGolem) this.mainModel).ironGolemRightArm.rotateAngleX;
        Gl.rotate(5.0F + 180.0F * arm / (float) Math.PI, 1.0F, 0.0F, 0.0F);
        Gl.translate(-0.6875F, 1.25F, -0.9375F);
        Gl.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        Gl.scale(0.8F, -0.8F, 0.8F);
        int light = golem.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                (float) (light & 65535),
                (float) (light >> 16));
        Gl.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(TextureMap.locationBlocksTexture);
        boolean cull = Gl.isEnabled(GL11.GL_CULL_FACE);
        Gl.enable(GL11.GL_CULL_FACE);
        drawPoppy(light);
        if (!cull) {
            Gl.disable(GL11.GL_CULL_FACE);
        }
        Gl.popMatrix();
    }

    @Override
    protected void renderEquippedItems(EntityLivingBase entity, float partial) {
        this.renderEquippedItems((EntityIronGolem) entity, partial);
    }

    private static void drawPoppy(int light) {
        IIcon icon = Blocks.red_flower.getIcon(0, 0);
        double minU = icon.getMinU();
        double minV = icon.getMinV();
        double maxU = icon.getMaxU();
        double maxV = icon.getMaxV();
        double x0 = -0.45D;
        double x1 = 0.45D;
        double y0 = -0.5D;
        double y1 = 0.5D;
        double z0 = -0.45D;
        double z1 = 0.45D;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(light);
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        tessellator.addVertexWithUV(x0, y1, z0, minU, minV);
        tessellator.addVertexWithUV(x0, y0, z0, minU, maxV);
        tessellator.addVertexWithUV(x1, y0, z1, maxU, maxV);
        tessellator.addVertexWithUV(x1, y1, z1, maxU, minV);
        tessellator.addVertexWithUV(x1, y1, z1, minU, minV);
        tessellator.addVertexWithUV(x1, y0, z1, minU, maxV);
        tessellator.addVertexWithUV(x0, y0, z0, maxU, maxV);
        tessellator.addVertexWithUV(x0, y1, z0, maxU, minV);
        tessellator.addVertexWithUV(x0, y1, z1, minU, minV);
        tessellator.addVertexWithUV(x0, y0, z1, minU, maxV);
        tessellator.addVertexWithUV(x1, y0, z0, maxU, maxV);
        tessellator.addVertexWithUV(x1, y1, z0, maxU, minV);
        tessellator.addVertexWithUV(x1, y1, z0, minU, minV);
        tessellator.addVertexWithUV(x1, y0, z0, minU, maxV);
        tessellator.addVertexWithUV(x0, y0, z1, maxU, maxV);
        tessellator.addVertexWithUV(x0, y1, z1, maxU, minV);
        tessellator.draw();
    }

    /**
     * Angelica draws entities through its own matrix stack. GL11 calls in this mod are not
     * rewritten onto that stack, so a poppy built with them never lands in the raised hand.
     */
    private static final class Gl {
        private static final Method PUSH = find("glPushMatrix");
        private static final Method POP = find("glPopMatrix");
        private static final Method TRANSLATE = find("glTranslatef", Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method ROTATE = find("glRotatef", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method SCALE = find("glScalef", Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method COLOR = find("glColor4f", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method ENABLE = find("glEnable", Integer.TYPE);
        private static final Method DISABLE = find("glDisable", Integer.TYPE);
        private static final Method IS_ENABLED = find("glIsEnabled", Integer.TYPE);

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

        static void enable(int cap) {
            call(ENABLE, () -> GL11.glEnable(cap), cap);
        }

        static void disable(int cap) {
            call(DISABLE, () -> GL11.glDisable(cap), cap);
        }

        static boolean isEnabled(int cap) {
            if (IS_ENABLED == null) {
                return GL11.glIsEnabled(cap);
            }
            try {
                return ((Boolean) IS_ENABLED.invoke(null, Integer.valueOf(cap))).booleanValue();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
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
