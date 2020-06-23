package connor135246.campfirebackport.common.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;

/**
 * Extends ItemBlockWithMetadata from vanilla, because for some reason using the vanilla one causes a crash.
 */
public class ItemBlockWithMetadataBase extends ItemBlockWithMetadata
{
    public ItemBlockWithMetadataBase(Block block)
    {
        super(block, block);
    }
}
