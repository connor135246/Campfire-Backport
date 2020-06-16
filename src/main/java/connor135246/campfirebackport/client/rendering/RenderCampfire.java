package connor135246.campfirebackport.client.rendering;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.client.models.ModelCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderCampfire extends TileEntitySpecialRenderer
{
    private ModelCampfire model;
    private EntityItem entItem[] = new EntityItem[4];
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation(Reference.MODID + ":" + "textures/blocks/campfire_base_tile.png");

    private static final double BASE_X_OFFSET = 0.9375;
    private static final double BASE_Y_OFFSET = 0.44;
    private static final double BASE_Z_OFFSET = 0.9375;
    private static final double ACROSS = 0.875;
    private static final double EDGE = 0.125;
    private static final double SMOKE_OFFSET = 0.15625;
    private static final double[][] RENDER_POSITION_ITEM = new double[][] { { BASE_X_OFFSET, BASE_Y_OFFSET, BASE_Z_OFFSET + EDGE - ACROSS },
            { BASE_X_OFFSET - EDGE, BASE_Y_OFFSET, BASE_Z_OFFSET },
            { BASE_X_OFFSET - ACROSS, BASE_Y_OFFSET, BASE_Z_OFFSET - EDGE },
            { BASE_X_OFFSET + EDGE - ACROSS, BASE_Y_OFFSET, BASE_Z_OFFSET - ACROSS } };
    private static final double[][] RENDER_POSITION_SMOKE = new double[][] { { BASE_X_OFFSET - SMOKE_OFFSET, BASE_Y_OFFSET, BASE_Z_OFFSET + EDGE - ACROSS },
            { BASE_X_OFFSET - EDGE, BASE_Y_OFFSET, BASE_Z_OFFSET - SMOKE_OFFSET },
            { BASE_X_OFFSET - ACROSS + SMOKE_OFFSET, BASE_Y_OFFSET, BASE_Z_OFFSET - EDGE },
            { BASE_X_OFFSET + EDGE - ACROSS, BASE_Y_OFFSET, BASE_Z_OFFSET - ACROSS + SMOKE_OFFSET } };
    private static final int[][] RENDER_SLOT_MAPPING = new int[][] { { 3, 0, 1, 2 }, { 1, 2, 3, 0 }, { 2, 3, 0, 1 }, { 0, 1, 2, 3 } };

    // https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/1571543-forge-rendering-an-item-on-your-block
    // thanks

    public RenderCampfire()
    {
        model = new ModelCampfire();
    }

    @Override
    public void renderTileEntityAt(TileEntity tileent, double x, double y, double z, float scale)
    {
        TileEntityCampfire tilecamp = (TileEntityCampfire) tileent;

        boolean lit = true;
        String type = EnumCampfireType.REGULAR;
        int dir = 2;
        int animTimer = 0;
        boolean renderItems = false;

        if (tilecamp.hasWorldObj())
        {
            lit = tilecamp.getThisLit();
            type = tilecamp.getThisType();
            dir = tilecamp.getThisMeta();
            animTimer = tilecamp.getAnimTimer();// < 0 ? 0 : tilecamp.getAnimTimer();
            renderItems = true;
        }
        // else
        // tilecamp.incrementAnimTimer();

        float angle;
        switch (dir)
        {
        default:
            angle = 0;
            break;
        case 5:
            angle = 90F;
            break;
        case 3:
            angle = 180F;
            break;
        case 4:
            angle = 270F;
            break;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 1.5, z + 0.5);
        GL11.glRotatef(180, 0, 0, 1);

        GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);

        if (lit)
            bindTexture(getTextureLit((animTimer % 31) / 2, type));
        else
            bindTexture(TEXTURE_BASE);

        model.render((Entity) null, 0, -0.1F, 0, 0, 0, 0.0625F);

        GL11.glPopMatrix();

        if (renderItems)
        {
            int[] iro = getRenderSlotMappingFromMeta(dir);

            for (int invslot = 0; invslot < tilecamp.getSizeInventory(); ++invslot)
            {
                ItemStack stack = tilecamp.getStackInSlot(invslot);

                if (stack != null)
                {
                    int renderSlot = iro[invslot];

                    if (entItem[invslot] == null || entItem[invslot].getEntityItem().toString() != stack.toString())
                        entItem[invslot] = new EntityItem(tilecamp.getWorldObj(), x, y, z, stack);

                    GL11.glPushMatrix();
                    entItem[invslot].hoverStart = 0.0F;
                    RenderItem.renderInFrame = true;
                    GL11.glDisable(GL11.GL_LIGHTING);

                    double[] position = getRenderPositionFromRenderSlot(renderSlot, false);
                    GL11.glTranslated(position[0] + x, position[1] + y, position[2] + z);

                    GL11.glRotatef(180, 0, 1, 1);
                    GL11.glRotatef(renderSlot * -90, 0, 0, 1);
                    GL11.glRotatef(270, 0, 0, 1);

                    GL11.glScalef(0.625F, 0.625F, 0.625F);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 200f, 200f);
                    RenderManager.instance.renderEntityWithPosYaw(entItem[invslot], 0.0, 0.0, 0.0, 0.0F, 0.0F);

                    GL11.glEnable(GL11.GL_LIGHTING);
                    RenderItem.renderInFrame = false;
                    GL11.glPopMatrix();
                }
            }
        }

    }

    public ResourceLocation getTextureLit(int index, String type)
    {
        return type.equals(EnumCampfireType.SOUL) ? new ResourceLocation(Reference.MODID + ":" + "textures/blocks/soul_campfire_tile" + index + ".png")
                : new ResourceLocation(Reference.MODID + ":" + "textures/blocks/campfire_tile" + index + ".png");
    }

    /**
     * Returns the mapping of inventory slots to render slots based on block metadata. This is so that inventory slots are always rendered counterclockwise starting with the front
     * right, no matter which direction the block is facing. See {@link #getRenderPositionFromRenderSlot(int, boolean) getRenderPositionFromRenderSlot} for an explanation of the
     * render slot. Block metadata is just the direction the player was facing when placing the block. South = 2, North = 3, East = 4, West = 5.
     * 
     * @param meta
     *            - the metadata of the block
     * @return an int[] where the index corresponds to the inventory slot and the value corresponds to the render slot
     */
    public static int[] getRenderSlotMappingFromMeta(int meta)
    {
        return RENDER_SLOT_MAPPING[Math.max(0, meta - 2)];
    }

    /**
     * Gets the offset of a render slot. Render slots are a position just above the campfire block on each corner where items and smoke are rendered. Render slot 0 is the southeast
     * corner. Render slot 1 is the southwest corner. Render slot 2 is the northwest corner. Render slot 3 is the northeast corner.
     * 
     * @param renderslot
     *            - the slot in question
     * @param smoke
     *            - true if rendering smoke, false if rendering items
     * @return a double[] where the first element is the x offset, the second element is the y offset, and the third element is the z offset
     */
    public static double[] getRenderPositionFromRenderSlot(int renderslot, boolean smoke)
    {
        return smoke ? RENDER_POSITION_SMOKE[renderslot] : RENDER_POSITION_ITEM[renderslot];
    }

}
