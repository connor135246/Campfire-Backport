package connor135246.campfirebackport.common.blocks;

import connor135246.campfirebackport.common.items.ItemBlockWithMetadataBase;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.Block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;

public class CampfireBackportBlocks
{

    public static final Block campfire = new BlockCampfire(true).setLightLevel(1.0F).setBlockName("campfire")
            .setBlockTextureName(Reference.MODID + ":" + "campfire");
    public static final Block campfire_base = new BlockCampfire(false).setLightLevel(0.0F).setBlockName("campfire_base")
            .setBlockTextureName(Reference.MODID + ":" + "campfire_base");

    public static void preInit()
    {
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire, ItemBlockWithMetadataBase.class, "campfire");
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire_base, ItemBlockWithMetadataBase.class, "campfire_base");
    }

}
