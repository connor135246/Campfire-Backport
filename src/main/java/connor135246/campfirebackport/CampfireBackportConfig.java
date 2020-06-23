package connor135246.campfirebackport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportConfig
{
    // finals
    public static final String NEITHER = EnumCampfireType.NEITHER.toString();
    public static final String REG_ONLY = EnumCampfireType.REG_ONLY.toString();
    public static final String SOUL_ONLY = EnumCampfireType.SOUL_ONLY.toString();
    public static final String BOTH = EnumCampfireType.BOTH.toString();

    public static final String[] regOrSoulSettings = new String[] { NEITHER, REG_ONLY, SOUL_ONLY, BOTH };

    public static final String[] defaultRecipeList = new String[] { "minecraft:porkchop/minecraft:cooked_porkchop", "minecraft:beef/minecraft:cooked_beef",
            "minecraft:chicken/minecraft:cooked_chicken", "minecraft:potato/minecraft:baked_potato",
            "minecraft:fish:0/minecraft:cooked_fished:0", "minecraft:fish:1/minecraft:cooked_fished:1" };

    public static final String customRecipesExplanation = "It's made of three sections, each separated by a /. "
            + "First is the input, which can be modid:name:meta@number or ore:oreName@number. @number should be between 1 and 4. "
            + "If :meta is not given, any meta is accepted. If @number is not given, it defaults to 1. "
            + "Next is the output, which can be modid:name:meta@number. "
            + "If :meta is not given, it defaults to 0. If @number is not given, it defaults to 1. "
            + "The last part is the cooking time, which is optional. For inputs that are more than one item, all items must have reached this cooking time. "
            + "If cooking time is not given, it defaults to 600. "
            + "Check the console for error messages in case an item wasn't found. "
            + "Note: recipes are given a priority based on their settings. Recipes created by Auto Recipe Discovery are lowest, followed by ore dictionary recipes, followed by recipes that specify an input meta, followed by recipes that specify an input size greater than 1. "
            + "Check the order of your recipes by setting #Debug: Print Campfire Recipes to true. ";

    // internal settings
    private static boolean initialLoad = true;

    // config settings
    public static boolean charcoalOnly;

    public static EnumCampfireType regenCampfires;
    public static int[] regularRegen;
    public static int[] soulRegen;

    public static EnumCampfireType autoRecipe;
    public static String[] autoBlacklistStrings;

    public static String[] regularRecipeList;
    public static String[] soulRecipeList;
    public static String recipeListInheritance;

    public static EnumCampfireType automation;

    public static EnumCampfireType startUnlit;
    public static EnumCampfireType rememberState;
    public static EnumCampfireType silkNeeded;

    public static EnumCampfireType putOutByRain;

    public static String[] signalFireStrings;

    public static boolean dispenserBehaviours;
    public static String[] dispenserBehavioursBlacklistStrings;
    public static String[] dispenserBehavioursWhitelist;

    public static boolean printCampfireRecipes;
    public static boolean printDispenserBehaviours;
    public static boolean suppressInputErrors;

    // lists made from config settings
    public static ArrayList<Item> dispenserBehavioursBlacklistItems = new ArrayList<Item>();

    public static ArrayList<CampfireRecipe> autoRecipeList = new ArrayList<CampfireRecipe>();

    public static HashMap<Item, Integer> autoBlacklistStacks = new HashMap<Item, Integer>();
    public static ArrayList<String> autoBlacklistOres = new ArrayList<String>();

    public static HashMap<Block, Integer> signalFireBlocks = new HashMap<Block, Integer>();
    public static ArrayList<String> signalFireOres = new ArrayList<String>();

    public static void doConfig(Configuration config)
    {
        getConfig(config);
        setConfig();
    }

    public static void getConfig(Configuration config)
    {
        String cat = Configuration.CATEGORY_GENERAL;

        Pattern recipePat = Pattern.compile("(((?!^ore:)(\\w+:\\w+)(:\\d+)?(@\\d+)?)|((^ore:)(\\w+)(@\\d+)?))\\/(\\w+:\\w+)(:\\d+)?(@\\d+)?(\\/\\d+)?");
        Pattern itemMetaOrePat = Pattern.compile("((?!^ore:)(\\w+:\\w+)(:\\d+)?)|(^ore:\\w+)");
        Pattern itemPat = Pattern.compile("(\\w+:\\w+)");
        Pattern behavPat = Pattern.compile("(\\w+:\\w+)/(shovel|sword)");

        charcoalOnly = config.get(cat, "Charcoal Only", false,
                "If true, regular campfires can be crafted only using charcoal. "
                        + "In vanilla either coal or charcoal can be used, but breaking campfires always drops charcoal. "
                        + "If for some reason you don't want players to be able to turn coal into charcoal, turn this on.")
                .setRequiresMcRestart(true).getBoolean();

        regenCampfires = EnumCampfireType.campfireCheck.get(config.get(cat, "Regeneration Campfires", NEITHER,
                "Lit campfires of this type will periodically apply a regeneration effect to nearby players.",
                regOrSoulSettings).getString());

        regularRegen = config.get(cat, "Regeneration Settings (Regular Campfires)", new int[] { 0, 50, 5, 900 },
                "First value is regen level, from 0 to 31.\n"
                        + "Second value is the timer on the regen effect to apply (in ticks).\n"
                        + "Third value is the radius from the campfire, from 0 to 100.\n"
                        + "Fourth value is the time between each application of regeneration (in ticks). Varies a little bit around this value.",
                0, 10000, true, 4).getIntList();

        regularRegen[0] = MathHelper.clamp_int(regularRegen[0], 0, 31);
        regularRegen[2] = MathHelper.clamp_int(regularRegen[2], 0, 100);

        soulRegen = config.get(cat, "Regeneration Settings (Soul Campfires)", new int[] { 1, 50, 10, 750 },
                "First value is regen level, from 0 to 31.\n"
                        + "Second value is the timer on the regen effect to apply (in ticks).\n"
                        + "Third value is the radius from the campfire, from 0 to 100.\n"
                        + "Fourth value is the time between each application of regeneration (in ticks). Varies a little bit around this value.",
                0, 10000, true, 4).getIntList();

        soulRegen[0] = MathHelper.clamp_int(soulRegen[0], 0, 31);
        soulRegen[2] = MathHelper.clamp_int(soulRegen[2], 0, 100);

        autoRecipe = EnumCampfireType.campfireCheck.get(config.get(cat, "Auto Recipe Discovery", BOTH,
                "Campfires of this type will look through every single furnace recipe to find ones that result in an ItemFood, and add those to its list.",
                regOrSoulSettings).getString());

        autoBlacklistStrings = config.get(cat, "Auto Recipe Discovery Blacklist", new String[] {},
                "Prevents Auto Recipe Discovery from adding furnace recipes that use these inputs to the recipe list. It's pattern validated. "
                        + "Format is ore:oreName or modid:name or modid:name:meta. If meta is not given, all metas of the item are blocked.",
                itemMetaOrePat)
                .getStringList();

        regularRecipeList = config.get(cat, "Custom Recipes (Regular)", defaultRecipeList,
                "The list of custom recipes for the regular campfire. It's pattern validated. " + customRecipesExplanation,
                recipePat)
                .getStringList();

        soulRecipeList = config.get(cat, "Custom Recipes (Soul)", new String[] {},
                "The list of custom recipes for the soul campfire. It's pattern validated. " + customRecipesExplanation,
                recipePat)
                .getStringList();

        recipeListInheritance = config.get(cat, "Custom Recipe Inheritance", "soul inherits regular",
                "Allows you to make campfires of one type inherit custom recipes from the other type. "
                        + "That way you don't have to type them all out twice.",
                new String[] { "soul inherits regular", "regular inherits soul", "no inheritance" }).getString();

        automation = EnumCampfireType.campfireCheck.get(config.get(cat, "Automation", BOTH,
                "Hoppers and other forms of automation will be able to insert items into these types of campfires from the sides.",
                regOrSoulSettings).getString());

        startUnlit = EnumCampfireType.campfireCheck.get(config.get(cat, "Unlit by Default", NEITHER,
                "The unlit version of this type of campfire is the default one, instead of the lit one. Requires restart to change recipes.",
                regOrSoulSettings).setRequiresMcRestart(true).getString());

        rememberState = EnumCampfireType.campfireCheck.get(config.get(cat, "Remember Lit/Unlit State", NEITHER,
                "Campfires of this type will remember the state they're in when broken and drop in the corresponding item form. "
                        + "As in, breaking a lit campfire always drops a lit campfire, and breaking an unlit campfire always drops an unlit campfire, "
                        + "regardless of the setting of Unlit by Default.",
                regOrSoulSettings).getString());

        silkNeeded = EnumCampfireType.campfireCheck.get(config.get(cat, "Silk Touch Needed", BOTH,
                "Campfires of this type need Silk Touch to drop themselves when broken.",
                regOrSoulSettings).getString());

        putOutByRain = EnumCampfireType.campfireCheck.get(config.get(cat, "Put Out by Rain", NEITHER,
                "Campfires of this type will be put out by rain. It's rather slow...",
                regOrSoulSettings).getString());

        signalFireStrings = config.get(cat, "Signal Fire Blocks", new String[] { "minecraft:hay_block" },
                "The list of blocks that, when placed below a campfire, make its particles go higher. It's pattern validated. "
                        + "Format is ore:oreName or modid:name or modid:name:meta. If meta is not given, all metas of the block work.",
                itemMetaOrePat)
                .getStringList();

        dispenserBehaviours = config.get(cat, "Dispenser Behaviours", true,
                "If true, dispensers will have added behaviours for shovels (putting out campfires) and swords (lighting campfires if enchanted with Fire Aspect). "
                        + "Note that this is accomplished by searching through every ItemSpade and ItemSword and adding those to the dispenser behaviour registry. "
                        + "If another mod adds dispenser behaviours to its tools and this causes that behaviour to be overwritten, add the tool to the Dispenser Behaviour Blacklist.")
                .setRequiresMcRestart(true).getBoolean();

        dispenserBehavioursBlacklistStrings = config
                .get(cat, "Dispenser Behaviours Blacklist", new String[] { "minecraft:wooden_shovel", "minecraft:wooden_sword" },
                        "Prevents these items from receiving dispenser behaviours when Dispenser Behaviours is true. It's pattern validated. "
                                + "Format is modid:name.",
                        itemPat)
                .setRequiresMcRestart(true).getStringList();

        dispenserBehavioursWhitelist = config.get(cat, "Dispenser Behaviours Whitelist", new String[] {},
                "Adds dispenser behaviours to these items when Dispenser Behaviours is true. Overwrites existing behaviours. "
                        + "It's pattern validated. Format is modid:name/shovel or modid:name/sword. "
                        + "Note that the sword behaviour requires the item to be enchanted with Fire Aspect.",
                behavPat)
                .setRequiresMcRestart(true)
                .getStringList();

        printCampfireRecipes = config.get(cat, "#Debug: Print Campfire Recipes", false,
                "If true, prints the final list of all campfire recipes on config (re)load. Use this to make sure everything worked.")
                .getBoolean();

        printDispenserBehaviours = config.get(cat, "#Debug: Print Dispenser Behaviours", false,
                "If true, prints each item that was given a dispenser behaviour when launching. Use this to make sure everything worked.")
                .setRequiresMcRestart(true).getBoolean();

        suppressInputErrors = config.get(cat, "#Debug: Suppress Input Error Warnings", false,
                "If true, warnings about invalid inputs for Custom Recipes, Auto Recipe Discovery Blacklist, Signal Fire Blocks, Dispenser Behaviours Blacklist, and Dispenser Behaviours Whitelist won't print to console.")
                .getBoolean();
    }

    public static void setConfig()
    {
        // startUnlit & charcoalOnly
        if (initialLoad)
        {
            GameRegistry.addRecipe(
                    new ShapedOreRecipe(
                            new ItemStack(
                                    startUnlit.matches(EnumCampfireType.REGULAR) ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire),
                            " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 1), 'C', "logWood"));

            if (!charcoalOnly)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(
                                new ItemStack(
                                        startUnlit.matches(EnumCampfireType.REGULAR) ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 0), 'C', "logWood"));
            
            GameRegistry.addRecipe(
                    new ShapedOreRecipe(
                            new ItemStack(
                                    startUnlit.matches(EnumCampfireType.SOUL) ? CampfireBackportBlocks.soul_campfire_base
                                            : CampfireBackportBlocks.soul_campfire),
                            " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Blocks.soul_sand, 1), 'C', "logWood"));            
        }

        // regularRecipeList & soulRecipeList & recipeListInheritance
        ArrayList<CampfireRecipe> masterList = CampfireRecipe.getMasterList();
        masterList.clear();

        if (printCampfireRecipes)
            CommonProxy.modlog.info("Parsing custom recipes...");

        if (recipeListInheritance.equals("soul inherits regular"))
        {
            for (String recipe : regularRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.BOTH);
            for (String recipe : soulRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.SOUL_ONLY);
        }
        else if (recipeListInheritance.equals("regular inherits soul"))
        {
            for (String recipe : regularRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.REG_ONLY);
            for (String recipe : soulRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.BOTH);
        }
        else
        {
            for (String recipe : regularRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.REG_ONLY);
            for (String recipe : soulRecipeList)
                CampfireRecipe.addToRecipeList(recipe, EnumCampfireType.SOUL_ONLY);
        }

        // autoRecipe & autoBlacklistStrings
        if (autoRecipe != EnumCampfireType.NEITHER)
        {
            if (printCampfireRecipes && autoBlacklistStrings.length != 0)
                CommonProxy.modlog.info("Parsing Auto Recipe Discovery Blacklist...");

            autoBlacklistStacks.clear();
            autoBlacklistOres.clear();
            for (String input : autoBlacklistStrings)
            {
                Object[] output = StringParsers.parseItemStackOrOre(input, 1);
                if (output[0] != null)
                {
                    if (output[0] instanceof String)
                        autoBlacklistOres.add((String) output[0]);
                    else
                        autoBlacklistStacks.put((Item) output[0], (Integer) output[2]);
                }
                else if (!suppressInputErrors)
                    CommonProxy.modlog.warn("Auto Recipe Discovery Blacklist entry " + input + " was invalid!");
            }

            autoRecipeList.clear();

            if (printCampfireRecipes)
                CommonProxy.modlog.info("Discovering furnace recipes...");

            Iterator inputsit = ((Collection) FurnaceRecipes.smelting().getSmeltingList().keySet()).iterator();
            Iterator resultsit = ((Collection) FurnaceRecipes.smelting().getSmeltingList().values()).iterator();

            iteratorLoop: while (resultsit.hasNext())
            {
                ItemStack inputstack = (ItemStack) inputsit.next();
                ItemStack resultstack = (ItemStack) resultsit.next();

                if (resultstack.getItem() instanceof ItemFood)
                {
                    if (autoBlacklistStacks.get(inputstack.getItem()) != null)
                    {
                        if (autoBlacklistStacks.get(inputstack.getItem()) == -1 || autoBlacklistStacks.get(inputstack.getItem()) == inputstack.getItemDamage())
                            continue iteratorLoop;
                    }
                    else
                    {
                        for (int id : OreDictionary.getOreIDs(inputstack))
                        {
                            if (autoBlacklistOres.contains(OreDictionary.getOreName(id)))
                                continue iteratorLoop;
                        }
                    }
                    autoRecipeList.add(new CampfireRecipe(inputstack, resultstack, 600, autoRecipe));
                }
            }

            if (printCampfireRecipes)
                CommonProxy.modlog.info("Adding discovered furnace recipes to recipe list...");

            for (CampfireRecipe foundCrecipe : autoRecipeList)
            {
                boolean addIt = true;
                for (CampfireRecipe masterCrecipe : masterList)
                {
                    if (!masterCrecipe.isOreDictRecipe())
                    {
                        if (CampfireRecipe.doStackRecipesMatch(foundCrecipe, masterCrecipe))
                        {
                            addIt = false;
                            break;
                        }
                    }
                }
                if (addIt)
                    masterList.add(foundCrecipe);
            }
        }

        if (printCampfireRecipes)
            CommonProxy.modlog.info("Sorting final list of recipes...");

        ArrayList<CampfireRecipe> regList = CampfireRecipe.getRecipeList(EnumCampfireType.REGULAR);
        ArrayList<CampfireRecipe> soulList = CampfireRecipe.getRecipeList(EnumCampfireType.SOUL);
        regList.clear();
        soulList.clear();

        for (CampfireRecipe masterCrecipe : masterList)
        {
            if (masterCrecipe.getTypes().matches(EnumCampfireType.REGULAR))
                regList.add(masterCrecipe);
            if (masterCrecipe.getTypes().matches(EnumCampfireType.SOUL))
                soulList.add(masterCrecipe);
        }

        Collections.sort(masterList);
        Collections.sort(regList);
        Collections.sort(soulList);

        // signalFireStrings
        signalFireBlocks.clear();
        signalFireOres.clear();
        for (String input : signalFireStrings)
        {
            Object[] output = StringParsers.parseBlockOrOre(input, 1);
            if (output[0] != null)
            {
                if (output[0] instanceof String)
                    signalFireOres.add((String) output[0]);
                else
                    signalFireBlocks.put((Block) output[0], (Integer) output[2]);
            }
            else if (!suppressInputErrors)
                CommonProxy.modlog.warn("Signal Fire Blocks entry " + input + " was invalid!");
        }

        // dispenserBehavioursBlacklistStrings
        if (dispenserBehavioursBlacklistItems.isEmpty())
        {
            for (String input : dispenserBehavioursBlacklistStrings)
            {
                Item item = (Item) StringParsers.parseItemAndMaybeMeta(input, 1)[0];
                if (item != null)
                    dispenserBehavioursBlacklistItems.add(item);
                else if (!suppressInputErrors)
                    CommonProxy.modlog.warn("Dispenser Behaviours Blacklist entry " + input + " was invalid!");
            }
        }

        // printCampfireRecipes
        if (printCampfireRecipes)
        {
            CommonProxy.modlog.info("-Regular Campfire Recipes: ");
            for (CampfireRecipe crecipe : regList)
                CommonProxy.modlog.info(crecipe);
            CommonProxy.modlog.info("---");

            CommonProxy.modlog.info("-Soul Campfire Recipes: ");
            for (CampfireRecipe crecipe : soulList)
                CommonProxy.modlog.info(crecipe);
            CommonProxy.modlog.info("---");
        }

        initialLoad = false;
    }

    public static void doDefaultConfig()
    {
        charcoalOnly = false;

        regenCampfires = EnumCampfireType.NEITHER;
        regularRegen = new int[] { 0, 50, 5, 900 };
        soulRegen = new int[] { 1, 50, 10, 700 };

        autoRecipe = EnumCampfireType.BOTH;
        autoBlacklistStrings = new String[] {};

        regularRecipeList = defaultRecipeList;
        soulRecipeList = new String[] {};
        recipeListInheritance = "soul inherits regular";

        automation = EnumCampfireType.BOTH;

        startUnlit = EnumCampfireType.NEITHER;
        rememberState = EnumCampfireType.NEITHER;
        silkNeeded = EnumCampfireType.BOTH;

        putOutByRain = EnumCampfireType.NEITHER;

        dispenserBehaviours = true;
        dispenserBehavioursBlacklistStrings = new String[] {};
        dispenserBehavioursWhitelist = new String[] {};

        printCampfireRecipes = false;
        printDispenserBehaviours = false;
        suppressInputErrors = false;

        initialLoad = false;

        setConfig();
    }

}
