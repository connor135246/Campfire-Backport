package connor135246.campfirebackport.common;

import org.apache.logging.log4j.Logger;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.recipes.CampfireBackportRecipes;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import connor135246.campfirebackport.config.ConfigNetworkManager.SendConfigMessage;
import connor135246.campfirebackport.config.ConfigNetworkManager.SendMixinConfigMessage;
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
import net.minecraft.nbt.NBTTagCompound;
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

        ConfigNetworkManager.getMixinsHere();
        simpleNetwork = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID);
        simpleNetwork.registerMessage(SendConfigMessage.Handler.class, SendConfigMessage.class, 1, Side.CLIENT);
        simpleNetwork.registerMessage(SendMixinConfigMessage.Handler.class, SendMixinConfigMessage.class, 2, Side.CLIENT);

        CampfireBackportCompat.preInit();

        CampfireBackportConfig.prepareConfig(event);
        CampfireBackportConfig.doConfig(11, true, true);

        CampfireBackportBlocks.preInit();

        GameRegistry.registerTileEntity(TileEntityCampfire.class, Reference.MODID + ":" + "campfire");
    }

    public void init(FMLInitializationEvent event)
    {
        FMLInterModComms.sendMessage("Waila", "register", Reference.MOD_PACKAGE + ".client.compat.waila.CampfireBackportWailaDataProvider.register");

        sendNEIGTNHHandler(Reference.NEI_RECIPE_ID, "campfirebackport:campfire", 65, 166, 5);
        sendNEIGTNHHandler(Reference.NEI_STATECHANGER_ID, "campfirebackport:campfire_base", 65, 166, 5);
        sendNEIGTNHHandler(Reference.NEI_SIGNALBLOCKS_ID, "minecraft:hay_block", 130, 166, 5);

        for (String neiID : new String[] { Reference.NEI_RECIPE_ID, Reference.NEI_STATECHANGER_ID, Reference.NEI_SIGNALBLOCKS_ID })
        {
            sendNEIGTNHCatalyst(neiID, "campfirebackport:campfire", 0);
            sendNEIGTNHCatalyst(neiID, "campfirebackport:campfire_base", -1);
            sendNEIGTNHCatalyst(neiID, "campfirebackport:soul_campfire", 0);
            sendNEIGTNHCatalyst(neiID, "campfirebackport:soul_campfire_base", -1);

            if (CampfireBackportCompat.isNetherliciousLoaded)
            {
                sendNEIGTNHCatalyst(neiID, "campfirebackport:foxfire_campfire", 0);
                sendNEIGTNHCatalyst(neiID, "campfirebackport:foxfire_campfire_base", -1);
                sendNEIGTNHCatalyst(neiID, "campfirebackport:shadow_campfire", 0);
                sendNEIGTNHCatalyst(neiID, "campfirebackport:shadow_campfire_base", -1);
            }
        }
    }

    public void postInit(FMLPostInitializationEvent event)
    {
        CampfireBackportCompat.postInit();

        CampfireBackportRecipes.postInit();
    }

    public void loadComplete(FMLLoadCompleteEvent event)
    {
        CampfireBackportConfig.doConfig(4, true, false);
    }

    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandCampfireBackport());
    }

    //

    /**
     * Makes campfire smoke particles.
     */
    public void generateBigSmokeParticles(World world, int x, int y, int z, int typeIndex, boolean signalFire)
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

    // NEI-GTNH compatibility

    public void sendNEIGTNHHandler(String id, String item, int height, int width, int maxRecipesPerPage)
    {
        NBTTagCompound handler = new NBTTagCompound();
        handler.setString("handler", id);
        handler.setString("handlerID", id);
        handler.setString("modId", Reference.MODID);
        handler.setString("modName", Reference.NAME);
        handler.setBoolean("modRequired", true);
        handler.setString("itemName", item);
        handler.setInteger("handlerHeight", height);
        handler.setInteger("handlerWidth", width);
        handler.setInteger("maxRecipesPerPage", maxRecipesPerPage);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", handler);
    }

    public void sendNEIGTNHCatalyst(String id, String item, int priority)
    {
        NBTTagCompound catalyst = new NBTTagCompound();
        catalyst.setString("handler", id);
        catalyst.setString("handlerID", id);
        catalyst.setString("catalystHandlerID", id);
        catalyst.setString("modId", Reference.MODID);
        catalyst.setString("modName", Reference.NAME);
        catalyst.setBoolean("modRequired", true);
        catalyst.setString("itemName", item);
        catalyst.setInteger("priority", priority);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerCatalystInfo", catalyst);
    }

}
