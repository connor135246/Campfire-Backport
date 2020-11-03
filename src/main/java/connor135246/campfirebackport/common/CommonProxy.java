package connor135246.campfirebackport.common;

import org.apache.logging.log4j.Logger;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager.SendConfigMessage;
import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.CommandCampfireBackport;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy
{

    public static Logger modlog;

    public static boolean isThaumcraftLoaded = false;
    public static boolean isMineTweaker3Loaded = false;

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

        CampfireBackportConfig.prepareConfig(event);
        CampfireBackportBlocks.preInit();
        GameRegistry.registerTileEntity(TileEntityCampfire.class, Reference.MODID + ":" + "campfire");
    }

    public void init(FMLInitializationEvent event)
    {
        FMLInterModComms.sendMessage("Waila", "register", Reference.MOD_PACKAGE + ".client.compat.waila.CampfireBackportWailaDataProvider.register");
    }

    public void postInit(FMLPostInitializationEvent event)
    {
        modCompat();
        CampfireBackportConfig.doConfig(0, true);
    }

    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandCampfireBackport());
    }

    //

    /**
     * checks for specific mod compatibility
     */
    public static void modCompat()
    {
        if (Loader.isModLoaded("MineTweaker3"))
        {
            try
            {
                Class.forName(Reference.MOD_PACKAGE + ".common.compat.crafttweaker.CampfireBackportCraftTweaking").getDeclaredMethod("postInit").invoke(null);
                isMineTweaker3Loaded = true;
            }
            catch (Exception excep)
            {
                modlog.error("Error while initializing MineTweaker3 (CraftTweaker) compat! Please report this bug!");
            }
        }

        if (Loader.isModLoaded("Thaumcraft"))
        {
            try
            {
                Class.forName(Reference.MOD_PACKAGE + ".common.compat.thaumcraft.CampfireBackportWandTriggerManager").getDeclaredMethod("postInit")
                        .invoke(null);
                isThaumcraftLoaded = true;
            }
            catch (Exception excep)
            {
                modlog.error("Error while initializing Thaumcraft compat! Please report this bug!");
            }
        }
    }

    /**
     * Makes campfire smoke particles client side.
     */
    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire, Block colourer, int meta)
    {
        ;
    }

}