package connor135246.campfirebackport.client.rendering;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.IItemRenderer;

public class RenderItemBlockCampfire implements IItemRenderer
{

    public static final RenderItemBlockCampfire INSTANCE = new RenderItemBlockCampfire();

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type)
    {
        return !CampfireBackportConfig.renderItem3D && CampfireBackportConfig.burnOutAsItem.matches(((ItemBlockCampfire) stack.getItem()).getType())
                && stack.hasTagCompound() && stack.getTagCompound().hasKey(TileEntityCampfire.KEY_BlockEntityTag);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper)
    {
        return helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data)
    {
        NBTTagCompound tilenbt = stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag);
        float size = 1.0F;

        if (tilenbt.hasKey(TileEntityCampfire.KEY_Life) && tilenbt.hasKey(TileEntityCampfire.KEY_StartingLife)
                && tilenbt.hasKey(TileEntityCampfire.KEY_PreviousTimestamp))
        {
            float life = tilenbt.getInteger(TileEntityCampfire.KEY_Life);
            float timePassed = Minecraft.getMinecraft().thePlayer.worldObj.getTotalWorldTime() - tilenbt.getLong(TileEntityCampfire.KEY_PreviousTimestamp);
            float starting = tilenbt.getInteger(TileEntityCampfire.KEY_StartingLife);

            size = MathHelper.clamp_float(((life - timePassed) / starting), 0.0F, 1.0F) * 0.9F + 0.1F;
        }

        Tessellator tess = Tessellator.instance;
        IIcon baseIcon = stack.getItem().getIconFromDamageForRenderPass(stack.getItemDamage(), 0);
        IIcon overlayIcon = stack.getItem().getIconFromDamageForRenderPass(stack.getItemDamage(), 1);

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        if (type == ItemRenderType.ENTITY)
        {
            GL11.glTranslatef(-0.5F, -0.25F, 0.0F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glTranslatef(1.0F, 0.0F, 0.0F);
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
        }

        if (type != ItemRenderType.INVENTORY)
        {
            ItemRenderer.renderItemIn2D(tess, baseIcon.getMaxU(), baseIcon.getMinV(), baseIcon.getMinU(), baseIcon.getMaxV(),
                    baseIcon.getIconWidth(), baseIcon.getIconHeight(), 0.0625F);

            GL11.glTranslatef((1F - size) / 2F, (0.6F - size + 0.5F * size) / 2F, 0.001F);
            GL11.glScalef(size, size, 1.02F);

            ItemRenderer.renderItemIn2D(tess, overlayIcon.getMaxU(), overlayIcon.getMinV(), overlayIcon.getMinU(), overlayIcon.getMaxV(),
                    overlayIcon.getIconWidth(), overlayIcon.getIconHeight(), 0.0625F);
        }
        else
        {
            GL11.glScalef(16.0F, 16.0F, 1.0F);

            tess.startDrawingQuads();
            tess.addVertexWithUV(0.0D, 0.0D, 0.0D, (double) baseIcon.getMinU(), (double) baseIcon.getMinV());
            tess.addVertexWithUV(0.0D, 1.0D, 0.0D, (double) baseIcon.getMinU(), (double) baseIcon.getMaxV());
            tess.addVertexWithUV(1.0D, 1.0D, 0.0D, (double) baseIcon.getMaxU(), (double) baseIcon.getMaxV());
            tess.addVertexWithUV(1.0D, 0.0D, 0.0D, (double) baseIcon.getMaxU(), (double) baseIcon.getMinV());
            tess.draw();

            GL11.glTranslatef((1F - size) / 2F, (1.4F - size - 0.4F * size) / 2F, 0.01F);
            GL11.glScalef(size, size, 1.0F);

            tess.startDrawingQuads();
            tess.addVertexWithUV(0.0D, 0.0D, 0.0D, (double) overlayIcon.getMinU(), (double) overlayIcon.getMinV());
            tess.addVertexWithUV(0.0D, 1.0D, 0.0D, (double) overlayIcon.getMinU(), (double) overlayIcon.getMaxV());
            tess.addVertexWithUV(1.0D, 1.0D, 0.0D, (double) overlayIcon.getMaxU(), (double) overlayIcon.getMaxV());
            tess.addVertexWithUV(1.0D, 0.0D, 0.0D, (double) overlayIcon.getMaxU(), (double) overlayIcon.getMinV());
            tess.draw();
        }

        GL11.glPopMatrix();
        GL11.glPushMatrix();
    }
}
