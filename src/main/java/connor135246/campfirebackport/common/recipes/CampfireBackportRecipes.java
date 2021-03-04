package connor135246.campfirebackport.common.recipes;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportRecipes
{

    /**
     * adds the crafting recipes for campfires. depends on various config settings.
     */
    public static void postInit()
    {
        ItemStack campfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsRegular(), EnumCampfireType.regular));

        GameRegistry.addRecipe(new ShapedOreRecipe(campfireResult.copy(),
                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 1), 'C', "logWood"));

        if (!CampfireBackportConfig.charcoalOnly)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(campfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 0), 'C', "logWood"));
        }

        //

        ItemStack soulcampfireResult = new ItemStack(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.soul));

        Block soulSoil = GameData.getBlockRegistry().getObject("netherlicious:SoulSoil");

        if (soulSoil != Blocks.air)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(soulcampfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(soulSoil), 'C', "logWood"));
        }

        if (soulSoil == Blocks.air || !CampfireBackportConfig.soulSoilOnly)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(soulcampfireResult.copy(),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Blocks.soul_sand), 'C', "logWood"));
        }
    }

}
