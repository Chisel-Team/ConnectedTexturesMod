package team.chisel.ctm.client.asm;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

// Should work with anything 1.8+, so no @MCVersion
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
public class CTMCorePlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "team.chisel.ctm.client.asm.CTMTransformer" };
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
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
