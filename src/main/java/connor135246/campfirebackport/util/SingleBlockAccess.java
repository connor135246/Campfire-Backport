package connor135246.campfirebackport.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * A dummy {@link IBlockAccess} that contains a single block at 0, 0, 0.
 */
public class SingleBlockAccess implements IBlockAccess
{

    /** commonly used accesses */
    private static final SingleBlockAccess GRASS = new SingleBlockAccess(Blocks.grass), HAY_BLOCK = new SingleBlockAccess(Blocks.hay_block), STONE = new SingleBlockAccess(Blocks.stone);

    public Block block;
    public int meta;

    public SingleBlockAccess(Block block, int meta)
    {
        this.block = block;
        this.meta = meta;
    }

    public SingleBlockAccess(Block block)
    {
        this(block, 0);
    }

    public static SingleBlockAccess getSingleBlockAccess(Block block, int meta)
    {
        if (meta == 0)
        {
            if (block == Blocks.grass)
                return GRASS;
            else if (block == Blocks.hay_block)
                return HAY_BLOCK;
            else if (block == Blocks.stone)
                return STONE;
        }
        return new SingleBlockAccess(block, meta);
    }

    public static SingleBlockAccess getSingleBlockAccess(Block block)
    {
        return getSingleBlockAccess(block, 0);
    }

    @Override
    public Block getBlock(int x, int y, int z)
    {
        return x == 0 && y == 0 && z == 0 ? block : Blocks.air;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z)
    {
        return null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light)
    {
        return MiscUtil.MAX_LIGHT_BRIGHTNESS;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z)
    {
        return x == 0 && y == 0 && z == 0 ? meta : 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int direction)
    {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z)
    {
        return getBlock(x, y, z).isAir(this, x, y, z);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z)
    {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight()
    {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache()
    {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default)
    {
        return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
    }

}