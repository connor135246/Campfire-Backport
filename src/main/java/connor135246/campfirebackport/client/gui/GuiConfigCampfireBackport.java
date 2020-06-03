package connor135246.campfirebackport.client.gui;

import java.util.List;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class GuiConfigCampfireBackport extends GuiConfig
{

    public GuiConfigCampfireBackport(GuiScreen parentScreen)
    {
        super(parentScreen,
                new ConfigElement(CommonProxy.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                Reference.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(CommonProxy.config.toString()));
    }

}
