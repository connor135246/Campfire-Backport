package connor135246.campfirebackport.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.dispenser.BehaviourShovel;
import connor135246.campfirebackport.common.dispenser.BehaviourSword;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
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

        if (CampfireBackportConfig.dispenserBehaviours)
            registerShovelsAndSwordsInDispenser();
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

    @SuppressWarnings("null")
    public static void registerShovelsAndSwordsInDispenser()
    {
        ArrayList<Item> bitemlist = CampfireBackportConfig.dispenserBehavioursBlacklistItems;
        boolean print = CampfireBackportConfig.printDispenserBehaviours;
        FMLControlledNamespacedRegistry<Item> itemreg = GameData.getItemRegistry();
        FMLControlledNamespacedRegistry<Block> blockreg = GameData.getBlockRegistry();

        iteratorLoop: for (Item item : itemreg.typeSafeIterable())
        {
            if (item instanceof ItemSpade)
            {
                for (Item bitem : bitemlist)
                {
                    if (item == bitem)
                        continue iteratorLoop;
                }
                BlockDispenser.dispenseBehaviorRegistry.putObject(item, new BehaviourShovel());

                if (print)
                    modlog.info("Dispenser Behaviour (Shovel) added to: " + item.getItemStackDisplayName(new ItemStack(item)));

            }
            else if (item instanceof ItemSword)
            {
                for (Item bitem : bitemlist)
                {
                    if (item == bitem)
                        continue iteratorLoop;
                }
                BlockDispenser.dispenseBehaviorRegistry.putObject(item, new BehaviourSword());

                if (print)
                    modlog.info("Dispenser Behaviour (Sword)  added to: " + item.getItemStackDisplayName(new ItemStack(item)));

            }
        }
        
        for (String witem : CampfireBackportConfig.dispenserBehavioursWhitelist)
        {
            String[] segment = witem.split("/");
            Item item = itemreg.getObject(segment[0]);

            if (item != null)
            {
                if (segment[1].equals("shovel"))
                {
                    BlockDispenser.dispenseBehaviorRegistry.putObject(item, new BehaviourShovel());
                    if (print)
                        modlog.info("Dispenser Behaviour (Shovel) added to: " + item.getItemStackDisplayName(new ItemStack(item)));
                }
                else //if (segment[1].equals("sword"))
                {
                    BlockDispenser.dispenseBehaviorRegistry.putObject(item, new BehaviourSword());
                    if (print)
                        modlog.info("Dispenser Behaviour (Sword)  added to: " + item.getItemStackDisplayName(new ItemStack(item)));
                }
                continue;
            }
            
            Block block = blockreg.getObject(segment[0]);

            if (block != Blocks.air)
            {
                if (segment[1].equals("shovel"))
                {
                    BlockDispenser.dispenseBehaviorRegistry.putObject(block, new BehaviourShovel());
                    if (print)
                        modlog.info("Dispenser Behaviour (Shovel) added to: " + item.getItemStackDisplayName(new ItemStack(block)));
                }
                else //if (segment[1].equals("sword"))
                {
                    BlockDispenser.dispenseBehaviorRegistry.putObject(block, new BehaviourSword());
                    if (print)
                        modlog.info("Dispenser Behaviour (Sword)  added to: " + item.getItemStackDisplayName(new ItemStack(block)));
                }
            }
        }
        
    }

    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire)
    {
        ;
    }
    
}