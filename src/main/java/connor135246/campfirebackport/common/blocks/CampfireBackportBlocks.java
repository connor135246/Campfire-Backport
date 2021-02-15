package connor135246.campfirebackport.common.blocks;

import java.util.List;

import com.google.common.collect.Lists;

import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

public class CampfireBackportBlocks
{

    public static final Block campfire = createCampfireBlock(true, EnumCampfireType.regular, "campfire", 1.0F);
    public static final Block campfire_base = createCampfireBlock(false, EnumCampfireType.regular, "campfire_base", 0.0F);
    public static final Block soul_campfire = createCampfireBlock(true, EnumCampfireType.soul, "soul_campfire", 0.67F);
    public static final Block soul_campfire_base = createCampfireBlock(false, EnumCampfireType.soul, "soul_campfire_base", 0.0F);

    /** a list containing the 4 campfires */
    public static final List<Block> LIST_OF_CAMPFIRES = Lists.newArrayList(campfire, soul_campfire, campfire_base, soul_campfire_base);

    /**
     * registers campfire blocks
     */
    public static void preInit()
    {
        LIST_OF_CAMPFIRES.forEach(cblock -> {
            GameRegistry.registerBlock(cblock, ItemBlockCampfire.class, cblock.getUnlocalizedName().substring(5));
        });
    }

    private static Block createCampfireBlock(boolean lit, String type, String name, float lightLevel)
    {
        return new BlockCampfire(lit, type).setLightLevel(lightLevel).setBlockName(name).setBlockTextureName(Reference.MODID + ":" + name);
    }

    //

    /**
     * @param lit
     *            - true if you want a lit campfire, false if you want an unlit campfire
     * @param type
     *            - "regular" for regular campfire, "soul" for soul campfire (if neither, defaults to regular)
     * @return the corresponding campfire block
     */
    public static Block getBlockFromLitAndType(boolean lit, String type)
    {
        return LIST_OF_CAMPFIRES.get((lit ? 0 : 2) + EnumCampfireType.index(type));
    }

}
