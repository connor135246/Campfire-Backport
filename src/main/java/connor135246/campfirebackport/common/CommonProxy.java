package connor135246.campfirebackport.common;

import java.util.ArrayList;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class CommonProxy
{

    public static Configuration config;

    public static CampfireBackportEventHandler handler = new CampfireBackportEventHandler();

    public static Logger modlog;
    
    public void preInit(FMLPreInitializationEvent event)
    {
        config = new Configuration(event.getSuggestedConfigurationFile());

        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);

        modlog = event.getModLog();

        CampfireBackportBlocks.preInit();
        GameRegistry.registerTileEntity(TileEntityCampfire.class, Reference.MODID + ":" + "campfire");
    }

    public void init(FMLInitializationEvent event)
    {

    }

    public void postInit(FMLPostInitializationEvent event)
    {
        syncConfig();
    }

    public static void syncConfig()
    {
        try
        {
            FMLCommonHandler.instance().bus().register(CampfireBackport.instance);
            CampfireBackportConfig.doConfig(config);
        }
        catch (Exception excep)
        {
            excep.printStackTrace();
        }
        finally
        {
            config.save();
        }
    }

    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire)
    {
        ;
    }
}