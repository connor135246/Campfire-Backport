package connor135246.campfirebackport.client;

import java.util.Random;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy
{

    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
        super.init(event);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, new RenderCampfire());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event)
    {
        super.postInit(event);
    }

    @Override
    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire)
    {
        EntityBigSmokeFX smokey = new EntityBigSmokeFX(world, x, y, z, signalFire);

        Minecraft.getMinecraft().effectRenderer.addEffect(smokey);
    }

}