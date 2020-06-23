package connor135246.campfirebackport.common.blocks;

import connor135246.campfirebackport.common.items.ItemBlockWithMetadataBase;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

public class CampfireBackportBlocks
{

    public static final Block campfire = new BlockCampfire(true, EnumCampfireType.REGULAR).setLightLevel(1.0F).setBlockName("campfire")
            .setBlockTextureName(Reference.MODID + ":" + "campfire");
    public static final Block campfire_base = new BlockCampfire(false, EnumCampfireType.REGULAR).setLightLevel(0.0F).setBlockName("campfire_base")
            .setBlockTextureName(Reference.MODID + ":" + "campfire_base");
    public static final Block soul_campfire = new BlockCampfire(true, EnumCampfireType.SOUL).setLightLevel(0.67F).setBlockName("soul_campfire")
            .setBlockTextureName(Reference.MODID + ":" + "soul_campfire");
    public static final Block soul_campfire_base = new BlockCampfire(false, EnumCampfireType.SOUL).setLightLevel(0.0F).setBlockName("soul_campfire_base")
            .setBlockTextureName(Reference.MODID + ":" + "soul_campfire_base");

    public static void preInit()
    {
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire, ItemBlockWithMetadataBase.class, "campfire");
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire_base, ItemBlockWithMetadataBase.class, "campfire_base");
        GameRegistry.registerBlock(CampfireBackportBlocks.soul_campfire, ItemBlockWithMetadataBase.class, "soul_campfire");
        GameRegistry.registerBlock(CampfireBackportBlocks.soul_campfire_base, ItemBlockWithMetadataBase.class, "soul_campfire_base");
    }

}
