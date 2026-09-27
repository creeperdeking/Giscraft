package doc.fasterminecarts;

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
import org.lwjgl.opengl.GL12;

public class RenderFlowerGolem extends RenderIronGolem {
    @Override
    public void doRender(EntityIronGolem entity, double x, double y, double z, float yaw, float partial) {
        super.doRender(entity, x, y, z, yaw, partial);
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertex(0.0D, 0.0D, 0.0D);
        tessellator.addVertex(0.0D, 0.0D, 0.0D);
        tessellator.addVertex(0.0D, 0.0D, 0.0D);
        tessellator.addVertex(0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
    }

    @Override
    protected void renderEquippedItems(EntityIronGolem golem, float partial) {
        if (!(golem instanceof EntityFlowerGolem) || golem.getHoldRoseTick() == 0) {
            return;
        }
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glPushMatrix();
        float arm = ((ModelIronGolem) this.mainModel).ironGolemRightArm.rotateAngleX;
        GL11.glRotatef(5.0F + 180.0F * arm / (float) Math.PI, 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(-0.6875F, 1.25F, -0.9375F);
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        float scale = 0.8F;
        GL11.glScalef(scale, -scale, scale);
        int light = golem.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                (float) (light & 65535),
                (float) (light >> 16));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(TextureMap.locationBlocksTexture);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        drawPoppy();
        if (lighting) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }
        if (cull) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        GL11.glPopMatrix();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }

    @Override
    protected void renderEquippedItems(EntityLivingBase entity, float partial) {
        this.renderEquippedItems((EntityIronGolem) entity, partial);
    }

    private static void drawPoppy() {
        IIcon icon = Blocks.red_flower.getIcon(0, 0);
        float minU = icon.getMinU();
        float minV = icon.getMinV();
        float maxU = icon.getMaxU();
        float maxV = icon.getMaxV();
        float x0 = -0.45F;
        float x1 = 0.45F;
        float y0 = -0.5F;
        float y1 = 0.5F;
        float z0 = -0.45F;
        float z1 = 0.45F;
        GL11.glBegin(GL11.GL_QUADS);
        flowerPlane(x0, y1, z0, x0, y0, z0, x1, y0, z1, x1, y1, z1, minU, minV, maxU, maxV);
        flowerPlane(x0, y1, z1, x0, y0, z1, x1, y0, z0, x1, y1, z0, minU, minV, maxU, maxV);
        GL11.glEnd();
    }

    private static void flowerPlane(
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float minU,
            float minV,
            float maxU,
            float maxV) {
        GL11.glTexCoord2f(minU, minV);
        GL11.glVertex3f(x0, y0, z0);
        GL11.glTexCoord2f(minU, maxV);
        GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(maxU, maxV);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glTexCoord2f(maxU, minV);
        GL11.glVertex3f(x3, y3, z3);
        GL11.glTexCoord2f(maxU, minV);
        GL11.glVertex3f(x3, y3, z3);
        GL11.glTexCoord2f(maxU, maxV);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glTexCoord2f(minU, maxV);
        GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(minU, minV);
        GL11.glVertex3f(x0, y0, z0);
    }
}
