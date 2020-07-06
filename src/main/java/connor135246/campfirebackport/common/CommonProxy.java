package connor135246.campfirebackport.common;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.CommandNBT;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class CommonProxy
{

    public static Configuration config;
    public static boolean useDefaultConfig = false;

    public static CampfireBackportEventHandler handler = new CampfireBackportEventHandler();

    public static Logger modlog;

    public void preInit(FMLPreInitializationEvent event)
    {
        config = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);

        modlog = event.getModLog();

        // old (pre-1.4) config is very different. old configs will be safely renamed and a new config will be created.
        if (config.hasKey(Configuration.CATEGORY_GENERAL, "Regen Level"))
        {
            try
            {
                modlog.info(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config"));
                Files.move(config.getConfigFile(), new File(config.getConfigFile().getCanonicalPath() + "_1.3"));
                config = new Configuration(event.getSuggestedConfigurationFile());
            }
            catch (Exception excep)
            {
                modlog.error(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config.error.0"));
                modlog.error(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config.error.1"));
                modlog.error(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config.error.2"));
                useDefaultConfig = true;
            }
        }

        // Create Config Explanation File
        try
        {
            File explanation = new File(event.getModConfigurationDirectory(), Reference.README_FILENAME);
            if (explanation.createNewFile())
            {
                modlog.info(StatCollector.translateToLocal(Reference.MODID + ".preinit.create_explanation"));
                PrintWriter explanationWriter = new PrintWriter(new FileWriter(explanation));
                for (int i = 0; i < 197; ++i)
                    explanationWriter.println(StatCollector.translateToLocal(Reference.MODID + ".config.explanation." + i));
                explanationWriter.close();
            }
        }
        catch (Exception excep)
        {
            modlog.error(StatCollector.translateToLocal(Reference.MODID + ".preinit.create_explanation.error.0"));
            modlog.error(StatCollector.translateToLocal(Reference.MODID + ".preinit.create_explanation.error.1"));
        }

        CampfireBackportBlocks.preInit();
        GameRegistry.registerTileEntity(TileEntityCampfire.class, Reference.MODID + ":" + "campfire");
    }

    public void init(FMLInitializationEvent event)
    {
        FMLInterModComms.sendMessage("Waila", "register", "connor135246.campfirebackport.client.compat.CampfireBackportWailaDataProvider.register");
    }

    public void postInit(FMLPostInitializationEvent event)
    {
        config.load();
        syncConfig();
    }
    
    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandNBT());
    }

    public static void syncConfig()
    {
        try
        {
            if (!useDefaultConfig)
            {
                FMLCommonHandler.instance().bus().register(CampfireBackport.instance);
                CampfireBackportConfig.doConfig(config);
                config.save();
            }
            else
            {
                CampfireBackportConfig.doDefaultConfig();
            }
        }
        catch (Exception excep)
        {
            modlog.catching(excep);
        }
    }

    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire, Block colourer)
    {
        ;
    }

}