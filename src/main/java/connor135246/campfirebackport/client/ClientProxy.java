package connor135246.campfirebackport.client;

import java.util.Random;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.client.rendering.RenderItemBlockCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy
{

    protected static final Random RAND = new Random();

    @Override
    public void init(FMLInitializationEvent event)
    {
        super.init(event);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, RenderCampfire.INSTANCE);

        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.campfire), RenderItemBlockCampfire.INSTANCE);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.soul_campfire), RenderItemBlockCampfire.INSTANCE);
    }

    @Override
    public void generateBigSmokeParticles(World world, int x, int y, int z, String type, boolean signalFire)
    {
        if (RAND.nextFloat() < 0.11F)
        {
            if (CampfireBackportConfig.colourfulSmoke.matches(type))
            {
                float[] colours = new float[0];
                Block blockBelow = world.getBlock(x, y - 1, z);

                if (blockBelow.getMaterial() != Material.air && world.isBlockIndirectlyGettingPowered(x, y, z))
                {
                    int intColour = blockBelow.getMapColor(world.getBlockMetadata(x, y - 1, z)).func_151643_b(2);
                    colours = new float[] { ((intColour >> 16) & 0xFF) / 255F, ((intColour >> 8) & 0xFF) / 255F, (intColour & 0xFF) / 255F };
                }

                for (int i = 0; i < RAND.nextInt(2) + 2; ++i)
                    Minecraft.getMinecraft().effectRenderer.addEffect(new EntityBigSmokeFX(world, x, y, z, signalFire, colours));
            }
        }
    }

    @Override
    public void generateSmokeOverItems(World world, int x, int y, int z, int meta, ItemStack[] items)
    {
        int[] iro = RenderCampfire.getRenderSlotMappingFromMeta(meta);
        for (int slot = 0; slot < items.length; ++slot)
        {
            if (items[slot] != null)
            {
                if (RAND.nextFloat() < 0.2F)
                {
                    double[] position = RenderCampfire.getRenderPositionFromRenderSlot(iro[slot], true);
                    world.spawnParticle("smoke", x + position[0], y + position[1], z + position[2], 0.0, 0.0005, 0.0);
                }
            }
        }
    }

}