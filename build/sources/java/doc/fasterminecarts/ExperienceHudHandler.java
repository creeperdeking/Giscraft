package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public final class ExperienceHudHandler {
    private static final int COMPACT_HUD_BASE_HEIGHT = 32;

    @SubscribeEvent
    public void onRenderExperience(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            GuiIngameForge.left_height = COMPACT_HUD_BASE_HEIGHT;
            GuiIngameForge.right_height = COMPACT_HUD_BASE_HEIGHT;
        } else if (event.type == RenderGameOverlayEvent.ElementType.EXPERIENCE) {
            event.setCanceled(true);
        }
    }
}
