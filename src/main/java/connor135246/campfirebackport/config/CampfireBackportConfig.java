package connor135246.campfirebackport.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.io.Files;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;

public class CampfireBackportConfig
{

    // config

    public static Configuration config;
    public static File configDirectory;

    public static final String CONFIGPREFIX = Reference.MODID + ".config.";

    public static boolean useDefaults = false;
    public static boolean initialLoad = true;

    // config settings

    public static boolean charcoalOnly;
    public static boolean soulSoilOnly;

    public static boolean renderItem3D;

    public static boolean showExtraCampfires;

    public static EnumCampfireType regenCampfires;
    public static int[] regularRegen;
    public static int[] soulRegen;

    public static EnumCampfireType autoRecipe;
    public static String[] autoBlacklistStrings;

    public static String[] regularRecipeList;
    public static String[] soulRecipeList;
    public static String recipeListInheritance;
    public static int[] defaultCookingTimes;

    public static EnumCampfireType spawnpointable;
    public static boolean spawnpointableAltTriggerObj;
    public static EnumCampfireType burnOutOnRespawn;

    public static EnumCampfireType automation;

    public static EnumCampfireType startUnlit;
    public static EnumCampfireType rememberState;
    public static EnumCampfireType silkNeeded;

    public static EnumCampfireType putOutByRain;
    public static EnumCampfireType worksUnderwater;

    public static EnumCampfireType damaging;

    public static double[] visCostsObj;

    public static int[] burnOutTimer;
    public static String[] burnOutRules;
    public static EnumCampfireType signalFiresBurnOut;
    public static double[] burnToNothingChances;
    public static EnumCampfireType burnOutAsItem;

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

    static
    {
        getConfigDefaults();
    }

    // lists made from config settings

    public static Set<Item> dispenserBlacklistItems = new HashSet<Item>();

    public static Map<Item, Set<Integer>> autoBlacklistStacks = new HashMap<Item, Set<Integer>>();
    public static Set<String> autoBlacklistOres = new HashSet<String>();

    public static boolean spawnpointableAltTrigger = false;

    public static Map<Block, Set<Integer>> signalFireBlocks = new LinkedHashMap<Block, Set<Integer>>();
    public static Set<String> signalFireOres = new LinkedHashSet<String>();

    public static double[] visCosts = new double[4];

    public static ItemStack[] campfireDropsStacks = new ItemStack[4];

    public static Set<String> possiblyInvalidOres = new HashSet<String>();

    // doing config things!

    /**
     * makes config file
     */
    public static void prepareConfig(FMLPreInitializationEvent event)
    {
        config = new Configuration(event.getSuggestedConfigurationFile());
        configDirectory = event.getModConfigurationDirectory();

        // old (pre-1.4) config is very different due to no soul campfires. old configs will be safely renamed and a new config will be created.
        if (config.hasKey(Configuration.CATEGORY_GENERAL, "Regen Level"))
        {
            try
            {
                CommonProxy.modlog.info(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config"));
                Files.move(config.getConfigFile(), new File(config.getConfigFile().getCanonicalPath() + "_1.3"));
                config = new Configuration(event.getSuggestedConfigurationFile());
            }
            catch (IOException excep)
            {
                CommonProxy.modlog.error(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error.0"));
                CommonProxy.modlog.error(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error.1"));
                CommonProxy.modlog.error(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error.2"));
                useDefaults = true;
            }
        }

        // delete old config explanation file
        File explanation = new File(configDirectory, Reference.MODID + ".readme1.6.cfg");
        if (explanation.exists())
            explanation.delete();
    }

    /**
     * Handles loading/saving config settings. <br>
     * Flags: <br>
     * 1 - Loads Configuration object from file. <br>
     * 2 - Gets settings from Configuration object. <br>
     * 4 - Applies settings. <br>
     * 8 - Saves Configuration object to file. <br>
     * Flags can be added together.
     * 
     * @param flag
     * @param stopPrinting
     *            - if true, info won't be printed to console, regardless of {@link #printCustomRecipes}
     * @param stopErrors
     *            - if true, errors won't be printed to console, regardless of {@link #suppressInputErrors}
     */
    public static void doConfig(int flag, boolean stopPrinting, boolean stopErrors)
    {
        if ((flag & 1) != 0 && !useDefaults)
            config.load();

        if ((flag & 2) != 0)
            getConfig();

        if ((flag & 4) != 0)
        {
            boolean tempPrint = printCustomRecipes;
            boolean tempSuppress = suppressInputErrors;

            printCustomRecipes = tempPrint && !stopPrinting;
            suppressInputErrors = tempSuppress || stopErrors;

            setConfig();

            printCustomRecipes = tempPrint;
            suppressInputErrors = tempSuppress;
        }

        if ((flag & 8) != 0 && !useDefaults)
            config.save();
    }

    /**
     * grabs settings from Configuration object
     */
    private static void getConfig()
    {
        if (useDefaults)
        {
            getConfigDefaults();
            return;
        }

        config.setCategoryPropertyOrder(Configuration.CATEGORY_GENERAL, ConfigReference.configOrder);

        renderItem3D = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.renderItem3D, false,
                StringParsers.translateTooltip("render_3d")).setLanguageKey(CONFIGPREFIX + "render_3d").getBoolean();

        showExtraCampfires = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.showExtraCampfires, false,
                StringParsers.translateTooltip("show_extra_campfires")).setLanguageKey(CONFIGPREFIX + "show_extra_campfires").setRequiresMcRestart(true)
                .getBoolean();

        charcoalOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.charcoalOnly, false,
                StringParsers.translateTooltip("charcoal")).setLanguageKey(CONFIGPREFIX + "charcoal").setRequiresMcRestart(true).getBoolean();

        // rename "Soul Soil Only (Netherlicious)" to "Soul Soil Only"
        boolean soulSoilOnlyDefault = false;
        if (config.getCategory(Configuration.CATEGORY_GENERAL).containsKey(ConfigReference.soulSoilOnly_OLD))
        {
            soulSoilOnlyDefault = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulSoilOnly_OLD, false).getBoolean();
            config.getCategory(Configuration.CATEGORY_GENERAL).remove(ConfigReference.soulSoilOnly_OLD);
        }
        soulSoilOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulSoilOnly, soulSoilOnlyDefault,
                StringParsers.translateTooltip("soul_soil")).setLanguageKey(CONFIGPREFIX + "soul_soil").setRequiresMcRestart(true).getBoolean();

        regenCampfires = enumFromConfig(ConfigReference.regenCampfires, ConfigReference.NEITHER, "regen");

        regularRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regularRegen, ConfigReference.defaultRegRegen,
                StringParsers.translateTooltip("regen_settings.reg"), 0, 10000, true, 4).setLanguageKey(CONFIGPREFIX + "regen_settings.reg").getIntList();

        soulRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulRegen, ConfigReference.defaultSoulRegen,
                StringParsers.translateTooltip("regen_settings.soul"), 0, 10000, true, 4).setLanguageKey(CONFIGPREFIX + "regen_settings.soul").getIntList();

        autoRecipe = enumFromConfig(ConfigReference.autoRecipe, ConfigReference.BOTH, "auto");

        autoBlacklistStrings = listFromConfig(ConfigReference.autoBlacklistStrings, ConfigReference.empty,
                StringParsers.itemMetaOrePat, "auto_blacklist");

        regularRecipeList = listFromConfig(ConfigReference.regularRecipeList, ConfigReference.defaultRecipeList,
                StringParsers.recipePat, "recipes.reg");

        soulRecipeList = listFromConfig(ConfigReference.soulRecipeList, ConfigReference.empty,
                StringParsers.recipePat, "recipes.soul");

        recipeListInheritance = inheritanceFromConfig(ConfigReference.recipeListInheritance, "recipes_inheritance");

        defaultCookingTimes = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.defaultCookingTimes, ConfigReference.defaultDefaultCookingTimes,
                StringParsers.translateTooltip("default_cooking_times"), 1, Integer.MAX_VALUE, true, 2).setLanguageKey(CONFIGPREFIX + "default_cooking_times")
                .getIntList();

        spawnpointable = enumFromConfig(ConfigReference.spawnpointable, ConfigReference.NEITHER, "spawnpointable");

        spawnpointableAltTriggerObj = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.spawnpointableAltTriggerObj, false,
                StringParsers.translateTooltip("spawnpointable_alt_trigger")).setLanguageKey(CONFIGPREFIX + "spawnpointable_alt_trigger").getBoolean();

        burnOutOnRespawn = enumFromConfig(ConfigReference.burnOutOnRespawn, ConfigReference.NEITHER, "burn_out_on_respawn");

        automation = enumFromConfig(ConfigReference.automation, ConfigReference.BOTH, "automation");

        startUnlit = enumFromConfig(ConfigReference.startUnlit, ConfigReference.NEITHER, "default_unlit");

        rememberState = enumFromConfig(ConfigReference.rememberState, ConfigReference.NEITHER, "remember_state");

        silkNeeded = enumFromConfig(ConfigReference.silkNeeded, ConfigReference.BOTH, "silk");

        putOutByRain = enumFromConfig(ConfigReference.putOutByRain, ConfigReference.NEITHER, "rained_out");

        worksUnderwater = enumFromConfig(ConfigReference.worksUnderwater, ConfigReference.NEITHER, "works_underwater");

        damaging = enumFromConfig(ConfigReference.damaging, ConfigReference.BOTH, "damaging");

        visCostsObj = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.visCostsObj, ConfigReference.defaultVisCosts,
                StringParsers.translateTooltip("vis_costs"), 0.0, Double.MAX_VALUE, true, 4).setLanguageKey(CONFIGPREFIX + "vis_costs").getDoubleList();

        burnOutTimer = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnOutTimer, ConfigReference.defaultBurnOuts,
                StringParsers.translateTooltip("burn_out_timer"), -1, Integer.MAX_VALUE, true, 2).setLanguageKey(CONFIGPREFIX + "burn_out_timer").getIntList();

        burnOutRules = listFromConfig(ConfigReference.burnOutRules, ConfigReference.empty,
                StringParsers.burnOutRulesPat, "burn_out_rules");

        signalFiresBurnOut = enumFromConfig(ConfigReference.signalFiresBurnOut, ConfigReference.NEITHER, "signals_burn_out");

        burnToNothingChances = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnToNothingChances, ConfigReference.defaultBurnToNothingChances,
                StringParsers.translateTooltip("burn_to_nothing"), 0.0, 1.0, true, 2).setLanguageKey(CONFIGPREFIX + "burn_to_nothing").getDoubleList();

        burnOutAsItem = enumFromConfig(ConfigReference.burnOutAsItem, ConfigReference.NEITHER, "burn_out_as_item");

        signalFireStrings = listFromConfig(ConfigReference.signalFireStrings, ConfigReference.defaultSignalFireBlocks,
                StringParsers.itemMetaOrePat, "signal_fire_blocks");

        campfireDropsStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.campfireDropsStrings, ConfigReference.defaultCampfireDrops,
                StringParsers.translateTooltip("campfire_drops"), true, 2, StringParsers.itemMetaAnySimpleDataSizeOREmptyPat)
                .setLanguageKey(CONFIGPREFIX + "campfire_drops").getStringList();

        colourfulSmoke = enumFromConfig(ConfigReference.colourfulSmoke, ConfigReference.NEITHER, "colourful_smoke");

        dispenserBlacklistStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.dispenserBlacklistStrings, ConfigReference.empty,
                StringParsers.translateTooltip("dispenser_blacklist"), StringParsers.itemPat).setLanguageKey(CONFIGPREFIX + "dispenser_blacklist")
                .setRequiresMcRestart(true).getStringList();

        // due to recipe updates, the old default extinguisher "right/minecraft:water_bucket/stackable>minecraft:bucket" is no longer necessary.
        // also, it would end up giving the player back 2 buckets with the new system. so we remove it here.
        Property regularExtinguishersListProperty = config.get(Configuration.CATEGORY_GENERAL,
                ConfigReference.regularExtinguishersList, ConfigReference.defaultExtinguishersList,
                StringParsers.translateTooltip("state_changers.extinguisher.reg"),
                StringParsers.stateChangePat).setLanguageKey(CONFIGPREFIX + "state_changers.extinguisher.reg");

        regularExtinguishersList = ArrayUtils.removeElement(regularExtinguishersListProperty.getStringList(),
                "right/minecraft:water_bucket/stackable>minecraft:bucket");

        regularExtinguishersListProperty.setValues(regularExtinguishersList);
        //

        soulExtinguishersList = listFromConfig(ConfigReference.soulExtinguishersList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers.extinguisher.soul");

        extinguishersListInheritance = inheritanceFromConfig(ConfigReference.extinguishersListInheritance, "extinguishers_inheritance");

        regularIgnitorsList = listFromConfig(ConfigReference.regularIgnitorsList, ConfigReference.defaultIgnitorsList,
                StringParsers.stateChangePat, "state_changers.ignitor.reg");

        soulIgnitorsList = listFromConfig(ConfigReference.soulIgnitorsList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers.ignitor.soul");

        ignitorsListInheritance = inheritanceFromConfig(ConfigReference.ignitorsListInheritance, "ignitors_inheritance");

        printCustomRecipes = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.printCustomRecipes, false,
                StringParsers.translateTooltip("print_recipes")).setLanguageKey(CONFIGPREFIX + "print_recipes").getBoolean();

        suppressInputErrors = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.suppressInputErrors, false,
                StringParsers.translateTooltip("suppress_errors")).setLanguageKey(CONFIGPREFIX + "suppress_errors").getBoolean();
    }

    private static EnumCampfireType enumFromConfig(String key, String defaultValue, String translationKey)
    {
        return Optional.ofNullable(EnumCampfireType.FROM_NAME.get(config.get(Configuration.CATEGORY_GENERAL, key, defaultValue,
                StringParsers.translateTooltip(translationKey), ConfigReference.enumSettings).setLanguageKey(CONFIGPREFIX + translationKey).getString()))
                .orElse(EnumCampfireType.FROM_NAME.get(defaultValue));
    }

    private static String inheritanceFromConfig(String key, String translationKey)
    {
        return config.get(Configuration.CATEGORY_GENERAL, key, ConfigReference.SOUL_GETS_REG, StringParsers.translateTooltip(translationKey),
                ConfigReference.inheritanceSettings).setLanguageKey(CONFIGPREFIX + translationKey).getString();
    }

    private static String[] listFromConfig(String key, String[] defaultList, Pattern pat, String translationKey)
    {
        return config.get(Configuration.CATEGORY_GENERAL, key, defaultList, StringParsers.translateTooltip(translationKey), pat)
                .setLanguageKey(CONFIGPREFIX + translationKey).getStringList();
    }

    /**
     * sets up recipe lists, etc from config settings
     */
    private static void setConfig()
    {
        ConfigReference.logInfo("setting_config");

        // regularRegen & soulRegen
        regularRegen[0] = MathHelper.clamp_int(regularRegen[0], 0, 31);
        regularRegen[2] = MathHelper.clamp_int(regularRegen[2], 0, 100);

        soulRegen[0] = MathHelper.clamp_int(soulRegen[0], 0, 31);
        soulRegen[2] = MathHelper.clamp_int(soulRegen[2], 0, 100);

        // spawnpointableAltTriggerObj
        spawnpointableAltTrigger = spawnpointableAltTriggerObj;

        // visCostsObj
        visCosts = Arrays.copyOf(visCostsObj, visCostsObj.length);

        // regularRecipeList & soulRecipeList & recipeListInheritance
        CampfireRecipe.clearRecipeLists();

        if (regularRecipeList.length != 0 || soulRecipeList.length != 0)
            ConfigReference.logInfo("parsing_recipes");

        possiblyInvalidOres.clear();

        boolean inheritanceBool = recipeListInheritance.equals(ConfigReference.SOUL_GETS_REG);
        for (String recipe : regularRecipeList)
            CampfireRecipe.addToRecipeLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY);

        clearInvalidOresNoCT();

        inheritanceBool = recipeListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulRecipeList)
            CampfireRecipe.addToRecipeLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY);

        clearInvalidOresNoCT();

        // add crafttweaker recipes before auto recipes, so that auto recipe can check crafttweaker eclipsing
        for (CampfireRecipe crecipe : CampfireRecipe.getCraftTweakerList())
            CampfireRecipe.addToRecipeLists(crecipe);

        // autoRecipe & autoBlacklistStrings
        CampfireRecipe.getFurnaceList().clear();

        if (autoRecipe != EnumCampfireType.NEITHER)
        {
            if (autoBlacklistStrings.length != 0)
                ConfigReference.logInfo("parsing_auto_blacklist");

            autoBlacklistStacks.clear();
            autoBlacklistOres.clear();
            for (String input : autoBlacklistStrings)
            {
                try
                {
                    Object[] output = StringParsers.parseItemOrOreOrToolOrClass(input, 1, new NBTTagCompound(), true);
                    if (output[0] instanceof String && ((String) output[0]).startsWith("ore:"))
                        autoBlacklistOres.add(((String) output[0]).substring(4));
                    else if (output[0] instanceof Item)
                    {
                        if (!autoBlacklistStacks.containsKey((Item) output[0]))
                            autoBlacklistStacks.put((Item) output[0], new HashSet<Integer>());

                        Set<Integer> metas = autoBlacklistStacks.get((Item) output[0]);

                        if (!metas.contains(OreDictionary.WILDCARD_VALUE))
                        {
                            if (((Integer) output[2]) == OreDictionary.WILDCARD_VALUE)
                                metas.clear();
                            metas.add((Integer) output[2]);
                        }
                    }
                    else
                        throw new Exception();
                }
                catch (Exception excep)
                {
                    ConfigReference.logError("invalid_auto_blacklist", input);
                }
            }

            clearInvalidOresNoCT();

            addFurnaceRecipes();
        }

        CampfireRecipe.sortRecipeLists();

        // dispenserBlacklistStrings
        if (dispenserBlacklistStrings.length != 0)
            ConfigReference.logInfo("parsing_dispenser_blacklist");

        dispenserBlacklistItems.clear();
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
                ConfigReference.logError("invalid_dispenser_blacklist", input);
            }
        }

        // regularExtinguishers & soulExtinguishers & extinguishersInheritance & regularIgnitors & soulIgnitors & ignitorsInheritance
        CampfireStateChanger.clearStateChangerLists();

        if (regularExtinguishersList.length != 0 || regularIgnitorsList.length != 0 || soulExtinguishersList.length != 0 || soulIgnitorsList.length != 0)
            ConfigReference.logInfo("parsing_state_changers");

        inheritanceBool = extinguishersListInheritance.equals(ConfigReference.SOUL_GETS_REG);
        for (String recipe : regularExtinguishersList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY, true);

        clearInvalidOresNoCT();

        inheritanceBool = extinguishersListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulExtinguishersList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, true);

        clearInvalidOresNoCT();

        inheritanceBool = ignitorsListInheritance.equals(ConfigReference.SOUL_GETS_REG);
        for (String recipe : regularIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY, false);

        clearInvalidOresNoCT();

        inheritanceBool = ignitorsListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, false);

        clearInvalidOresNoCT();

        for (CampfireStateChanger cstate : CampfireStateChanger.getCraftTweakerList())
            CampfireStateChanger.addToStateChangerLists(cstate);

        CampfireStateChanger.sortStateChangerLists();

        // signalFireStrings
        signalFireBlocks.clear();
        signalFireOres.clear();

        if (signalFireStrings.length != 0)
            ConfigReference.logInfo("parsing_signal_blocks");

        for (String input : signalFireStrings)
        {
            try
            {
                Object[] output = StringParsers.parseBlockOrOre(input, true);
                if (output[0] instanceof String && ((String) output[0]).startsWith("ore:"))
                    signalFireOres.add(((String) output[0]).substring(4));
                else if (output[0] instanceof Block)
                {
                    Block block = (Block) output[0];
                    if (!signalFireBlocks.containsKey(block))
                        signalFireBlocks.put(block, new HashSet<Integer>());

                    Set<Integer> metas = signalFireBlocks.get(block);

                    if (!metas.contains(OreDictionary.WILDCARD_VALUE))
                    {
                        int meta = (Integer) output[2];
                        if (meta == OreDictionary.WILDCARD_VALUE)
                            metas.clear();
                        else if (meta < 0 || meta > 15)
                        {
                            ConfigReference.logError("invalid_block_meta", meta);
                            throw new Exception();
                        }
                        metas.add(meta);
                    }
                }
                else
                    throw new Exception();
            }
            catch (Exception excep)
            {
                ConfigReference.logError("invalid_signal_fire_block", input);
            }
        }

        clearInvalidOresNoCT();

        // burnOutTimer & burnOutRules
        BurnOutRule.setDefaultBurnOutRules(burnOutTimer[0], burnOutTimer[1]);

        if (burnOutRules.length != 0)
            ConfigReference.logInfo("parsing_burn_out_rules");

        BurnOutRule.clearRules();
        for (String input : burnOutRules)
            BurnOutRule.addToRules(input);

        for (BurnOutRule brule : BurnOutRule.getCraftTweakerRules())
            BurnOutRule.addToRules(brule);

        setCampfireDrops();

        ConfigReference.logInfo("done");

        // printCustomRecipes
        if (printCustomRecipes)
        {
            if (!CampfireRecipe.getMasterList().isEmpty())
            {
                ConfigReference.logInfo("print_recipes");
                for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
                    CommonProxy.modlog.info(crecipe);
                CommonProxy.modlog.info("---");
            }

            if (!CampfireStateChanger.getMasterList().isEmpty())
            {
                ConfigReference.logInfo("print_state_changers");
                for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
                    CommonProxy.modlog.info(cstate);
                CommonProxy.modlog.info("---");
            }

            if (!BurnOutRule.getRules().isEmpty())
            {
                ConfigReference.logInfo("print_burn_out_rules");
                for (BurnOutRule brule : BurnOutRule.getRules())
                    CommonProxy.modlog.info(brule);
                CommonProxy.modlog.info("---");
            }
        }

        initialLoad = false;
    }

    /**
     * Discovers furnace recipes that result in an ItemFood, and adds them to the campfire types that were specified by autoRecipe. <br>
     * It's also accessed from {@link connor135246.campfirebackport.common.compat.crafttweaker.CampfireBackportCraftTweaking.PostReloadEventHandler}. <br>
     * CraftTweaker can change furnace recipes and auto blacklist ores, so it's gotta be re-parsed.
     */
    public static void addFurnaceRecipes()
    {
        ConfigReference.logInfo("discovering_autos");

        Iterator inputsit = FurnaceRecipes.smelting().getSmeltingList().keySet().iterator();
        Iterator resultsit = FurnaceRecipes.smelting().getSmeltingList().values().iterator();

        iteratorLoop: while (resultsit.hasNext())
        {
            ItemStack inputstack = (ItemStack) inputsit.next();
            ItemStack resultstack = (ItemStack) resultsit.next();

            if (inputstack == null || resultstack == null)
                continue iteratorLoop;

            if (CampfireBackportCompat.allowAutoRecipe(inputstack, resultstack))
            {
                if (!autoBlacklistStacks.isEmpty())
                {
                    Set<Integer> metas = autoBlacklistStacks.get(inputstack.getItem());
                    if (metas != null && (inputstack.getItemDamage() == OreDictionary.WILDCARD_VALUE || metas.contains(OreDictionary.WILDCARD_VALUE)
                            || metas.contains(inputstack.getItemDamage())))
                        continue iteratorLoop;
                }

                if (!autoBlacklistOres.isEmpty())
                {
                    ItemStack oreStack = inputstack.getItemDamage() == OreDictionary.WILDCARD_VALUE ? new ItemStack(inputstack.getItem(), 1, 0) : inputstack;
                    for (int id : OreDictionary.getOreIDs(oreStack))
                    {
                        if (autoBlacklistOres.contains(OreDictionary.getOreName(id)))
                            continue iteratorLoop;
                    }
                }

                CampfireRecipe.addAutoDiscoveryRecipe(inputstack, resultstack, autoRecipe);
            }
        }
    }

    /**
     * Sets campfire drops to default and then parses config drops. <br>
     * It's also accessed from {@link connor135246.campfirebackport.common.compat.crafttweaker.CampfireBackportCraftTweaking.PostReloadEventHandler}. <br>
     * CraftTweaker can change ores and the default soul drop cares about ores, so it's gotta be re-parsed.
     */
    public static void setCampfireDrops()
    {
        // campfireDropsStrings
        campfireDropsStacks[0] = ConfigReference.getDefaultRegDrop();
        campfireDropsStacks[1] = ConfigReference.getDefaultSoulDrop(); // cares about ores!
        // not yet controlled by campfireDropsStrings. if you want to edit the drop, subscribe to BlockEvent.HarvestDropsEvent.
        campfireDropsStacks[2] = ConfigReference.getDefaultFoxfireDrop();
        campfireDropsStacks[3] = ConfigReference.getDefaultShadowDrop();

        ConfigReference.logInfo("parsing_drops");

        for (int i = 0; i < campfireDropsStrings.length; ++i)
        {
            try
            {
                if (!campfireDropsStrings[i].isEmpty())
                {
                    Object[] output = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(campfireDropsStrings[i], false);
                    if (output[0] == null)
                        throw new Exception();

                    ItemStack drop = new ItemStack((Item) output[0], (Integer) output[1], (Integer) output[2]);
                    if (output[3] != null && !((NBTTagCompound) output[3]).hasNoTags())
                    {
                        ((NBTTagCompound) output[3]).removeTag(StringParsers.KEY_GCIDataType);
                        drop.setTagCompound((NBTTagCompound) output[3]);
                    }

                    campfireDropsStacks[i] = drop;
                }
            }
            catch (Exception excep)
            {
                ConfigReference.logError("invalid_drops", campfireDropsStrings[i]);
            }
        }
    }

    /**
     * Does {@link #checkInvalidOres()} and then clears it, but only if CraftTweaker isn't loaded.
     */
    public static void clearInvalidOresNoCT()
    {
        if (!CampfireBackportCompat.isMineTweaker3Loaded)
        {
            checkInvalidOres();
            possiblyInvalidOres.clear();
        }
    }

    /**
     * Goes through {@link #possiblyInvalidOres} and logs any truly invalid ones.
     */
    public static void checkInvalidOres()
    {
        for (String ore : possiblyInvalidOres)
        {
            if (!OreDictionary.doesOreNameExist(ore))
                ConfigReference.logError("unknown_ore", ore);
            else if (OreDictionary.getOres(ore, false).isEmpty())
                ConfigReference.logError("empty_ore", ore);
        }
    }

    /**
     * sets settings to the default
     */
    private static void getConfigDefaults()
    {
        renderItem3D = false;

        charcoalOnly = false;
        soulSoilOnly = false;

        showExtraCampfires = false;

        regenCampfires = EnumCampfireType.NEITHER;
        regularRegen = ConfigReference.defaultRegRegen;
        soulRegen = ConfigReference.defaultSoulRegen;

        autoRecipe = EnumCampfireType.BOTH;
        autoBlacklistStrings = ConfigReference.empty;

        regularRecipeList = ConfigReference.defaultRecipeList;
        soulRecipeList = ConfigReference.empty;
        recipeListInheritance = ConfigReference.SOUL_GETS_REG;
        defaultCookingTimes = ConfigReference.defaultDefaultCookingTimes;

        spawnpointable = EnumCampfireType.NEITHER;
        spawnpointableAltTriggerObj = false;
        burnOutOnRespawn = EnumCampfireType.NEITHER;

        automation = EnumCampfireType.BOTH;

        startUnlit = EnumCampfireType.NEITHER;
        rememberState = EnumCampfireType.NEITHER;
        silkNeeded = EnumCampfireType.BOTH;

        putOutByRain = EnumCampfireType.NEITHER;
        worksUnderwater = EnumCampfireType.NEITHER;

        damaging = EnumCampfireType.BOTH;

        visCostsObj = ConfigReference.defaultVisCosts;

        burnOutTimer = ConfigReference.defaultBurnOuts;
        burnOutRules = ConfigReference.empty;
        signalFiresBurnOut = EnumCampfireType.NEITHER;
        burnToNothingChances = ConfigReference.defaultBurnToNothingChances;
        burnOutAsItem = EnumCampfireType.NEITHER;

        signalFireStrings = ConfigReference.defaultSignalFireBlocks;

        campfireDropsStrings = ConfigReference.defaultCampfireDrops;

        colourfulSmoke = EnumCampfireType.NEITHER;

        dispenserBlacklistStrings = ConfigReference.empty;

        regularExtinguishersList = ConfigReference.defaultExtinguishersList;
        soulExtinguishersList = ConfigReference.empty;
        extinguishersListInheritance = ConfigReference.SOUL_GETS_REG;
        regularIgnitorsList = ConfigReference.defaultIgnitorsList;
        soulIgnitorsList = ConfigReference.empty;
        ignitorsListInheritance = ConfigReference.SOUL_GETS_REG;

        printCustomRecipes = false;
        suppressInputErrors = false;
    }

}
