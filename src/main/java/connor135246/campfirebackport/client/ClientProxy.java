package connor135246.campfirebackport.client;

import java.awt.Color;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.client.rendering.RenderItemBlockCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy
{

    @Override
    public void init(FMLInitializationEvent event)
    {
        super.init(event);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, RenderCampfire.INSTANCE);

        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.campfire), RenderItemBlockCampfire.INSTANCE);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.soul_campfire), RenderItemBlockCampfire.INSTANCE);
    }

    @Override
    public void generateBigSmokeParticles(World world, int x, int y, int z, boolean signalFire, Block colourer, int meta)
    {
        float[] colours = new float[0];

        if (colourer != Blocks.air)
            colours = Color.decode(((Integer) colourer.getMapColor(meta).func_151643_b(2)).toString()).getRGBColorComponents(null);

        Minecraft.getMinecraft().effectRenderer.addEffect(new EntityBigSmokeFX(world, x, y, z, signalFire, colours));
    }

}