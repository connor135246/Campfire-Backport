package connor135246.campfirebackport.common.recipes;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportRecipes
{

    public static final String oreSoulSoil = "soulSoil", oreSoulSand = "soulSand";

    /**
     * adds oredicts. adds the crafting recipes for campfires, which depend on various config settings.
     */
    public static void postInit()
    {
        ItemStack campfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsRegular(), EnumCampfireType.regIndex));

        GameRegistry.addRecipe(new ShapedOreRecipe(campfireResult.copy(),
                " A ", "ABA", "CCC", 'A', "stickWood", 'B', "charcoal", 'C', "logWood"));

        if (!CampfireBackportConfig.charcoalOnly)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(campfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', "coal", 'C', "logWood"));
        }

        //

        OreDictionary.registerOre(oreSoulSand, Blocks.soul_sand);
        OreDictionary.getOres(oreSoulSoil, true); // create soulSoil ore as empty

        // add netherlicious soul soil to the soulSoil ore. netherlicious has an ore for both soul sand and soul soil (called blockSoul), but not one for just soul soil.
        Block soulSoil = GameData.getBlockRegistry().getObject("netherlicious:SoulSoil");
        if (soulSoil != Blocks.air)
            OreDictionary.registerOre(oreSoulSoil, new ItemStack(soulSoil, 1, OreDictionary.WILDCARD_VALUE));

        ItemStack soulcampfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.soulIndex));

        GameRegistry.addRecipe(new ShapedOreRecipe(soulcampfireResult.copy(),
                " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreSoulSoil, 'C', "logWood"));

        if (!CampfireBackportConfig.soulSoilOnly)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(soulcampfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreSoulSand, 'C', "logWood"));
        }

        //

        ItemStack foxfirecampfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.foxfireIndex));

        Item foxfirePowder = GameData.getItemRegistry().getObject("netherlicious:FoxfirePowder");
        if (foxfirePowder != null)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(foxfirecampfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', foxfirePowder, 'C', "logWood"));
        }

        //

        ItemStack shadowcampfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.shadowIndex));

        Block cryingBlackstone = GameData.getBlockRegistry().getObject("netherlicious:CryingBlackstone");
        if (cryingBlackstone != Blocks.air)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(shadowcampfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', cryingBlackstone, 'C', "logWood"));
        }
    }

}
