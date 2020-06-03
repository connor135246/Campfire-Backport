package connor135246.campfirebackport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportConfig
{
    public static boolean charcoalOnly;
    public static boolean regenCampfires;
    public static int regenLevel;
    public static int regenTime;
    public static int regenWait;
    public static int regenRadius;
    public static boolean autoRecipe;
    public static boolean allowAutomation;
    public static boolean startUnlit;
    public static boolean maintainState;
    public static boolean silkRequired;
    public static String[] recipeList;
    public static String[] autoBlacklistStrings;
    public static boolean printCampfireRecipes;

    // public static final String[] defaultRecipeList = new String[0];
    public static final String[] defaultRecipeList = new String[] { "minecraft:porkchop/minecraft:cooked_porkchop", "minecraft:beef/minecraft:cooked_beef",
            "minecraft:chicken/minecraft:cooked_chicken", "minecraft:potato/minecraft:baked_potato",
            "minecraft:fish:0/minecraft:cooked_fished:0", "minecraft:fish:1/minecraft:cooked_fished:1" };

    public static ArrayList<CampfireRecipe> autoRecipeList = new ArrayList<CampfireRecipe>();
    public static ArrayList<ItemStack> autoBlacklistStacks = new ArrayList<ItemStack>();

    public static void doConfig(Configuration config)
    {
        String cat = Configuration.CATEGORY_GENERAL;

        Pattern recipePat = Pattern.compile("(\\w+:\\w+)(:\\d+)?\\/(\\w+:\\w+)(:\\d+)?(@\\d+)?(\\/\\d+)?");
        Pattern blacklistPat = Pattern.compile("(\\w+:\\w+)(:\\d+)?");

        charcoalOnly = config.get(cat, "Charcoal Only", false,
                "If true, campfires can only be crafted using charcoal. "
                        + "In vanilla either coal or charcoal can be used, but breaking campfires always drops charcoal. "
                        + "If for some reason you don't want players to be able to turn coal into charcoal, turn this on.")
                .setRequiresMcRestart(true).getBoolean();

        regenCampfires = config.get(cat, "Regen Campfires", false,
                "If true, lit campfires will periodically apply a regeneration effect to nearby players.")
                .getBoolean();

        regenLevel = config.get(cat, "Regen Level", 0,
                "The level of the regeneraton effect to apply.", 0, 31)
                .getInt();

        regenTime = config.get(cat, "Regen Timer", 50,
                "The timer of the regeneraton effect to apply (in ticks).", 0, 10000)
                .getInt();

        regenRadius = config.get(cat, "Regen Radius", 5,
                "The maximum distance from the campfire players can be to still receive the effect.", 0, 100)
                .getInt();

        regenWait = config.get(cat, "Regen Wait Time", 900,
                "The time between each application of regeneration (in ticks). Varies a little bit above and below the given value.", 0, 10000)
                .getInt();

        autoRecipe = config.get(cat, "Automatic Recipe Discovery", true,
                "If true, campfires will look through every single furnace recipe to find ones that result in an ItemFood, and add those to its list.")
                .getBoolean();

        autoBlacklistStrings = config.get(cat, "Automatic Recipe Discovery Blacklist", new String[] {},
                "Prevents Automatic Recipe Discovery from adding furnace recipes that use these inputs to the recipe list. It's pattern validated. "
                        + "Format is modid:name or modid:name:meta. If meta is not given, it defaults to 0.",
                blacklistPat)
                .getStringList();

        recipeList = config.get(cat, "Custom Recipes", defaultRecipeList,
                "The list of custom recipes that work in the campfire. It's pattern validated. "
                        + "Each part is separated by a /. "
                        + "First is the input, which can be either modid:name or modid:name:meta. "
                        + "If meta is not given, it defaults to 0. "
                        + "Next is the output, which is the same as input, but you can add @number to the end to change the size of the output stack. "
                        + "If @number is not given, it defaults to 1. "
                        + "The last part is the cooking time, which is optional. It's the number of ticks until the item cooks. "
                        + "If cooking time is not given, it defaults to 600. "
                        + "Error messages will be printed in console if a recipe is invalid due to the given item not existing.",
                recipePat)
                .getStringList();

        allowAutomation = config.get(cat, "Allow Automation", true,
                "If true, hoppers and other forms of automation will be able to insert items into campfires from the sides.")
                .getBoolean();

        startUnlit = config.get(cat, "Unlit is Default", false,
                "If true, the unlit campfire is the default one, instead of the lit one. Requires restart to change recipes.")
                .setRequiresMcRestart(true).getBoolean();

        maintainState = config.get(cat, "Maintain Lit/Unlit State", false,
                "If true, campfires will remember the state they're in when broken and drop in the corresponding item form. "
                        + "As in, breaking a lit campfire always drops a lit campfire, and breaking an unlit campfire always drops an unlit campfire, "
                        + "regardless of the setting of Unlit is Default.")
                .getBoolean();

        silkRequired = config.get(cat, "Silk Touch Required", true,
                "If false, campfires won't need Silk Touch to drop themselves when broken.")
                .getBoolean();

        printCampfireRecipes = config.get(cat, "#Debug: Print Campfire Recipes", false,
                "If true, prints the final list of campfire recipes on config (re)load. Use this to make sure everything worked.")
                .getBoolean();

        //

        // startUnlit & charcoalOnly
        GameRegistry.addRecipe(
                new ShapedOreRecipe(new ItemStack(startUnlit ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire),
                        " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 1), 'C', "logWood"));

        if (!charcoalOnly)
            GameRegistry.addRecipe(
                    new ShapedOreRecipe(new ItemStack(startUnlit ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire),
                            " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 0), 'C', "logWood"));

        // recipeList
        ArrayList<CampfireRecipe> theList = CampfireRecipe.getRecipeList();
        theList.clear();

        for (String recipe : recipeList)
        {
            CampfireRecipe.addToRecipeList(recipe);
        }

        // autoRecipe & autoBlacklistStrings
        if (autoRecipe)
        {
            autoBlacklistStacks.clear();
            for (String input : autoBlacklistStrings)
            {
                ItemStack stack = CampfireRecipe.parseItemStack(input);
                if (stack != null)
                    autoBlacklistStacks.add(stack);
            }

            if (autoRecipeList.isEmpty() || !autoBlacklistStacks.isEmpty())
            {
                autoRecipeList.clear();

                Iterator inputsit = ((Collection) FurnaceRecipes.smelting().getSmeltingList().keySet()).iterator();
                Collection results = (Collection) FurnaceRecipes.smelting().getSmeltingList().values();
                Iterator resultsit = results.iterator();

                iteratorLoop: for (int i = 0; i < results.size(); ++i)
                {
                    ItemStack inputstack = (ItemStack) inputsit.next();
                    ItemStack resultstack = (ItemStack) resultsit.next();

                    inputstack.setItemDamage(inputstack.getItemDamage() % 32767);
                    resultstack.setItemDamage(resultstack.getItemDamage() % 32767);

                    Object[] frecipe = { inputstack, resultstack, 600 };

                    if (((ItemStack) frecipe[1]).getItem() instanceof ItemFood)
                    {
                        for (ItemStack bstack : autoBlacklistStacks)
                        {
                            if (ItemStack.areItemStacksEqual(inputstack, bstack))
                                continue iteratorLoop;
                        }

                        for (CampfireRecipe crecipe : theList)
                        {
                            if (ItemStack.areItemStacksEqual(crecipe.getInput(), (ItemStack) frecipe[0])
                                    && ItemStack.areItemStacksEqual(crecipe.getOutput(), (ItemStack) frecipe[1]))
                                continue iteratorLoop;
                        }

                        autoRecipeList.add(new CampfireRecipe(frecipe));
                    }

                }
            }
            theList.addAll(autoRecipeList);

        }

        // printCampfireRecipes
        if (printCampfireRecipes)
        {
            CommonProxy.modlog.info("Campfire Recipes: ");
            for (CampfireRecipe recipe : theList)
                CommonProxy.modlog.info(recipe);
        }
    }

}
