package connor135246.campfirebackport.client.rendering;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Renders the campfire block. Doesn't render its items.
 */
public class RenderBlockCampfire implements ISimpleBlockRenderingHandler
{

    public static final RenderBlockCampfire INSTANCE = new RenderBlockCampfire();

    @Override
    public boolean renderWorldBlock(IBlockAccess access, int x, int y, int z, Block block, int modelId, RenderBlocks renderer)
    {
        renderCampfire(access, x, y, z, block, access.getBlockMetadata(x, y, z), renderer, false, false, false);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer)
    {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        renderCampfire(null, block, 5, renderer, true, false, true);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId)
    {
        return CampfireBackportConfig.renderItem3D;
    }

    @Override
    public int getRenderId()
    {
        return BlockCampfire.renderId;
    }

    public static void renderCampfire(@Nullable IBlockAccess access, final Block block, final int meta, final RenderBlocks renderer, final boolean doDraw,
            final boolean mixedFire, final boolean flatSideColor)
    {
        renderCampfire(access, 0, 0, 0, block, meta, renderer, doDraw, mixedFire, flatSideColor);
    }

    public static void renderCampfire(@Nullable IBlockAccess access, final int x, final int y, final int z, final Block block, final int meta,
            final RenderBlocks renderer, final boolean doDraw, final boolean mixedFire, final boolean flatSideColor)
    {
        Tessellator tess = Tessellator.instance;

        final boolean enableAO = renderer.enableAO;
        renderer.enableAO = false;

        // 16777215 is the default return of Block.colorMultiplier
        int color = access == null ? 16777215 : block.colorMultiplier(access, x, y, z);
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        if (EntityRenderer.anaglyphEnable)
        {
            float f3 = (r * 30.0F + g * 59.0F + b * 11.0F) / 100.0F;
            float f4 = (r * 30.0F + g * 70.0F) / 100.0F;
            float f5 = (r * 30.0F + b * 70.0F) / 100.0F;
            r = f3;
            g = f4;
            b = f5;
        }

        if (!doDraw)
            // 15728880 is 15 << 20 | 15 << 4, aka max brightness (see World.getLightBrightnessForSkyBlocks)
            tess.setBrightness(access == null ? 15728880 : block.getMixedBrightnessForBlock(access, x, y, z));

        final boolean isLit = CampfireBackportBlocks.isLitCampfire(block);
        final boolean northSouth = !(meta == 4 || meta == 5);
        final double sideLogY = 0.1875, logY1 = 0.25, logY2 = 0.4375, firepitO = 0.3125, logO1 = 0.0625, logO2 = 0.6875;
        final float colorYNeg, colorYPos, colorZ, colorX;
        if (flatSideColor)
            colorYNeg = colorYPos = colorZ = colorX = 1.0F;
        else
        {
            colorYNeg = 0.5F;
            colorYPos = 1.0F;
            colorZ = 0.8F;
            colorX = 0.6F;
        }

        if (doDraw)
        {
            tess.startDrawingQuads();
            tess.setNormal(0.0F, -1.0F, 0.0F);
        }
        tess.setColorOpaque_F(r * colorYNeg, g * colorYNeg, b * colorYNeg);

        // upper log bottoms
        renderLogTopOrBot(northSouth ? x : (x + logO1), y + sideLogY, northSouth ? (z + logO1) : z, block, renderer, 0,
                meta == 3 ? 0 : (meta == 4 ? 2 : (meta == 5 ? 1 : 3)), isLit);
        renderLogTopOrBot(northSouth ? x : (x + logO2), y + sideLogY, northSouth ? (z + logO2) : z, block, renderer, 0,
                meta == 3 ? 0 : (meta == 4 ? 2 : (meta == 5 ? 1 : 3)), isLit);

        // bottom sides
        if (renderer.renderAllFaces || access == null || block.shouldSideBeRendered(access, x, y - 1, z, 0))
        {
            final int rotateFromMeta = meta == 3 ? 2 : (meta == 4 ? 3 : (meta == 5 ? 0 : 1));

            renderLogTopOrBot(northSouth ? (x + logO1) : x, y, northSouth ? z : (z + logO1), block, renderer, 0, rotateFromMeta, false);
            renderLogTopOrBot(northSouth ? (x + logO2) : x, y, northSouth ? z : (z + logO2), block, renderer, 0, rotateFromMeta, false);
            renderFirepitTopOrBot(northSouth ? (x + firepitO) : x, y, northSouth ? z : (z + firepitO), block, renderer, 0, rotateFromMeta, isLit);
        }

        if (doDraw)
        {
            tess.draw();
            tess.startDrawingQuads();
            tess.setNormal(0.0F, 1.0F, 0.0F);
        }
        tess.setColorOpaque_F(r * colorYPos, g * colorYPos, b * colorYPos);

        // north-south log tops
        renderLogTopOrBot(x + logO1, northSouth ? (y - 1 + logY1) : (y - 1 + logY2), z, block, renderer, 1, meta == 3 || meta == 5 ? 2 : 1, false);
        renderLogTopOrBot(x + logO2, northSouth ? (y - 1 + logY1) : (y - 1 + logY2), z, block, renderer, 1, meta == 3 || meta == 5 ? 2 : 1, false);

        // east-west log tops
        renderLogTopOrBot(x, northSouth ? (y - 1 + logY2) : (y - 1 + logY1), z + logO1, block, renderer, 1, meta == 3 || meta == 5 ? 0 : 3, false);
        renderLogTopOrBot(x, northSouth ? (y - 1 + logY2) : (y - 1 + logY1), z + logO2, block, renderer, 1, meta == 3 || meta == 5 ? 0 : 3, false);

        // firepit top
        renderFirepitTopOrBot(northSouth ? (x + firepitO) : x, y - 0.9375, northSouth ? z : (z + firepitO), block, renderer, 1,
                meta == 3 ? 2 : (meta == 4 ? 0 : (meta == 5 ? 3 : 1)), isLit);

        if (doDraw)
        {
            tess.draw();
            tess.startDrawingQuads();
            tess.setNormal(0.0F, 0.0F, -1.0F);
        }
        tess.setColorOpaque_F(r * colorZ, g * colorZ, b * colorZ);

        // north log sides
        renderLogSide(x, northSouth ? (y + sideLogY) : y, z + logO1, block, renderer, 2, northSouth ? isLit : false, false);
        renderLogSide(x, northSouth ? (y + sideLogY) : y, z + logO2, block, renderer, 2, isLit, !northSouth);

        // north side faces
        if (renderer.renderAllFaces || access == null || block.shouldSideBeRendered(access, x, y, z - 1, 2))
        {
            renderLogEnd(x + logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 2);
            renderLogEnd(x + logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 2);
            if (northSouth)
                renderFirepitSide(x + firepitO, y, z, block, renderer, 2, meta < 3 || meta > 5);
        }

        if (doDraw)
        {
            tess.draw();
            tess.startDrawingQuads();
            tess.setNormal(0.0F, 0.0F, 1.0F);
        }
        tess.setColorOpaque_F(r * colorZ, g * colorZ, b * colorZ);

        // south side faces
        if (renderer.renderAllFaces || access == null || block.shouldSideBeRendered(access, x, y, z + 1, 3))
        {
            renderLogEnd(x + logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 3);
            renderLogEnd(x + logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 3);
            if (northSouth)
                renderFirepitSide(x + firepitO, y, z, block, renderer, 3, meta == 3);
        }

        // south log sides
        renderLogSide(x, northSouth ? (y + sideLogY) : y, z - logO1, block, renderer, 3, northSouth ? isLit : false, false);
        renderLogSide(x, northSouth ? (y + sideLogY) : y, z - logO2, block, renderer, 3, isLit, !northSouth);

        if (doDraw)
        {
            tess.draw();
            tess.startDrawingQuads();
            tess.setNormal(-1.0F, 0.0F, 0.0F);
        }
        tess.setColorOpaque_F(r * colorX, g * colorX, b * colorX);

        // west log sides
        renderLogSide(x + logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 4, northSouth ? false : isLit, false);
        renderLogSide(x + logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 4, isLit, northSouth);

        // west side faces
        if (renderer.renderAllFaces || access == null || block.shouldSideBeRendered(access, x - 1, y, z, 4))
        {
            renderLogEnd(x, northSouth ? (y + sideLogY) : y, z + logO2, block, renderer, 4);
            renderLogEnd(x, northSouth ? (y + sideLogY) : y, z + logO1, block, renderer, 4);
            if (!northSouth)
                renderFirepitSide(x, y, z + firepitO, block, renderer, 4, meta == 4);
        }

        if (doDraw)
        {
            tess.draw();
            tess.startDrawingQuads();
            tess.setNormal(1.0F, 0.0F, 0.0F);
        }
        tess.setColorOpaque_F(r * colorX, g * colorX, b * colorX);

        // east log sides
        renderLogSide(x - logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 5, northSouth ? false : isLit, false);
        renderLogSide(x - logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 5, isLit, northSouth);

        // east side faces
        if (renderer.renderAllFaces || access == null || block.shouldSideBeRendered(access, x + 1, y, z, 5))
        {
            renderLogEnd(x, northSouth ? (y + sideLogY) : y, z + logO1, block, renderer, 5);
            renderLogEnd(x, northSouth ? (y + sideLogY) : y, z + logO2, block, renderer, 5);
            if (!northSouth)
                renderFirepitSide(x, y, z + firepitO, block, renderer, 5, meta == 5);
        }
        
        if (doDraw)
        {
            tess.draw();
        }

        // fire
        if (isLit)
        {
            boolean lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);

            if (doDraw)
            {
                if (lighting)
                    GL11.glDisable(GL11.GL_LIGHTING);
                tess.startDrawingQuads();
            }

            renderFire(x, y, z, block, renderer, mixedFire);

            if (doDraw)
            {
                tess.draw();
                if (lighting)
                    GL11.glEnable(GL11.GL_LIGHTING);
            }
        }

        renderer.enableAO = enableAO;
    }

    // Campfire Texture Face Renderers
    // Renders the piece at the bottom-north-west corner of the face. Adjust the given coordinates to render at a particular part of the face.

    /**
     * @param side
     *            - should be 2 (north), 3 (south), 4 (west), or 5 (east)
     */
    public static void renderLogEnd(double x, double y, double z, Block block, RenderBlocks renderer, int side)
    {
        if (side == 2 || side == 3)
            renderer.setRenderBounds(0, 0.5F, 0, 0.25F, 0.75F, 1);
        else if (side == 4 || side == 5)
            renderer.setRenderBounds(0, 0.5F, 0, 1, 0.75F, 0.25F);

        y = y - renderer.renderMinY;

        renderFace(x, y, z, block, renderer, block.getIcon(0, 0), side);
    }

    /**
     * @param side
     *            - should be 2 (north), 3 (south), 4 (west), or 5 (east)
     * @param lower
     *            - lowers the texture by one pixel
     */
    public static void renderLogSide(double x, double y, double z, Block block, RenderBlocks renderer, int side, boolean lit, boolean lower)
    {
        float maxX = 1, maxZ = 1;

        if (side == 2)
            maxZ = 0.25F;
        else if (side == 4)
            maxX = 0.25F;

        float minY = 0.75F;
        if (lower)
            minY -= 0.0625F;
        float maxY = minY + 0.25F;

        renderer.setRenderBounds(0, minY, 0, maxX, maxY, maxZ);

        y = y - renderer.renderMinY;

        renderFace(x, y, z, block, renderer, block.getIcon(0, lit ? -2 : 0), side);
    }

    /**
     * @param side
     *            - should be 0 (down) or 1 (up)
     * @param rotate
     *            - 0 and 3 are opposite, 1 and 2 are opposite
     */
    public static void renderLogTopOrBot(double x, double y, double z, Block block, RenderBlocks renderer, int side, int rotate, boolean lit)
    {
        float minX = 0, maxX = 1, minZ = 0, maxZ = 1;

        if (side == 0)
        {
            renderer.flipTexture = true;
            renderer.uvRotateBottom = rotate;
        }
        else if (side == 1)
            renderer.uvRotateTop = rotate;

        if (rotate == 0)
        {
            minZ = 0;
            if (lit)
                minZ += 0.25F;
            maxZ = minZ + 0.25F;
        }
        else if ((rotate == 1 && side == 1) || (rotate == 2 && side == 0))
        {
            minX = 0.75F;
            if (lit)
                minX -= 0.25;
            maxX = minX + 0.25F;
        }
        else if ((rotate == 2 && side == 1) || (rotate == 1 && side == 0))
        {
            minX = 0;
            if (lit)
                minX += 0.25F;
            maxX = minX + 0.25F;
        }
        else if (rotate == 3)
        {
            minZ = 0.75F;
            if (lit)
                minZ -= 0.25F;
            maxZ = minZ + 0.25F;
        }

        renderer.setRenderBounds(minX, 0, minZ, maxX, 1, maxZ);

        x = x - renderer.renderMinX;
        z = z - renderer.renderMinZ;

        renderFace(x, y, z, block, renderer, block.getIcon(0, lit ? -2 : 0), side);

        renderer.flipTexture = false;
        renderer.uvRotateBottom = 0;
        renderer.uvRotateTop = 0;
    }

    /**
     * @param side
     *            - should be 2 (north), 3 (south), 4 (west), or 5 (east)
     * @param front
     *            - should be true if side is the front of the campfire, false if it's the back
     */
    public static void renderFirepitSide(double x, double y, double z, Block block, RenderBlocks renderer, int side, boolean front)
    {
        float minX = 0, maxX = 1, minZ = 0, maxZ = 1;

        if (front)
        {
            if (side == 2 || side == 3)
                maxX = 0.375F;
            else if (side == 4 || side == 5)
                maxZ = 0.375F;
        }
        else
        {
            if (side == 2 || side == 3)
                minX = 0.625F;
            else if (side == 4 || side == 5)
                minZ = 0.625F;
        }

        renderer.setRenderBounds(minX, 0, minZ, maxX, 0.0625F, maxZ);

        x = x - renderer.renderMinX;
        z = z - renderer.renderMinZ;

        renderFace(x, y, z, block, renderer, block.getIcon(0, 0), side);
    }

    /**
     * @param side
     *            - should be 0 (down) or 1 (up)
     * @param rotate
     *            - 0 and 3 are opposite, 1 and 2 are opposite
     */
    public static void renderFirepitTopOrBot(double x, double y, double z, Block block, RenderBlocks renderer, int side, int rotate, boolean lit)
    {
        float minX = 0, maxX = 1, minZ = 0, maxZ = 1;

        if (side == 0)
        {
            renderer.flipTexture = true;
            renderer.uvRotateBottom = rotate;
        }
        else if (side == 1)
            renderer.uvRotateTop = rotate;

        if (rotate == 0)
        {
            minZ = 0.5F;
            maxZ = minZ + 0.375F;
        }
        else if ((rotate == 1 && side == 1) || (rotate == 2 && side == 0))
        {
            minX = 0.125F;
            maxX = minX + 0.375F;
        }
        else if ((rotate == 2 && side == 1) || (rotate == 1 && side == 0))
        {
            minX = 0.5F;
            maxX = minX + 0.375F;
        }
        else if (rotate == 3)
        {
            minZ = 0.125F;
            maxZ = minZ + 0.375F;
        }

        renderer.setRenderBounds(minX, 0, minZ, maxX, 1, maxZ);

        x = x - renderer.renderMinX;
        z = z - renderer.renderMinZ;

        renderFace(x, y, z, block, renderer, block.getIcon(0, side == 1 && lit ? -2 : 0), side);

        renderer.flipTexture = false;
        renderer.uvRotateBottom = 0;
        renderer.uvRotateTop = 0;
    }

    /**
     * Draws the campfire fire. If mixedFire is true, the fire will be half regular and half soul.
     */
    public static void renderFire(double x, double y, double z, Block block, RenderBlocks renderer, boolean mixedFire)
    {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(15728880); // 15728880 is 15 << 20 | 15 << 4, aka max brightness (see World.getLightBrightnessForSkyBlocks)
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        if (mixedFire)
            drawCrossedSquaresTwoIcons(CampfireBackportBlocks.campfire.getIcon(0, -3), CampfireBackportBlocks.soul_campfire.getIcon(0, -3), x, y, z, 1.0F);
        else
            renderer.drawCrossedSquares(block.getIcon(0, -3), x, y, z, 1.0F);
    }

    /**
     * Like {@link RenderBlocks#drawCrossedSquares}, but there's two textures.
     */
    public static void drawCrossedSquaresTwoIcons(IIcon icon1, IIcon icon2, double x, double y, double z, float size)
    {
        Tessellator tess = Tessellator.instance;

        double scaledSize = 0.45D * (double) size;
        double minX = x + 0.5D - scaledSize;
        double midX = x + 0.5D;
        double maxX = x + 0.5D + scaledSize;
        double minZ = z + 0.5D - scaledSize;
        double midZ = z + 0.5D;
        double maxZ = z + 0.5D + scaledSize;

        double minU1 = (double) icon1.getMinU();
        double minV1 = (double) icon1.getMinV();
        double midU1 = (double) (icon1.getMaxU() + icon1.getMinU()) / 2;
        double maxU1 = (double) icon1.getMaxU();
        double maxV1 = (double) icon1.getMaxV();

        tess.addVertexWithUV(minX, y + (double) size, minZ, minU1, minV1);
        tess.addVertexWithUV(minX, y + 0.0D, minZ, minU1, maxV1);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU1, maxV1);
        tess.addVertexWithUV(midX, y + (double) size, midZ, midU1, minV1);

        tess.addVertexWithUV(midX, y + (double) size, midZ, midU1, minV1);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU1, maxV1);
        tess.addVertexWithUV(minX, y + 0.0D, minZ, maxU1, maxV1);
        tess.addVertexWithUV(minX, y + (double) size, minZ, maxU1, minV1);

        tess.addVertexWithUV(minX, y + (double) size, maxZ, minU1, minV1);
        tess.addVertexWithUV(minX, y + 0.0D, maxZ, minU1, maxV1);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU1, maxV1);
        tess.addVertexWithUV(midX, y + (double) size, midZ, midU1, minV1);

        tess.addVertexWithUV(midX, y + (double) size, midZ, midU1, minV1);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU1, maxV1);
        tess.addVertexWithUV(minX, y + 0.0D, maxZ, maxU1, maxV1);
        tess.addVertexWithUV(minX, y + (double) size, maxZ, maxU1, minV1);

        double minU2 = (double) icon2.getMinU();
        double minV2 = (double) icon2.getMinV();
        double midU2 = (double) (icon2.getMaxU() + icon2.getMinU()) / 2;
        double maxU2 = (double) icon2.getMaxU();
        double maxV2 = (double) icon2.getMaxV();

        tess.addVertexWithUV(midX, y + (double) size, midZ, midU2, minV2);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU2, maxV2);
        tess.addVertexWithUV(maxX, y + 0.0D, maxZ, maxU2, maxV2);
        tess.addVertexWithUV(maxX, y + (double) size, maxZ, maxU2, minV2);

        tess.addVertexWithUV(maxX, y + (double) size, maxZ, minU2, minV2);
        tess.addVertexWithUV(maxX, y + 0.0D, maxZ, minU2, maxV2);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU2, maxV2);
        tess.addVertexWithUV(midX, y + (double) size, midZ, midU2, minV2);

        tess.addVertexWithUV(midX, y + (double) size, midZ, midU2, minV2);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU2, maxV2);
        tess.addVertexWithUV(maxX, y + 0.0D, minZ, maxU2, maxV2);
        tess.addVertexWithUV(maxX, y + (double) size, minZ, maxU2, minV2);

        tess.addVertexWithUV(maxX, y + (double) size, minZ, minU2, minV2);
        tess.addVertexWithUV(maxX, y + 0.0D, minZ, minU2, maxV2);
        tess.addVertexWithUV(midX, y + 0.0D, midZ, midU2, maxV2);
        tess.addVertexWithUV(midX, y + (double) size, midZ, midU2, minV2);
    }

    /**
     * Shortcut for all the renderFace methods. These renderFace methods are almost exactly the same as the ones in {@link RenderBlocks}, except they slightly adjust the quads with
     * {@link #FINAGLE}, and {@link #renderFaceYNegFlippable} uses the {@link RenderBlocks#flipTexture} option to flip textures perpendicular to the facing direction.
     */
    public static void renderFace(double x, double y, double z, Block block, RenderBlocks renderer, IIcon icon, int side)
    {
        if (side == 0)
            renderFaceYNegFlippable(renderer, block, x, y, z, icon);
        else if (side == 1)
            renderFaceYPos(renderer, block, x, y, z, icon);
        else if (side == 2)
            renderFaceZNeg(renderer, block, x, y, z, icon);
        else if (side == 3)
            renderFaceZPos(renderer, block, x, y, z, icon);
        else if (side == 4)
            renderFaceXNeg(renderer, block, x, y, z, icon);
        else if (side == 5)
            renderFaceXPos(renderer, block, x, y, z, icon);
    }

    /**
     * The tiny amount to adjust texture quads by, to close gaps between textures and at corners.
     */
    public static final float FINAGLE = 0.00025F;

    /**
     * Renders the given texture to the bottom face of the block. If the {@link RenderBlocks#flipTexture} option is true, flips textures perpendicular to the facing direction.
     */
    public static void renderFaceYNegFlippable(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinX * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxX * 16.0D);
        double d5 = (double) icon.getInterpolatedV(renderer.renderMinZ * 16.0D);
        double d6 = (double) icon.getInterpolatedV(renderer.renderMaxZ * 16.0D);
        double d7;

        if (renderer.flipTexture)
        {
            d7 = d5;
            d5 = d6;
            d6 = d7;
        }

        if (renderer.renderMinX < 0.0D || renderer.renderMaxX > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinZ < 0.0D || renderer.renderMaxZ > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateBottom == 2)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxX * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinX * 16.0D);

            if (renderer.flipTexture)
            {
                d7 = d5;
                d5 = d6;
                d6 = d7;
            }

            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateBottom == 1)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMaxX * 16.0D);

            if (renderer.flipTexture)
            {
                d7 = d5;
                d5 = d6;
                d6 = d7;
            }

            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateBottom == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxX * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxZ * 16.0D);

            if (renderer.flipTexture)
            {
                d7 = d5;
                d5 = d6;
                d6 = d7;
            }

            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMinX;
        double d12 = x + renderer.renderMaxX;
        double d13 = y + renderer.renderMinY;
        double d14 = z + renderer.renderMinZ;
        double d15 = z + renderer.renderMaxZ;

        if (renderer.renderFromInside)
        {
            d11 = x + renderer.renderMaxX;
            d12 = x + renderer.renderMinX;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d15 + FINAGLE, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d14 - FINAGLE, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d14 - FINAGLE, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d15 + FINAGLE, d4, d6);
        }
        else
        {
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d15 + FINAGLE, d8, d10);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d14 - FINAGLE, d3, d5);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d14 - FINAGLE, d7, d9);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d15 + FINAGLE, d4, d6);
        }
    }

    /**
     * Renders the given texture to the top face of the block.
     */
    public static void renderFaceYPos(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinX * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxX * 16.0D);
        double d5 = (double) icon.getInterpolatedV(renderer.renderMinZ * 16.0D);
        double d6 = (double) icon.getInterpolatedV(renderer.renderMaxZ * 16.0D);

        if (renderer.renderMinX < 0.0D || renderer.renderMaxX > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinZ < 0.0D || renderer.renderMaxZ > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        double d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateTop == 1)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxX * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinX * 16.0D);
            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateTop == 2)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMaxX * 16.0D);
            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateTop == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxX * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinZ * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxZ * 16.0D);
            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMinX;
        double d12 = x + renderer.renderMaxX;
        double d13 = y + renderer.renderMaxY;
        double d14 = z + renderer.renderMinZ;
        double d15 = z + renderer.renderMaxZ;

        if (renderer.renderFromInside)
        {
            d11 = x + renderer.renderMaxX;
            d12 = x + renderer.renderMinX;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d15 + FINAGLE, d4, d6);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d14 - FINAGLE, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d14 - FINAGLE, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d15 + FINAGLE, d8, d10);
        }
        else
        {
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d15 + FINAGLE, d4, d6);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13, d14 - FINAGLE, d7, d9);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d14 - FINAGLE, d3, d5);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13, d15 + FINAGLE, d8, d10);
        }
    }

    /**
     * Renders the given texture to the north (z-negative) face of the block.
     */
    public static void renderFaceZNeg(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinX * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxX * 16.0D);

        if (renderer.field_152631_f)
        {
            d4 = (double) icon.getInterpolatedU((1.0D - renderer.renderMinX) * 16.0D);
            d3 = (double) icon.getInterpolatedU((1.0D - renderer.renderMaxX) * 16.0D);
        }

        double d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxY * 16.0D);
        double d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinY * 16.0D);
        double d7;

        if (renderer.flipTexture)
        {
            d7 = d3;
            d3 = d4;
            d4 = d7;
        }

        if (renderer.renderMinX < 0.0D || renderer.renderMaxX > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinY < 0.0D || renderer.renderMaxY > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateEast == 2)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinY * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxY * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinX * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxX * 16.0D);
            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateEast == 1)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxY * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinY * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxX * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinX * 16.0D);
            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateEast == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxX * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinY * 16.0D);
            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMinX;
        double d12 = x + renderer.renderMaxX;
        double d13 = y + renderer.renderMinY;
        double d14 = y + renderer.renderMaxY;
        double d15 = z + renderer.renderMinZ;

        if (renderer.renderFromInside)
        {
            d11 = x + renderer.renderMaxX;
            d12 = x + renderer.renderMinX;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d11 - FINAGLE, d14 + FINAGLE, d15, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d12 + FINAGLE, d14 + FINAGLE, d15, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13 - FINAGLE, d15, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13 - FINAGLE, d15, d4, d6);
        }
        else
        {
            tessellator.addVertexWithUV(d11 - FINAGLE, d14 + FINAGLE, d15, d7, d9);
            tessellator.addVertexWithUV(d12 + FINAGLE, d14 + FINAGLE, d15, d3, d5);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13 - FINAGLE, d15, d8, d10);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13 - FINAGLE, d15, d4, d6);
        }
    }

    /**
     * Renders the given texture to the south (z-positive) face of the block.
     */
    public static void renderFaceZPos(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinX * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxX * 16.0D);
        double d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxY * 16.0D);
        double d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinY * 16.0D);
        double d7;

        if (renderer.flipTexture)
        {
            d7 = d3;
            d3 = d4;
            d4 = d7;
        }

        if (renderer.renderMinX < 0.0D || renderer.renderMaxX > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinY < 0.0D || renderer.renderMaxY > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateWest == 1)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinY * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxY * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxX * 16.0D);
            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateWest == 2)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxY * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMaxX * 16.0D);
            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateWest == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinX * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxX * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinY * 16.0D);
            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMinX;
        double d12 = x + renderer.renderMaxX;
        double d13 = y + renderer.renderMinY;
        double d14 = y + renderer.renderMaxY;
        double d15 = z + renderer.renderMaxZ;

        if (renderer.renderFromInside)
        {
            d11 = x + renderer.renderMaxX;
            d12 = x + renderer.renderMinX;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d11 - FINAGLE, d14 + FINAGLE, d15, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13 - FINAGLE, d15, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13 - FINAGLE, d15, d4, d6);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d12 + FINAGLE, d14 + FINAGLE, d15, d7, d9);
        }
        else
        {
            tessellator.addVertexWithUV(d11 - FINAGLE, d14 + FINAGLE, d15, d3, d5);
            tessellator.addVertexWithUV(d11 - FINAGLE, d13 - FINAGLE, d15, d8, d10);
            tessellator.addVertexWithUV(d12 + FINAGLE, d13 - FINAGLE, d15, d4, d6);
            tessellator.addVertexWithUV(d12 + FINAGLE, d14 + FINAGLE, d15, d7, d9);
        }
    }

    /**
     * Renders the given texture to the west (x-negative) face of the block.
     */
    public static void renderFaceXNeg(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinZ * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxZ * 16.0D);
        double d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxY * 16.0D);
        double d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinY * 16.0D);
        double d7;

        if (renderer.flipTexture)
        {
            d7 = d3;
            d3 = d4;
            d4 = d7;
        }

        if (renderer.renderMinZ < 0.0D || renderer.renderMaxZ > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinY < 0.0D || renderer.renderMaxY > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateNorth == 1)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinY * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinZ * 16.0D);
            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateNorth == 2)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxY * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMinZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMaxZ * 16.0D);
            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateNorth == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinY * 16.0D);
            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMinX;
        double d12 = y + renderer.renderMinY;
        double d13 = y + renderer.renderMaxY;
        double d14 = z + renderer.renderMinZ;
        double d15 = z + renderer.renderMaxZ;

        if (renderer.renderFromInside)
        {
            d14 = z + renderer.renderMaxZ;
            d15 = z + renderer.renderMinZ;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d15 + FINAGLE, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d14 - FINAGLE, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d14 - FINAGLE, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d15 + FINAGLE, d4, d6);
        }
        else
        {
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d15 + FINAGLE, d7, d9);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d14 - FINAGLE, d3, d5);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d14 - FINAGLE, d8, d10);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d15 + FINAGLE, d4, d6);
        }
    }

    /**
     * Renders the given texture to the east (x-positive) face of the block.
     */
    public static void renderFaceXPos(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
    {
        Tessellator tessellator = Tessellator.instance;

        if (renderer.hasOverrideBlockTexture())
        {
            icon = renderer.overrideBlockTexture;
        }

        double d3 = (double) icon.getInterpolatedU(renderer.renderMinZ * 16.0D);
        double d4 = (double) icon.getInterpolatedU(renderer.renderMaxZ * 16.0D);

        if (renderer.field_152631_f)
        {
            d4 = (double) icon.getInterpolatedU((1.0D - renderer.renderMinZ) * 16.0D);
            d3 = (double) icon.getInterpolatedU((1.0D - renderer.renderMaxZ) * 16.0D);
        }

        double d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxY * 16.0D);
        double d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinY * 16.0D);
        double d7;

        if (renderer.flipTexture)
        {
            d7 = d3;
            d3 = d4;
            d4 = d7;
        }

        if (renderer.renderMinZ < 0.0D || renderer.renderMaxZ > 1.0D)
        {
            d3 = (double) icon.getMinU();
            d4 = (double) icon.getMaxU();
        }

        if (renderer.renderMinY < 0.0D || renderer.renderMaxY > 1.0D)
        {
            d5 = (double) icon.getMinV();
            d6 = (double) icon.getMaxV();
        }

        d7 = d4;
        double d8 = d3;
        double d9 = d5;
        double d10 = d6;

        if (renderer.uvRotateSouth == 2)
        {
            d3 = (double) icon.getInterpolatedU(renderer.renderMinY * 16.0D);
            d5 = (double) icon.getInterpolatedV(16.0D - renderer.renderMinZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(16.0D - renderer.renderMaxZ * 16.0D);
            d9 = d5;
            d10 = d6;
            d7 = d3;
            d8 = d4;
            d5 = d6;
            d6 = d9;
        }
        else if (renderer.uvRotateSouth == 1)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxY * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinZ * 16.0D);
            d7 = d4;
            d8 = d3;
            d3 = d4;
            d4 = d8;
            d9 = d6;
            d10 = d5;
        }
        else if (renderer.uvRotateSouth == 3)
        {
            d3 = (double) icon.getInterpolatedU(16.0D - renderer.renderMinZ * 16.0D);
            d4 = (double) icon.getInterpolatedU(16.0D - renderer.renderMaxZ * 16.0D);
            d5 = (double) icon.getInterpolatedV(renderer.renderMaxY * 16.0D);
            d6 = (double) icon.getInterpolatedV(renderer.renderMinY * 16.0D);
            d7 = d4;
            d8 = d3;
            d9 = d5;
            d10 = d6;
        }

        double d11 = x + renderer.renderMaxX;
        double d12 = y + renderer.renderMinY;
        double d13 = y + renderer.renderMaxY;
        double d14 = z + renderer.renderMinZ;
        double d15 = z + renderer.renderMaxZ;

        if (renderer.renderFromInside)
        {
            d14 = z + renderer.renderMaxZ;
            d15 = z + renderer.renderMinZ;
        }

        if (renderer.enableAO)
        {
            tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
            tessellator.setBrightness(renderer.brightnessTopLeft);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d15 + FINAGLE, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d14 - FINAGLE, d4, d6);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d14 - FINAGLE, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d15 + FINAGLE, d3, d5);
        }
        else
        {
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d15 + FINAGLE, d8, d10);
            tessellator.addVertexWithUV(d11, d12 - FINAGLE, d14 - FINAGLE, d4, d6);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d14 - FINAGLE, d7, d9);
            tessellator.addVertexWithUV(d11, d13 + FINAGLE, d15 + FINAGLE, d3, d5);
        }
    }

}
