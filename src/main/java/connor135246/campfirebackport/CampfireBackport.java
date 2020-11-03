package connor135246.campfirebackport;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.MC_VERSION,
        guiFactory = Reference.MOD_PACKAGE + ".client.gui.GuiFactoryCampfireBackport")
public class CampfireBackport
{

    @Instance
    public static CampfireBackport instance = new CampfireBackport();
    @SidedProxy(clientSide = Reference.MOD_PACKAGE + ".client.ClientProxy", serverSide = Reference.MOD_PACKAGE + ".common.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit(event);
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        proxy.serverLoad(event);
    }

}
