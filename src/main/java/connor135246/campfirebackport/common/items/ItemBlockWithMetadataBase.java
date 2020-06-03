package connor135246.campfirebackport.common.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;

public class ItemBlockWithMetadataBase extends ItemBlockWithMetadata
{

    /**
     * Extends ItemBlockWithMetadata from vanilla, because for some reason using the vanilla one causes a crash.
     * 
     * @param block
     */
    public ItemBlockWithMetadataBase(Block block)
    {
        super(block, block);
    }
}
