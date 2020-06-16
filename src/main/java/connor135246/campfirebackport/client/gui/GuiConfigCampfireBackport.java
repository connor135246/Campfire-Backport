package connor135246.campfirebackport.client.gui;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class GuiConfigCampfireBackport extends GuiConfig
{

    public GuiConfigCampfireBackport(GuiScreen parentScreen)
    {
        super(parentScreen,
                new ConfigElement(CampfireBackport.proxy.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                Reference.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(CampfireBackport.proxy.config.toString()));
    }

}
