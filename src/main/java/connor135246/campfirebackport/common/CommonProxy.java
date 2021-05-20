package connor135246.campfirebackport.common;

import org.apache.logging.log4j.Logger;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.recipes.CampfireBackportRecipes;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager.SendConfigMessage;
import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.CommandCampfireBackport;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy
{

    public static Logger modlog;

    public static CampfireBackportEventHandler handler = new CampfireBackportEventHandler();

    public static SimpleNetworkWrapper simpleNetwork;

    public void preInit(FMLPreInitializationEvent event)
    {
        modlog = event.getModLog();

        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
        FMLCommonHandler.instance().bus().register(CampfireBackport.instance);

        simpleNetwork = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID);
        simpleNetwork.registerMessage(SendConfigMessage.Handler.class, SendConfigMessage.class, 1, Side.CLIENT);

        CampfireBackportCompat.preInit();

        CampfireBackportConfig.prepareConfig(event);
        CampfireBackportConfig.doConfig(11, true);

        CampfireBackportBlocks.preInit();

        GameRegistry.registerTileEntity(TileEntityCampfire.class, Reference.MODID + ":" + "campfire");
    }

    public void init(FMLInitializationEvent event)
    {
        FMLInterModComms.sendMessage("Waila", "register", Reference.MOD_PACKAGE + ".client.compat.waila.CampfireBackportWailaDataProvider.register");
    }

    public void postInit(FMLPostInitializationEvent event)
    {
        CampfireBackportCompat.postInit();

        CampfireBackportRecipes.postInit();
    }

    public void loadComplete(FMLLoadCompleteEvent event)
    {
        CampfireBackportConfig.doConfig(4, true);
    }

    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandCampfireBackport());
    }

    //

    /**
     * Makes campfire smoke particles.
     */
    public void generateBigSmokeParticles(World world, int x, int y, int z, String type, boolean signalFire)
    {
        ;
    }

    /**
     * Makes vanilla smoke particles above items in the campfire.
     */
    public void generateSmokeOverItems(World world, int x, int y, int z, int meta, ItemStack[] items)
    {
        ;
    }

}
