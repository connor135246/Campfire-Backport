package connor135246.campfirebackport.client.gui;

import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.client.config.GuiConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class GuiConfigCampfireBackport extends GuiConfig
{

    public GuiConfigCampfireBackport(GuiScreen parentScreen)
    {
        super(parentScreen,
                new ConfigElement(CampfireBackportConfig.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                Reference.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(CampfireBackportConfig.config.toString()));
    }

}
