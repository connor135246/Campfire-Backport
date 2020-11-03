package connor135246.campfirebackport.common.blocks;

import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

public class CampfireBackportBlocks
{

    public static final Block campfire = new BlockCampfire(true, EnumCampfireType.regular).setLightLevel(1.0F).setBlockName("campfire")
            .setBlockTextureName(Reference.MODID + ":" + "campfire");
    public static final Block campfire_base = new BlockCampfire(false, EnumCampfireType.regular).setLightLevel(0.0F).setBlockName("campfire_base")
            .setBlockTextureName(Reference.MODID + ":" + "campfire_base");
    public static final Block soul_campfire = new BlockCampfire(true, EnumCampfireType.soul).setLightLevel(0.67F).setBlockName("soul_campfire")
            .setBlockTextureName(Reference.MODID + ":" + "soul_campfire");
    public static final Block soul_campfire_base = new BlockCampfire(false, EnumCampfireType.soul).setLightLevel(0.0F).setBlockName("soul_campfire_base")
            .setBlockTextureName(Reference.MODID + ":" + "soul_campfire_base");

    public static void preInit()
    {
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire, ItemBlockCampfire.class, "campfire");
        GameRegistry.registerBlock(CampfireBackportBlocks.campfire_base, ItemBlockCampfire.class, "campfire_base");
        GameRegistry.registerBlock(CampfireBackportBlocks.soul_campfire, ItemBlockCampfire.class, "soul_campfire");
        GameRegistry.registerBlock(CampfireBackportBlocks.soul_campfire_base, ItemBlockCampfire.class, "soul_campfire_base");
    }

    //

    /** a double array where the first index refers to the lit state (0 = lit, 1 = unlit) and the second index refers to the campfire type (0 = regular, 1 = soul) */
    private static final Block[][] CAMPFIRE_REF_TABLE = new Block[][] { { campfire, soul_campfire }, { campfire_base, soul_campfire_base } };

    /**
     * @param lit
     *            - true if you want a lit campfire, false if you want an unlit campfire
     * @param type
     *            - "regular" for regular campfire, "soul" for soul campfire (if neither, defaults to regular)
     * @return the corresponding campfire block
     */
    public static Block getBlockFromLitAndType(boolean lit, String type)
    {
        return CAMPFIRE_REF_TABLE[lit ? 0 : 1][EnumCampfireType.toInt(type)];
    }

}
