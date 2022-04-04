package connor135246.campfirebackport.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockC extends Block
{

    protected BlockC(Material p_i45394_1_)
    {
        super(p_i45394_1_);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.4375F, 1.0F);
    }
    
    @Override
    public int getRenderType()
    {
        return BlockCampfire.renderId;
    }
    
    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

}
