package doc.fasterminecarts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Method;

import org.lwjgl.opengl.GL11;

public final class LightSlotGui {
    private static final ResourceLocation INVENTORY =
            new ResourceLocation("textures/gui/container/inventory.png");
    private static final ResourceLocation TORCH =
            new ResourceLocation("giscraft", "textures/gui/light_slot_torch.png");

    private LightSlotGui() {}

    public static void draw(int guiLeft, int guiTop) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null) {
            return;
        }
        LightCarry.ensureSlot(player);
        int x = guiLeft + LightSlot.X;
        int y = guiTop + LightSlot.Y;
        Gl.color(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager().bindTexture(INVENTORY);
        blit(x - 1, y - 1, 7, 83, 18, 18, 256);
        LightCarry carry = LightCarry.get(player);
        ItemStack stack = carry == null ? null : carry.getStack();
        if (stack != null) {
            return;
        }
        boolean blend = Gl.isEnabled(GL11.GL_BLEND);
        Gl.enable(GL11.GL_BLEND);
        Gl.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        minecraft.getTextureManager().bindTexture(TORCH);
        blit(x, y, 0, 0, 16, 16, 16);
        if (!blend) {
            Gl.disable(GL11.GL_BLEND);
        }
        Gl.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void apply(int entityId, ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) {
            return;
        }
        net.minecraft.entity.Entity entity = minecraft.theWorld.getEntityByID(entityId);
        if (entity instanceof EntityPlayer) {
            LightCarry carry = LightCarry.get((EntityPlayer) entity);
            if (carry != null) {
                carry.setSilent(stack);
            }
        }
    }

    private static void blit(int x, int y, int u, int v, int width, int height, int textureSize) {
        float unit = 1.0F / (float) textureSize;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, u * unit, (v + height) * unit);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, (u + width) * unit, (v + height) * unit);
        tessellator.addVertexWithUV(x + width, y, 0.0D, (u + width) * unit, v * unit);
        tessellator.addVertexWithUV(x, y, 0.0D, u * unit, v * unit);
        tessellator.draw();
    }

    /**
     * Angelica removes the GL11 entry points. GUI color and blend have to go through its state manager.
     */
    private static final class Gl {
        private static final Method COLOR = find("glColor4f", Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        private static final Method ENABLE = find("glEnable", Integer.TYPE);
        private static final Method DISABLE = find("glDisable", Integer.TYPE);
        private static final Method BLEND = find("glBlendFunc", Integer.TYPE, Integer.TYPE);
        private static final Method IS_ENABLED = find("glIsEnabled", Integer.TYPE);

        private static Method find(String name, Class<?>... params) {
            try {
                return Class.forName("com.gtnewhorizons.angelica.glsm.GLStateManager").getMethod(name, params);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
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

        static void blend(int source, int dest) {
            call(BLEND, () -> GL11.glBlendFunc(source, dest), source, dest);
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
