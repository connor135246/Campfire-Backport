package connor135246.campfirebackport.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.common.crafting.CampfireStateChanger;
import connor135246.campfirebackport.common.crafting.GenericCustomInput;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CampfireBackportConfig
{

    public static boolean initialLoad = true;

    // config settings
    public static boolean charcoalOnly;
    public static boolean soulSoilOnly;

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

    public static int[] burnOutTimer;
    public static EnumCampfireType signalFiresBurnOut;
    public static double[] burnToNothingChances;

    public static String[] signalFireStrings;

    public static String[] campfireDropsStrings;

    public static EnumCampfireType colourfulSmoke;

    public static String[] dispenserBlacklistStrings;

    public static String[] regularExtinguishersList;
    public static String[] soulExtinguishersList;
    public static String extinguishersListInheritance;
    public static String[] regularIgnitorsList;
    public static String[] soulIgnitorsList;
    public static String ignitorsListInheritance;

    public static boolean printCustomRecipes;
    public static boolean suppressInputErrors;

    // lists made from config settings
    public static ArrayList<Item> dispenserBlacklistItems = new ArrayList<Item>();

    public static ArrayList<CampfireRecipe> autoRecipeList = new ArrayList<CampfireRecipe>();

    public static Map<Item, Integer> autoBlacklistStacks = new HashMap<Item, Integer>();
    public static Set<Integer> autoBlacklistOres = new HashSet<Integer>();

    public static Map<Block, Integer> signalFireBlocks = new HashMap<Block, Integer>();
    public static Set<Integer> signalFireOres = new HashSet<Integer>();

    public static ItemStack[] campfireDropsStacks = new ItemStack[2];

    public static void doConfig(Configuration config)
    {
        getConfig(config);
        setConfig();
    }

    public static void getConfig(Configuration config)
    {
        // TODO delete property???
        // "Dispenser Behaviours"
        // "Dispenser Behaviours Whitelist"
        // "#Debug: Print Dispenser Behaviours"
        config.setCategoryPropertyOrder(Configuration.CATEGORY_GENERAL, ConfigReference.configOrder);

        // Getting
        charcoalOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.charcoalOnly, false,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "charcoal"))
                .setRequiresMcRestart(true).getBoolean();

        soulSoilOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulSoilOnly, false,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "soul_soil"))
                .setRequiresMcRestart(true).getBoolean();

        regenCampfires = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regenCampfires, ConfigReference.NEITHER,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "regen"),
                ConfigReference.regOrSoulSettings).getString());

        regularRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regularRegen, ConfigReference.defaultRegRegen,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "regen_settings"),
                0, 10000, true, 4).getIntList();

        regularRegen[0] = MathHelper.clamp_int(regularRegen[0], 0, 31);
        regularRegen[2] = MathHelper.clamp_int(regularRegen[2], 0, 100);

        soulRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulRegen, ConfigReference.defaultSoulRegen,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "regen_settings"),
                0, 10000, true, 4).getIntList();

        soulRegen[0] = MathHelper.clamp_int(soulRegen[0], 0, 31);
        soulRegen[2] = MathHelper.clamp_int(soulRegen[2], 0, 100);

        autoRecipe = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.autoRecipe, ConfigReference.BOTH,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "auto"),
                ConfigReference.regOrSoulSettings).getString());

        autoBlacklistStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.autoBlacklistStrings, new String[] {},
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "auto_blacklist"),
                StringParsers.itemMetaOrePat)
                .getStringList();

        regularRecipeList = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regularRecipeList, ConfigReference.defaultRecipeList,
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "recipes", ConfigReference.regular, Reference.README_FILENAME),
                StringParsers.recipePat)
                .getStringList();

        soulRecipeList = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulRecipeList, new String[] {},
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "recipes", ConfigReference.soul, Reference.README_FILENAME),
                StringParsers.recipePat)
                .getStringList();

        recipeListInheritance = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.recipeListInheritance, ConfigReference.SOUL_GETS_REG,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "recipes_inheritance"),
                ConfigReference.inheritanceSettings).getString();

        automation = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.automation, ConfigReference.BOTH,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "automation"),
                ConfigReference.regOrSoulSettings).getString());

        startUnlit = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.startUnlit, ConfigReference.NEITHER,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "default_unlit"),
                ConfigReference.regOrSoulSettings).setRequiresMcRestart(true).getString());

        rememberState = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.rememberState, ConfigReference.NEITHER,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "remember_state"),
                ConfigReference.regOrSoulSettings).getString());

        silkNeeded = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.silkNeeded, ConfigReference.BOTH,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "silk"),
                ConfigReference.regOrSoulSettings).getString());

        putOutByRain = EnumCampfireType.campfireCheck.get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.putOutByRain, ConfigReference.NEITHER,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "rained_out"),
                ConfigReference.regOrSoulSettings).getString());

        burnOutTimer = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnOutTimer, ConfigReference.defaultBurnOuts,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "burn_out_timer"),
                -1, Integer.MAX_VALUE, true, 2).getIntList();

        signalFiresBurnOut = EnumCampfireType.campfireCheck
                .get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.signalFiresBurnOut, ConfigReference.NEITHER,
                        StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "signals_burn_out"),
                        ConfigReference.regOrSoulSettings).getString());

        burnToNothingChances = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnToNothingChances, ConfigReference.defaultBurnToNothingChances,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "burn_to_nothing"),
                0.0, 1.0, true, 2).getDoubleList();

        signalFireStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.signalFireStrings, ConfigReference.defaultSignalFireBlocks,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "signal_fire_blocks"),
                StringParsers.itemMetaOrePat)
                .getStringList();

        campfireDropsStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.campfireDropsStrings, ConfigReference.defaultCampfireDrops,
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "campfire_drops", Reference.README_FILENAME),
                true, 2, StringParsers.itemMetaAnySimpleDataSizeOREmptyPat)
                .getStringList();

        colourfulSmoke = EnumCampfireType.campfireCheck
                .get(config.get(Configuration.CATEGORY_GENERAL, ConfigReference.colourfulSmoke, ConfigReference.NEITHER,
                        StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "colourful_smoke"),
                        ConfigReference.regOrSoulSettings).getString());

        dispenserBlacklistStrings = config
                .get(Configuration.CATEGORY_GENERAL, ConfigReference.dispenserBlacklistStrings, new String[] {},
                        StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "dispenser_blacklist"),
                        StringParsers.itemPat)
                .setRequiresMcRestart(true).getStringList();

        regularExtinguishersList = config
                .get(Configuration.CATEGORY_GENERAL, ConfigReference.regularExtinguishersList, ConfigReference.defaultExtinguishersList,
                        StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "state_changers", ConfigReference.extinguisher,
                                ConfigReference.regular, Reference.README_FILENAME),
                        StringParsers.stateChangePat)
                .getStringList();

        soulExtinguishersList = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulExtinguishersList, new String[] {},
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "state_changers", ConfigReference.extinguisher,
                        ConfigReference.soul,
                        Reference.README_FILENAME),
                StringParsers.stateChangePat)
                .getStringList();

        extinguishersListInheritance = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.extinguishersListInheritance, ConfigReference.SOUL_GETS_REG,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "extinguishers_inheritance"),
                ConfigReference.inheritanceSettings).getString();

        regularIgnitorsList = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regularIgnitorsList, ConfigReference.defaultIgnitorsList,
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "state_changers", ConfigReference.ignitor, ConfigReference.regular,
                        Reference.README_FILENAME),
                StringParsers.stateChangePat)
                .getStringList();

        soulIgnitorsList = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulIgnitorsList, new String[] {},
                StatCollector.translateToLocalFormatted(ConfigReference.TRANSLATE_PREFIX + "state_changers", ConfigReference.ignitor, ConfigReference.soul,
                        Reference.README_FILENAME),
                StringParsers.stateChangePat)
                .getStringList();

        ignitorsListInheritance = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.ignitorsListInheritance, ConfigReference.SOUL_GETS_REG,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "ignitors_inheritance"),
                ConfigReference.inheritanceSettings).getString();

        printCustomRecipes = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.printCustomRecipes, false,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "print_recipes"))
                .getBoolean();

        suppressInputErrors = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.suppressInputErrors, false,
                StatCollector.translateToLocal(ConfigReference.TRANSLATE_PREFIX + "suppress_errors"))
                .getBoolean();
    }

    public static void setConfig()
    {
        // startUnlit & charcoalOnly & soulSoilOnly
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

            Block soulSoil = GameData.getBlockRegistry().getObject("netherlicious:SoulSoil");

            if (soulSoil != Blocks.air)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(
                                new ItemStack(
                                        startUnlit.matches(EnumCampfireType.SOUL) ? CampfireBackportBlocks.soul_campfire_base
                                                : CampfireBackportBlocks.soul_campfire),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(soulSoil), 'C', "logWood"));

            if (soulSoil == Blocks.air || !soulSoilOnly)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(
                                new ItemStack(
                                        startUnlit.matches(EnumCampfireType.SOUL) ? CampfireBackportBlocks.soul_campfire_base
                                                : CampfireBackportBlocks.soul_campfire),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Blocks.soul_sand), 'C', "logWood"));
        }

        // regularRecipeList & soulRecipeList & recipeListInheritance
        ArrayList<CampfireRecipe> masterRecipeList = CampfireRecipe.getMasterList();
        masterRecipeList.clear();

        if (printCustomRecipes)
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.parsing_recipes"));

        for (String recipe : regularRecipeList)
            CampfireRecipe.addToMasterList(recipe,
                    recipeListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY);
        for (String recipe : soulRecipeList)
            CampfireRecipe.addToMasterList(recipe,
                    recipeListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY);

        // autoRecipe & autoBlacklistStrings
        if (autoRecipe != EnumCampfireType.NEITHER)
        {
            if (printCustomRecipes && autoBlacklistStrings.length != 0)
                CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.parsing_auto_blacklist"));

            autoBlacklistStacks.clear();
            autoBlacklistOres.clear();
            for (String input : autoBlacklistStrings)
            {
                try
                {
                    Object[] output = StringParsers.parseItemOrOreOrToolOrClass(input, 1, new NBTTagCompound(), true);
                    if (output[0] == null)
                        throw new Exception();
                    else if (output[0] instanceof Integer)
                        autoBlacklistOres.add((Integer) output[0]);
                    else
                        autoBlacklistStacks.put((Item) output[0], (Integer) output[2]);
                }
                catch (Exception excep)
                {
                    if (!suppressInputErrors)
                        CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_auto_blacklist", input));
                }
            }

            autoRecipeList.clear();

            if (printCustomRecipes)
                CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.discovering_autos"));

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
                        if (autoBlacklistStacks.get(inputstack.getItem()) == OreDictionary.WILDCARD_VALUE
                                || autoBlacklistStacks.get(inputstack.getItem()) == inputstack.getItemDamage())
                            continue iteratorLoop;
                    }
                    else
                    {
                        for (int id : OreDictionary.getOreIDs(inputstack))
                        {
                            if (autoBlacklistOres.contains(id))
                                continue iteratorLoop;
                        }
                    }
                    CampfireRecipe furnaceRecipe = new CampfireRecipe(inputstack, resultstack, autoRecipe);
                    if (furnaceRecipe.getInput() != null && furnaceRecipe.getOutput() != null)
                        autoRecipeList.add(new CampfireRecipe(inputstack, resultstack, autoRecipe));
                }
            }

            if (printCustomRecipes)
                CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.adding_autos"));

            for (CampfireRecipe foundCrecipe : autoRecipeList)
            {
                boolean addIt = true;
                for (CampfireRecipe masterCrecipe : masterRecipeList)
                {
                    if (masterCrecipe.isItemInput() && GenericCustomInput.doStackRecipesMatch(foundCrecipe, masterCrecipe))
                    {
                        addIt = false;
                        break;
                    }
                }
                if (addIt)
                    masterRecipeList.add(foundCrecipe);
            }
        }

        if (printCustomRecipes)
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.sorting_recipes"));

        ArrayList<CampfireRecipe> regRecipeList = CampfireRecipe.getRecipeList(EnumCampfireType.REGULAR);
        ArrayList<CampfireRecipe> soulRecipeList = CampfireRecipe.getRecipeList(EnumCampfireType.SOUL);
        regRecipeList.clear();
        soulRecipeList.clear();

        for (CampfireRecipe masterCrecipe : masterRecipeList)
        {
            if (masterCrecipe.getTypes().matches(EnumCampfireType.REGULAR))
                regRecipeList.add(masterCrecipe);
            if (masterCrecipe.getTypes().matches(EnumCampfireType.SOUL))
                soulRecipeList.add(masterCrecipe);
        }

        Collections.sort(masterRecipeList);
        Collections.sort(regRecipeList);
        Collections.sort(soulRecipeList);

        // dispenserBlacklistStrings
        if (printCustomRecipes)
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.parsing_dispenser_blacklist"));

        if (dispenserBlacklistItems.isEmpty())
        {
            for (String input : dispenserBlacklistStrings)
            {
                try
                {
                    Item item = (Item) StringParsers.parseItemAndMaybeMeta(input, 1, new NBTTagCompound(), true)[0];
                    if (item == null)
                        throw new Exception();
                    dispenserBlacklistItems.add(item);
                }
                catch (Exception excep)
                {
                    if (!suppressInputErrors)
                        CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_dispenser_blacklist", input));
                }
            }
        }

        // regularIgnitors & soulIgnitors & ignitorsInheritance & regularExtinguishers & soulExtinguishers & extinguishersInheritance
        ArrayList<CampfireStateChanger> masterStateChangerList = CampfireStateChanger.getMasterList();
        masterStateChangerList.clear();

        if (printCustomRecipes)
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.parsing_state_changers"));

        for (String recipe : regularExtinguishersList)
            CampfireStateChanger.addToMasterList(recipe,
                    extinguishersListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY,
                    true);
        for (String recipe : soulExtinguishersList)
            CampfireStateChanger.addToMasterList(recipe,
                    extinguishersListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, true);

        for (String recipe : regularIgnitorsList)
            CampfireStateChanger.addToMasterList(recipe,
                    ignitorsListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY,
                    false);
        for (String recipe : soulIgnitorsList)
            CampfireStateChanger.addToMasterList(recipe,
                    ignitorsListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY,
                    false);

        if (printCustomRecipes)
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.sorting_state_changers"));

        ArrayList<CampfireStateChanger> leftStateChangerList = CampfireStateChanger.getStateChangerList(true);
        ArrayList<CampfireStateChanger> rightStateChangerList = CampfireStateChanger.getStateChangerList(false);
        leftStateChangerList.clear();
        rightStateChangerList.clear();

        for (CampfireStateChanger masterCstate : masterStateChangerList)
        {
            if (masterCstate.isLeftClick())
                leftStateChangerList.add(masterCstate);
            else
                rightStateChangerList.add(masterCstate);
        }

        Collections.sort(masterStateChangerList);
        Collections.sort(leftStateChangerList);
        Collections.sort(rightStateChangerList);

        // signalFireStrings
        signalFireBlocks.clear();
        signalFireOres.clear();
        for (String input : signalFireStrings)
        {
            try
            {
                Object[] output = StringParsers.parseBlockOrOre(input, true);
                if (output[0] == null)
                    throw new Exception();
                else if (output[0] instanceof Integer)
                    signalFireOres.add((Integer) output[0]);
                else
                    signalFireBlocks.put((Block) output[0], (Integer) output[2]);
            }
            catch (Exception excep)
            {
                if (!suppressInputErrors)
                    CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_signal_fire_block", input));
            }
        }

        // campfireDropsStrings
        campfireDropsStacks[0] = ConfigReference.defaultRegDrop.copy();
        campfireDropsStacks[1] = ConfigReference.defaultSoulDrop.copy();

        for (int i = 0; i < campfireDropsStacks.length; ++i)
        {
            try
            {
                if (!campfireDropsStrings[i].isEmpty())
                {
                    Object[] output = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(campfireDropsStrings[i], false);
                    if (output[0] == null)
                        throw new Exception();
                    ItemStack drop = new ItemStack((Item) output[0], (Integer) output[1], (Integer) output[2]);
                    if (!((NBTTagCompound) output[3]).hasNoTags())
                        drop.setTagCompound((NBTTagCompound) output[3]);
                    campfireDropsStacks[i] = drop;
                }
            }
            catch (Exception excep)
            {
                if (!suppressInputErrors)
                    CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_drops", campfireDropsStrings[i]));
            }
        }

        // printCampfireRecipes
        if (printCustomRecipes)
        {
            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.print_recipes.reg"));
            for (CampfireRecipe crecipe : regRecipeList)
                CommonProxy.modlog.info(crecipe);
            CommonProxy.modlog.info("---");

            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.print_recipes.soul"));
            for (CampfireRecipe crecipe : soulRecipeList)
                CommonProxy.modlog.info(crecipe);
            CommonProxy.modlog.info("---");

            CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".info.print_state_changers"));
            for (CampfireStateChanger cstate : masterStateChangerList)
                CommonProxy.modlog.info(cstate);
            CommonProxy.modlog.info("---");
        }

        initialLoad = false;
    }

    public static void doDefaultConfig()
    {
        charcoalOnly = false;
        soulSoilOnly = false;

        regenCampfires = EnumCampfireType.NEITHER;
        regularRegen = ConfigReference.defaultRegRegen;
        soulRegen = ConfigReference.defaultSoulRegen;

        autoRecipe = EnumCampfireType.BOTH;
        autoBlacklistStrings = new String[] {};

        regularRecipeList = ConfigReference.defaultRecipeList;
        soulRecipeList = new String[] {};
        recipeListInheritance = ConfigReference.SOUL_GETS_REG;

        automation = EnumCampfireType.BOTH;

        startUnlit = EnumCampfireType.NEITHER;
        rememberState = EnumCampfireType.NEITHER;
        silkNeeded = EnumCampfireType.BOTH;

        putOutByRain = EnumCampfireType.NEITHER;

        burnOutTimer = ConfigReference.defaultBurnOuts;
        signalFiresBurnOut = EnumCampfireType.NEITHER;
        burnToNothingChances = ConfigReference.defaultBurnToNothingChances;

        signalFireStrings = ConfigReference.defaultSignalFireBlocks;

        campfireDropsStrings = ConfigReference.defaultCampfireDrops;

        colourfulSmoke = EnumCampfireType.NEITHER;

        dispenserBlacklistStrings = new String[] {};

        regularExtinguishersList = ConfigReference.defaultExtinguishersList;
        soulExtinguishersList = new String[] {};
        extinguishersListInheritance = ConfigReference.SOUL_GETS_REG;
        regularIgnitorsList = ConfigReference.defaultIgnitorsList;
        soulIgnitorsList = new String[] {};
        ignitorsListInheritance = ConfigReference.SOUL_GETS_REG;

        printCustomRecipes = false;
        suppressInputErrors = false;

        setConfig();
    }

}
