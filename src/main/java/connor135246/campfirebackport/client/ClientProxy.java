package connor135246.campfirebackport.client;

import java.util.Random;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTSecondaryColor;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.client.rendering.RenderBlockCampfire;
import connor135246.campfirebackport.client.rendering.RenderItemBlockCampfire;
import connor135246.campfirebackport.client.rendering.RenderTileEntityCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
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

    /** ensures we can use glSecondaryColor. based on how OpenGlHelper works for glBlendFuncSeparate. */
    public static boolean canUseGL14SecondaryColor, canUseEXTSecondaryColor;

    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);

        ContextCapabilities contextcapabilities = GLContext.getCapabilities();
        canUseGL14SecondaryColor = contextcapabilities.OpenGL14;
        canUseEXTSecondaryColor = contextcapabilities.GL_EXT_secondary_color;
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
        super.init(event);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, RenderTileEntityCampfire.INSTANCE);

        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.campfire), RenderItemBlockCampfire.INSTANCE);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.soul_campfire), RenderItemBlockCampfire.INSTANCE);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.foxfire_campfire), RenderItemBlockCampfire.INSTANCE);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CampfireBackportBlocks.shadow_campfire), RenderItemBlockCampfire.INSTANCE);

        BlockCampfire.renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(RenderBlockCampfire.INSTANCE);
    }

    @Override
    public void generateBigSmokeParticles(World world, int x, int y, int z, int typeIndex, boolean signalFire)
    {
        if (RAND.nextFloat() < 0.11F)
        {
            float[] colours = new float[0];

            if (CampfireBackportConfig.colourfulSmoke.matches(typeIndex))
            {
                Block blockBelow = world.getBlock(x, y - 1, z);

                if (blockBelow.getMaterial() != Material.air && world.isBlockIndirectlyGettingPowered(x, y, z))
                {
                    int intColour = blockBelow.getMapColor(world.getBlockMetadata(x, y - 1, z)).func_151643_b(2);
                    colours = new float[] { ((intColour >> 16) & 0xFF) / 255F, ((intColour >> 8) & 0xFF) / 255F, (intColour & 0xFF) / 255F };
                }
            }

            for (int i = 0; i < RAND.nextInt(2) + 2; ++i)
                Minecraft.getMinecraft().effectRenderer.addEffect(new EntityBigSmokeFX(world, x, y, z, signalFire, colours));
        }
    }

    @Override
    public void generateSmokeOverItems(World world, int x, int y, int z, int meta, ItemStack[] items)
    {
        int[] iro = RenderTileEntityCampfire.getRenderSlotMappingFromMeta(meta);
        for (int slot = 0; slot < items.length; ++slot)
        {
            if (items[slot] != null)
            {
                if (RAND.nextFloat() < 0.2F)
                {
                    double[] position = RenderTileEntityCampfire.getRenderPositionFromRenderSlot(iro[slot], true);
                    world.spawnParticle("smoke", x + position[0], y + position[1], z + position[2], 0.0, 0.0005, 0.0);
                }
            }
        }
    }

    //

    public static void enableGLSecondaryColor(float multiplier, float red, float green, float blue)
    {
        if (canUseGL14SecondaryColor)
        {
            GL11.glEnable(GL14.GL_COLOR_SUM);
            GL14.glSecondaryColor3f(multiplier * red, multiplier * green, multiplier * blue);
        }
        else if (canUseEXTSecondaryColor)
        {
            GL11.glEnable(EXTSecondaryColor.GL_COLOR_SUM_EXT);
            EXTSecondaryColor.glSecondaryColor3fEXT(multiplier * red, multiplier * green, multiplier * blue);
        }
    }

    public static void disableGLSecondaryColor()
    {
        if (canUseGL14SecondaryColor)
        {
            GL11.glDisable(GL14.GL_COLOR_SUM);
        }
        else if (canUseEXTSecondaryColor)
        {
            GL11.glDisable(EXTSecondaryColor.GL_COLOR_SUM_EXT);
        }
    }

}