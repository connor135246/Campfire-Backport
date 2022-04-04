package connor135246.campfirebackport.common.blocks;

import java.util.List;

import com.google.common.collect.Lists;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

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
        
        GameRegistry.registerBlock(new BlockC(Material.wood).setBlockName("campfire_log").setBlockTextureName(Reference.MODID + ":" + "campfire_log"), "campfire_log");
    }

    /**
     * makes the campfire block, or the botania campfire block if botania is installed
     */
    private static Block createCampfireBlock(boolean lit, String type, String name, float lightLevel)
    {
        Block block = null;

        if (CampfireBackportCompat.isBotaniaLoaded)
        {
            try
            {
                block = (Block) Class.forName(Reference.MOD_PACKAGE + ".common.compat.botania.BlockCampfireBotania").getConstructor(boolean.class, String.class)
                        .newInstance(lit, type);
            }
            catch (Exception excep)
            {
                CampfireBackportCompat.logError("Botania");
                block = null;
            }
        }

        if (block == null)
            block = new BlockCampfire(lit, type);

        return block.setLightLevel(lightLevel).setBlockName(name).setBlockTextureName(Reference.MODID + ":" + name);
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

    /**
     * @return true if and only if the given block is a lit campfire
     */
    public static boolean isLitCampfire(Block block)
    {
        return block instanceof BlockCampfire ? ((BlockCampfire) block).isLit() : false;
    }

    /**
     * @return true if and only if the given block is an unlit campfire
     */
    public static boolean isUnlitCampfire(Block block)
    {
        return block instanceof BlockCampfire ? !((BlockCampfire) block).isLit() : false;
    }

}
