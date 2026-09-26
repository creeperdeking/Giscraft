package doc.fasterminecarts;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraftforge.client.event.GuiScreenEvent;

public final class OptionsMenuHandler {
    private static final int SNOOPER_BUTTON_ID = 104;

    @SubscribeEvent
    public void onInitOptions(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiOptions)) {
            return;
        }

        for (Object entry : event.buttonList) {
            GuiButton button = (GuiButton) entry;
            if (button.id == SNOOPER_BUTTON_ID) {
                button.displayString = "Mods";
            }
        }
    }

    @SubscribeEvent
    public void onOptionsClick(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiOptions) || event.button.id != SNOOPER_BUTTON_ID) {
            return;
        }

        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.gameSettings.saveOptions();
        minecraft.displayGuiScreen(new GuiModList(event.gui));
    }
}
