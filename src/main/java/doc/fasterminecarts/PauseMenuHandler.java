package doc.fasterminecarts;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;

public final class PauseMenuHandler {
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_GAP = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private static int screenHeight;

    public static int titleY() {
        return blockTop(screenHeight);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInitPauseMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiIngameMenu)) {
            return;
        }

        GuiIngameMenu menu = (GuiIngameMenu) event.gui;
        event.buttonList.clear();

        int x = menu.width / 2 - 100;
        int y = firstButtonY(menu.height);
        String quitLabel = Minecraft.getMinecraft().isIntegratedServerRunning()
                ? I18n.format("menu.returnToMenu")
                : I18n.format("menu.disconnect");

        event.buttonList.add(new GuiButton(
                4, x, y, I18n.format("menu.returnToGame")));
        event.buttonList.add(new GuiButton(
                0, x, y + 24, I18n.format("menu.options")));
        event.buttonList.add(new GuiButton(
                1, x, y + 48, quitLabel));
    }

    private static int firstButtonY(int height) {
        screenHeight = height;
        return blockTop(height) + TITLE_HEIGHT + TITLE_GAP;
    }

    private static int blockTop(int height) {
        int buttonsHeight = BUTTON_HEIGHT * 3 + BUTTON_GAP * 2;
        int blockHeight = TITLE_HEIGHT + TITLE_GAP + buttonsHeight;
        return Math.max(0, (height - blockHeight) / 2);
    }
}
