package connor135246.campfirebackport.common.recipes;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportRecipes
{

    // oredicts

    public static final String oreCoal = "coal", oreCharcoal = "charcoal", oreSoulSand = "soulSand", oreSoulSoil = "soulSoil";

    // recipe suppliers

    public static final RecipeHolder charcoalRecipe = new RecipeHolder(() -> new ShapedOreRecipe(
            CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsRegular(), EnumCampfireType.regIndex),
            " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreCharcoal, 'C', "logWood"));

    public static final RecipeHolder coalRecipe = new RecipeHolder(() -> {
        if (CampfireBackportConfig.charcoalOnly)
            return null;

        return new ShapedOreRecipe(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsRegular(), EnumCampfireType.regIndex),
                " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreCoal, 'C', "logWood");

    });

    public static final RecipeHolder soulSoilRecipe = new RecipeHolder(() -> new ShapedOreRecipe(
            CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.soulIndex),
            " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreSoulSoil, 'C', "logWood"));

    public static final RecipeHolder soulSandRecipe = new RecipeHolder(() -> {
        if (CampfireBackportConfig.soulSoilOnly)
            return null;

        return new ShapedOreRecipe(
                CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.soulIndex),
                " A ", "ABA", "CCC", 'A', "stickWood", 'B', oreSoulSand, 'C', "logWood");
    });

    public static final RecipeHolder foxfireRecipe = new RecipeHolder(() -> {
        if (CampfireBackportCompat.isNetherliciousLoaded || CampfireBackportConfig.enableExtraCampfires)
        {
            Item foxfirePowder = GameData.getItemRegistry().getObject("netherlicious:FoxfirePowder");
            if (foxfirePowder != null)
            {
                return new ShapedOreRecipe(
                        CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.foxfireIndex),
                        " A ", "ABA", "CCC", 'A', "stickWood", 'B', foxfirePowder, 'C', "logWood");
            }
        }
        return null;
    });

    public static final RecipeHolder shadowRecipe = new RecipeHolder(() -> {
        if (CampfireBackportCompat.isNetherliciousLoaded || CampfireBackportConfig.enableExtraCampfires)
        {
            Block cryingBlackstone = GameData.getBlockRegistry().getObject("netherlicious:CryingBlackstone");
            if (cryingBlackstone != Blocks.air)
            {
                return new ShapedOreRecipe(
                        CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.acceptsSoul(), EnumCampfireType.shadowIndex),
                        " A ", "ABA", "CCC", 'A', "stickWood", 'B', cryingBlackstone, 'C', "logWood");
            }
        }
        return null;
    });

    public static final List<RecipeHolder> RECIPES = Lists.newArrayList(charcoalRecipe, coalRecipe, soulSoilRecipe, soulSandRecipe, foxfireRecipe, shadowRecipe);

    /**
     * adds oredicts. <br>
     * adds the crafting recipes for campfires, which depend on various config settings.
     */
    public static void postInit()
    {
        OreDictionary.registerOre(oreCoal, new ItemStack(Items.coal, 1, 0));
        OreDictionary.registerOre(oreCharcoal, new ItemStack(Items.coal, 1, 1));

        OreDictionary.registerOre(oreSoulSand, Blocks.soul_sand);
        OreDictionary.getOres(oreSoulSoil, true); // create soulSoil ore as empty

        // add netherlicious soul soil to the soulSoil ore. netherlicious has an ore for both soul sand and soul soil (called blockSoul), but not one for just soul soil.
        Block soulSoil = GameData.getBlockRegistry().getObject("netherlicious:SoulSoil");
        if (soulSoil != Blocks.air)
            OreDictionary.registerOre(oreSoulSoil, new ItemStack(soulSoil, 1, OreDictionary.WILDCARD_VALUE));

        RECIPES.forEach(recipe -> recipe.reset());
    }

    /**
     * Very similar to an IUndoableAction from CraftTweaker.
     */
    public static class RecipeHolder
    {
        protected final Supplier<IRecipe> supplier;
        @Nullable
        protected IRecipe recipe = null;

        public RecipeHolder(Supplier<IRecipe> supplier)
        {
            this.supplier = supplier;
        }

        public void reset()
        {
            if (recipe != null)
                CraftingManager.getInstance().getRecipeList().remove(recipe);

            recipe = supplier.get();

            if (recipe != null)
                CraftingManager.getInstance().getRecipeList().add(recipe);
        }
    }

}
