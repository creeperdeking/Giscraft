package doc.fasterminecarts;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.Name("FasterVanillaMinecarts")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"doc.fasterminecarts."})
public final class FasterVanillaMinecartsCore implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"doc.fasterminecarts.RailSpeedTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Object mcLocation = data.get("mcLocation");
        if (mcLocation instanceof File) {
            RailSpeedConfig.load((File) mcLocation);
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
