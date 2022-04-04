package connor135246.campfirebackport.client.rendering;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public class RenderBlockCampfire implements ISimpleBlockRenderingHandler
{

    // HELP! SEAMS BETWEEN TEXTURES?!:
    // - you can see through where some textures meet for some reason. i figured out a way to finagle it on the fire pit bottom and side textures, but it also happens on log
    // corners and edges. is there a better way?
    // - i investigated other mods with unusually shaped blocks... the lanterns from both UpToDateMod and EtFuturumRequiem have the same problem. it's a little harder to notice,
    // but it is there. it's more noticeable on the campfire since it's bigger and behind the logs is the fire texture, which is animated.
    // - there may not be an easy fix. but i'm not merging this change until i can look at the campfire without looking through it. it's too noticeable!

    // TODO:
    // - inventory render for nei.
    // - mixed fire for nei.

    // {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};

    /**
     * Shortcut for all the renderFace methods.
     */
    public void renderFace(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, IIcon icon, int side)
    {
        if (side == 0)
            renderFaceYNeg(renderer, block, x, y, z, icon);
        else if (side == 1)
            renderer.renderFaceYPos(block, x, y, z, icon);
        else if (side == 2)
            renderer.renderFaceZNeg(block, x, y, z, icon);
        else if (side == 3)
            renderer.renderFaceZPos(block, x, y, z, icon);
        else if (side == 4)
            renderer.renderFaceXNeg(block, x, y, z, icon);
        else if (side == 5)
            renderer.renderFaceXPos(block, x, y, z, icon);
    }

    /*
     * public void drawCrossedSquaresTwoIcons(IIcon icon1, IIcon icon2, double x, double y, double z, float scale) { }
     */

    /**
     * @param side
     *            - should be 2 (north), 3 (south), 4 (west), or 5 (east)
     */
    public void renderLogEnd(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, int side)
    {
        if (side == 2 || side == 3)
            renderer.setRenderBounds(0, 0.5F, 0, 0.25F, 0.75F, 1);
        else if (side == 4 || side == 5)
            renderer.setRenderBounds(0, 0.5F, 0, 1, 0.75F, 0.25F);

        y = y - renderer.renderMinY;

        renderFace(access, x, y, z, block, renderer, block.getIcon(0, 0), side);
    }

    /**
     * @param side
     *            - should be 2 (north), 3 (south), 4 (west), or 5 (east)
     * @param lower
     *            - lowers the texture by one pixel
     */
    public void renderLogSide(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, int side, boolean lit,
            boolean lower)
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

        renderFace(access, x, y, z, block, renderer, block.getIcon(0, lit ? -2 : 0), side);
    }

    /**
     * @param side
     *            - should be 0 (down) or 1 (up)
     * @param rotate
     *            - 0 and 3 are opposite, 1 and 2 are opposite
     */
    public void renderLogTopOrBot(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, int side, int rotate,
            boolean lit)
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

        renderFace(access, x, y, z, block, renderer, block.getIcon(0, lit ? -2 : 0), side);

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
    public void renderFirepitSide(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, int side, boolean front)
    {
        float minX = 0, maxX = 1, minZ = 0, maxZ = 1;

        // fixes an issue where you can see between textures on a face
        float texbridge = 0.0F;// 0.00035F;

        if (front)
        {
            if (side == 2 || side == 3)
                maxX = 0.375F + texbridge;
            else if (side == 4 || side == 5)
                maxZ = 0.375F + texbridge;
        }
        else
        {
            if (side == 2 || side == 3)
                minX = 0.625F - texbridge;
            else if (side == 4 || side == 5)
                minZ = 0.625F - texbridge;
        }

        renderer.setRenderBounds(minX, 0, minZ, maxX, 0.0625F, maxZ);

        x = x - renderer.renderMinX - (side == 3 ? texbridge : 0);
        z = z - renderer.renderMinZ - (side == 4 ? texbridge : 0);

        renderFace(access, x, y, z, block, renderer, block.getIcon(0, 0), side);
    }

    /**
     * @param side
     *            - should be 0 (down) or 1 (up)
     * @param rotate
     *            - 0 and 3 are opposite, 1 and 2 are opposite
     */
    public void renderFirepitTopOrBot(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer, int side, int rotate, boolean lit)
    {
        float minX = 0, maxX = 1, minZ = 0, maxZ = 1;

        // fixes an issue where you can see between textures on a face
        float texbridge = 0.0F;// 0.00035F;

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
            maxZ = minZ + 0.375F + texbridge;
            z -= texbridge;
        }
        else if ((rotate == 1 && side == 1) || (rotate == 2 && side == 0))
        {
            minX = 0.125F;
            maxX = minX + 0.375F;
            minX -= texbridge;
        }
        else if ((rotate == 2 && side == 1) || (rotate == 1 && side == 0))
        {
            minX = 0.5F;
            maxX = minX + 0.375F + texbridge;
            x -= texbridge;
        }
        else if (rotate == 3)
        {
            minZ = 0.125F;
            maxZ = minZ + 0.375F;
            minZ -= texbridge;
        }

        renderer.setRenderBounds(minX, 0, minZ, maxX, 1, maxZ);

        x = x - renderer.renderMinX;
        z = z - renderer.renderMinZ;

        renderFace(access, x, y, z, block, renderer, block.getIcon(0, side == 1 && lit ? -2 : 0), side);

        renderer.flipTexture = false;
        renderer.uvRotateBottom = 0;
        renderer.uvRotateTop = 0;
    }

    public void renderFire(IBlockAccess access, double x, double y, double z, Block block, RenderBlocks renderer)
    {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(15728880); // 15 << 20 | 15 << 4, aka max brightness
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderer.drawCrossedSquares(block.getIcon(0, -3), x, y, z, 1.0F);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess access, int x, int y, int z, Block block, int modelId, RenderBlocks renderer)
    {
        block = CampfireBackportBlocks.campfire; // TODO

        Tessellator tess = Tessellator.instance;
        renderer.enableAO = false;

        int color = block.colorMultiplier(renderer.blockAccess, x, y, z);
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

        tess.setBrightness(block.getMixedBrightnessForBlock(renderer.blockAccess, x, y, z));

        boolean isLit = CampfireBackportBlocks.isLitCampfire(block);
        int meta = 2;// access.getBlockMetadata(x, y, z);
        boolean northSouth = !(meta == 4 || meta == 5);
        double sideLogY = 0.1875, logY1 = 0.25, logY2 = 0.4375, firepitO = 0.3125, logO1 = 0.0625, logO2 = 0.6875;
        float colorYNeg = 0.5F, colorYPos = 1.0F, colorZ = 0.8F, colorX = 0.6F;

        tess.setColorOpaque_F(r * colorYNeg, g * colorYNeg, b * colorYNeg);

        // upper log bottoms
        renderLogTopOrBot(access, northSouth ? x : (x + logO1), y + sideLogY, northSouth ? (z + logO1) : z, block, renderer, 0,
                meta == 3 ? 0 : (meta == 4 ? 2 : (meta == 5 ? 1 : 3)), isLit);
        renderLogTopOrBot(access, northSouth ? x : (x + logO2), y + sideLogY, northSouth ? (z + logO2) : z, block, renderer, 0,
                meta == 3 ? 0 : (meta == 4 ? 2 : (meta == 5 ? 1 : 3)), isLit);

        // bottom sides
        if (renderer.renderAllFaces || block.shouldSideBeRendered(access, x, y - 1, z, 0))
        {
            int rotateFromMeta = meta == 3 ? 2 : (meta == 4 ? 3 : (meta == 5 ? 0 : 1));

            renderLogTopOrBot(access, northSouth ? (x + logO1) : x, y, northSouth ? z : (z + logO1), block, renderer, 0, rotateFromMeta, false);
            renderLogTopOrBot(access, northSouth ? (x + logO2) : x, y, northSouth ? z : (z + logO2), block, renderer, 0, rotateFromMeta, false);
            renderFirepitTopOrBot(access, northSouth ? (x + firepitO) : x, y, northSouth ? z : (z + firepitO), block, renderer, 0, rotateFromMeta, isLit);
        }

        tess.setColorOpaque_F(r * colorYPos, g * colorYPos, b * colorYPos);

        // north-south log tops
        renderLogTopOrBot(access, x + logO1, northSouth ? (y - 1 + logY1) : (y - 1 + logY2), z, block, renderer, 1, meta == 3 || meta == 5 ? 2 : 1, false);
        renderLogTopOrBot(access, x + logO2, northSouth ? (y - 1 + logY1) : (y - 1 + logY2), z, block, renderer, 1, meta == 3 || meta == 5 ? 2 : 1, false);

        // east-west log tops
        renderLogTopOrBot(access, x, northSouth ? (y - 1 + logY2) : (y - 1 + logY1), z + logO1, block, renderer, 1, meta == 3 || meta == 5 ? 0 : 3, false);
        renderLogTopOrBot(access, x, northSouth ? (y - 1 + logY2) : (y - 1 + logY1), z + logO2, block, renderer, 1, meta == 3 || meta == 5 ? 0 : 3, false);

        // firepit top
        renderFirepitTopOrBot(access, northSouth ? (x + firepitO) : x, y - 0.9375, northSouth ? z : (z + firepitO), block, renderer, 1,
                meta == 3 ? 2 : (meta == 4 ? 0 : (meta == 5 ? 3 : 1)), isLit);

        tess.setColorOpaque_F(r * colorZ, g * colorZ, b * colorZ);

        // north log sides
        renderLogSide(access, x, northSouth ? (y + sideLogY) : y, z + logO1, block, renderer, 2, northSouth ? isLit : false, false);
        renderLogSide(access, x, northSouth ? (y + sideLogY) : y, z + logO2, block, renderer, 2, isLit, !northSouth);

        // south log sides
        renderLogSide(access, x, northSouth ? (y + sideLogY) : y, z - logO1, block, renderer, 3, northSouth ? isLit : false, false);
        renderLogSide(access, x, northSouth ? (y + sideLogY) : y, z - logO2, block, renderer, 3, isLit, !northSouth);

        // north side faces
        if (renderer.renderAllFaces || block.shouldSideBeRendered(access, x, y, z - 1, 2))
        {
            renderLogEnd(access, x + 0.0625, northSouth ? y : (y + sideLogY), z, block, renderer, 2);
            renderLogEnd(access, x + 0.6875, northSouth ? y : (y + sideLogY), z, block, renderer, 2);
            if (northSouth)
                renderFirepitSide(access, x + 0.3125, y, z, block, renderer, 2, meta == 2);
        }

        // south side faces
        if (renderer.renderAllFaces || block.shouldSideBeRendered(access, x, y, z + 1, 3))
        {
            renderLogEnd(access, x + 0.6875, northSouth ? y : (y + sideLogY), z, block, renderer, 3);
            renderLogEnd(access, x + 0.0625, northSouth ? y : (y + sideLogY), z, block, renderer, 3);
            if (northSouth)
                renderFirepitSide(access, x + 0.3125, y, z, block, renderer, 3, meta == 3);
        }

        tess.setColorOpaque_F(r * colorX, g * colorX, b * colorX);

        // west log sides
        renderLogSide(access, x + logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 4, northSouth ? false : isLit, false);
        renderLogSide(access, x + logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 4, isLit, northSouth);

        // east log sides
        renderLogSide(access, x - logO1, northSouth ? y : (y + sideLogY), z, block, renderer, 5, northSouth ? false : isLit, false);
        renderLogSide(access, x - logO2, northSouth ? y : (y + sideLogY), z, block, renderer, 5, isLit, northSouth);

        // west side faces
        if (renderer.renderAllFaces || block.shouldSideBeRendered(access, x - 1, y, z, 4))
        {
            renderLogEnd(access, x, northSouth ? (y + sideLogY) : y, z + 0.6875, block, renderer, 4);
            renderLogEnd(access, x, northSouth ? (y + sideLogY) : y, z + 0.0625, block, renderer, 4);
            if (!northSouth)
                renderFirepitSide(access, x, y, z + 0.3125, block, renderer, 4, meta == 4);
        }

        // east side faces
        if (renderer.renderAllFaces || block.shouldSideBeRendered(access, x + 1, y, z, 5))
        {
            renderLogEnd(access, x, northSouth ? (y + sideLogY) : y, z + 0.0625, block, renderer, 5);
            renderLogEnd(access, x, northSouth ? (y + sideLogY) : y, z + 0.6875, block, renderer, 5);
            if (!northSouth)
                renderFirepitSide(access, x, y, z + 0.3125, block, renderer, 5, meta == 5);
        }

        // fire
        if (isLit)
        {
            tess.setBrightness(15728880); // 15 << 20 | 15 << 4, aka max brightness
            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            renderer.drawCrossedSquares(block.getIcon(0, -3), x, y + 0.0625, z, 1.0F);
        }

        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer)
    {
        // TODO for nei
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId)
    {
        return false;
    }

    @Override
    public int getRenderId()
    {
        return BlockCampfire.renderId;
    }

    /**
     * Copy-pasted from {@link RenderBlocks#renderFaceYNeg}, except it now uses the {@link RenderBlocks#flipTexture} option to flip textures perpendicular to the facing direction.
     */
    public void renderFaceYNeg(RenderBlocks renderer, Block block, double x, double y, double z, IIcon icon)
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
            tessellator.addVertexWithUV(d11, d13, d15, d8, d10);
            tessellator.setColorOpaque_F(renderer.colorRedBottomLeft, renderer.colorGreenBottomLeft, renderer.colorBlueBottomLeft);
            tessellator.setBrightness(renderer.brightnessBottomLeft);
            tessellator.addVertexWithUV(d11, d13, d14, d3, d5);
            tessellator.setColorOpaque_F(renderer.colorRedBottomRight, renderer.colorGreenBottomRight, renderer.colorBlueBottomRight);
            tessellator.setBrightness(renderer.brightnessBottomRight);
            tessellator.addVertexWithUV(d12, d13, d14, d7, d9);
            tessellator.setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
            tessellator.setBrightness(renderer.brightnessTopRight);
            tessellator.addVertexWithUV(d12, d13, d15, d4, d6);
        }
        else
        {
            tessellator.addVertexWithUV(d11, d13, d15, d8, d10);
            tessellator.addVertexWithUV(d11, d13, d14, d3, d5);
            tessellator.addVertexWithUV(d12, d13, d14, d7, d9);
            tessellator.addVertexWithUV(d12, d13, d15, d4, d6);
        }
    }

}
