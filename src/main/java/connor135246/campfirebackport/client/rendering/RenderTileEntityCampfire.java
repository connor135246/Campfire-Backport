package connor135246.campfirebackport.client.rendering;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;

/**
 * Renders the campfire tile's items.
 */
public class RenderTileEntityCampfire extends TileEntitySpecialRenderer
{

    public static final RenderTileEntityCampfire INSTANCE = new RenderTileEntityCampfire();

    // https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/1571543-forge-rendering-an-item-on-your-block
    // thanks

    protected EntityItem invRender[] = new EntityItem[4];

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float scale)
    {

        if (tile instanceof TileEntityCampfire && tile.hasWorldObj())
        {
            TileEntityCampfire ctile = (TileEntityCampfire) tile;

            int[] iro = getRenderSlotMappingFromMeta(ctile.getBlockMetadata());

            for (int slot = 0; slot < ctile.getSizeInventory(); ++slot)
            {
                ItemStack stack = ctile.getStackInSlot(slot);

                if (stack != null)
                {
                    int renderSlot = iro[slot];

                    if (invRender[slot] == null)
                    {
                        invRender[slot] = new EntityItem(ctile.getWorldObj());
                        invRender[slot].hoverStart = 0.0F;
                    }
                    else
                        invRender[slot].setWorld(ctile.getWorldObj());

                    invRender[slot].setEntityItemStack(stack);

                    boolean rendersAsBlock = stack.getItemSpriteNumber() == 0 && stack.getItem() instanceof ItemBlock
                            && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(stack.getItem()).getRenderType());

                    GL11.glPushMatrix();
                    GL11.glColor4f(1, 1, 1, 1);
                    RenderItem.renderInFrame = true;

                    GL11.glDisable(GL11.GL_BLEND);

                    if (!rendersAsBlock)
                        GL11.glDisable(GL11.GL_LIGHTING);

                    double[] position = getRenderPositionFromRenderSlot(renderSlot, false);
                    GL11.glTranslated(x + position[0], y + position[1], z + position[2]);

                    if (rendersAsBlock)
                    {
                        GL11.glRotatef(renderSlot * -90, 0, 1, 0);
                        GL11.glTranslated(-0.125, -0.01625, 0.0);
                    }
                    else
                    {
                        GL11.glRotatef(180, 0, 1, 1);
                        GL11.glRotatef(renderSlot * -90, 0, 0, 1);
                        GL11.glRotatef(270, 0, 0, 1);
                    }

                    GL11.glScalef(0.625F, 0.625F, 0.625F);
                    RenderManager.instance.renderEntityWithPosYaw(invRender[slot], 0.0, 0.0, 0.0, 0.0F, 0.0F);

                    if (!rendersAsBlock)
                        GL11.glEnable(GL11.GL_LIGHTING);

                    RenderItem.renderInFrame = false;
                    GL11.glPopMatrix();
                }
            }
        }
    }

    // Rendering Positions

    private static final double BASE_X_OFFSET = 0.9375;
    private static final double BASE_Y_OFFSET = 0.45;
    private static final double BASE_Z_OFFSET = 0.9375;
    private static final double ACROSS = 0.875;
    private static final double EDGE = 0.125;
    private static final double SMOKE_OFFSET = 0.15625;
    private static final double SMOKE_Y_OFFSET = 0.06;
    private static final double[][] RENDER_POSITION_ITEM = new double[][] {
            { BASE_X_OFFSET, BASE_Y_OFFSET, BASE_Z_OFFSET + EDGE - ACROSS },
            { BASE_X_OFFSET - EDGE, BASE_Y_OFFSET, BASE_Z_OFFSET },
            { BASE_X_OFFSET - ACROSS, BASE_Y_OFFSET, BASE_Z_OFFSET - EDGE },
            { BASE_X_OFFSET + EDGE - ACROSS, BASE_Y_OFFSET, BASE_Z_OFFSET - ACROSS } };
    private static final double[][] RENDER_POSITION_SMOKE = new double[][] {
            { BASE_X_OFFSET - SMOKE_OFFSET, BASE_Y_OFFSET + SMOKE_Y_OFFSET, BASE_Z_OFFSET + EDGE - ACROSS },
            { BASE_X_OFFSET - EDGE, BASE_Y_OFFSET + SMOKE_Y_OFFSET, BASE_Z_OFFSET - SMOKE_OFFSET },
            { BASE_X_OFFSET - ACROSS + SMOKE_OFFSET, BASE_Y_OFFSET + SMOKE_Y_OFFSET, BASE_Z_OFFSET - EDGE },
            { BASE_X_OFFSET + EDGE - ACROSS, BASE_Y_OFFSET + SMOKE_Y_OFFSET, BASE_Z_OFFSET - ACROSS + SMOKE_OFFSET } };
    private static final int[][] RENDER_SLOT_MAPPING = new int[][] {
            { 3, 0, 1, 2 },
            { 1, 2, 3, 0 },
            { 2, 3, 0, 1 },
            { 0, 1, 2, 3 } };

    /**
     * Returns the mapping of inventory slots to render slots based on block metadata. This is so that inventory slots are always rendered counterclockwise starting with the front
     * right, no matter which direction the block is facing. See {@link #getRenderPositionFromRenderSlot} for an explanation of the render slot. Block metadata is just the
     * direction the player was facing when placing the block. South = 2, North = 3, East = 4, West = 5.
     * 
     * @param meta
     *            - the metadata of the block
     * @return an int[] where the index corresponds to the inventory slot and the value corresponds to the render slot
     */
    public static int[] getRenderSlotMappingFromMeta(int meta)
    {
        switch (meta)
        {
        default:
            return RENDER_SLOT_MAPPING[0];
        case 5:
            return RENDER_SLOT_MAPPING[3];
        case 3:
            return RENDER_SLOT_MAPPING[1];
        case 4:
            return RENDER_SLOT_MAPPING[2];
        }
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
        return (smoke ? RENDER_POSITION_SMOKE : RENDER_POSITION_ITEM)[MathHelper.clamp_int(renderslot, 0, 3)];
    }

}
