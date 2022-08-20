package connor135246.campfirebackport.config;

import java.io.File;
import java.io.IOException;
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
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
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

    public static boolean useDefaults = false;
    public static boolean initialLoad = true;

    // config settings

    public static boolean charcoalOnly;
    public static boolean soulSoilOnly;

    public static boolean renderItem3D;

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
    public static EnumCampfireType burnOutOnRespawn;

    public static EnumCampfireType automation;

    public static EnumCampfireType startUnlit;
    public static EnumCampfireType rememberState;
    public static EnumCampfireType silkNeeded;

    public static EnumCampfireType putOutByRain;
    public static EnumCampfireType worksUnderwater;

    public static EnumCampfireType damaging;

    public static double[] visCosts;

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
    public static Set<Integer> autoBlacklistOres = new HashSet<Integer>();

    public static Map<Block, Set<Integer>> signalFireBlocks = new LinkedHashMap<Block, Set<Integer>>();
    public static Set<Integer> signalFireOres = new LinkedHashSet<Integer>();

    public static ItemStack[] campfireDropsStacks = new ItemStack[2];

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
     * @param stopConsoleSpam
     *            - if true, nothing will be printed to console while setting values, regardless of config settings
     */
    public static void doConfig(int flag, boolean stopConsoleSpam)
    {
        if ((flag & 1) != 0 && !useDefaults)
            config.load();

        if ((flag & 2) != 0)
            getConfig();

        if ((flag & 4) != 0)
        {
            if (stopConsoleSpam)
            {
                boolean tempPrint = printCustomRecipes;
                boolean tempSuppress = suppressInputErrors;

                printCustomRecipes = false;
                suppressInputErrors = true;

                setConfig();

                printCustomRecipes = tempPrint;
                suppressInputErrors = tempSuppress;
            }
            else
                setConfig();
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
                StringParsers.translateComment("render_3d")).getBoolean();

        charcoalOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.charcoalOnly, false,
                StringParsers.translateComment("charcoal")).setRequiresMcRestart(true).getBoolean();

        soulSoilOnly = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulSoilOnly, false,
                StringParsers.translateComment("soul_soil")).setRequiresMcRestart(true).getBoolean();

        regenCampfires = enumFromConfig(ConfigReference.regenCampfires, ConfigReference.NEITHER, "regen");

        regularRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.regularRegen, ConfigReference.defaultRegRegen,
                StringParsers.translateComment("regen_settings"), 0, 10000, true, 4).getIntList();

        soulRegen = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.soulRegen, ConfigReference.defaultSoulRegen,
                StringParsers.translateComment("regen_settings"), 0, 10000, true, 4).getIntList();

        autoRecipe = enumFromConfig(ConfigReference.autoRecipe, ConfigReference.BOTH, "auto");

        autoBlacklistStrings = listFromConfig(ConfigReference.autoBlacklistStrings, ConfigReference.empty,
                StringParsers.itemMetaOrePat, "auto_blacklist");

        regularRecipeList = listFromConfig(ConfigReference.regularRecipeList, ConfigReference.defaultRecipeList,
                StringParsers.recipePat, "recipes", ConfigReference.regular());

        soulRecipeList = listFromConfig(ConfigReference.soulRecipeList, ConfigReference.empty,
                StringParsers.recipePat, "recipes", ConfigReference.soul());

        recipeListInheritance = inheritanceFromConfig(ConfigReference.recipeListInheritance, "recipes_inheritance");

        defaultCookingTimes = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.defaultCookingTimes, ConfigReference.defaultDefaultCookingTimes,
                StringParsers.translateComment("default_cooking_times"), 1, Integer.MAX_VALUE, true, 2).getIntList();

        spawnpointable = enumFromConfig(ConfigReference.spawnpointable, ConfigReference.NEITHER, "spawnpointable");

        burnOutOnRespawn = enumFromConfig(ConfigReference.burnOutOnRespawn, ConfigReference.NEITHER, "burn_out_on_respawn");

        automation = enumFromConfig(ConfigReference.automation, ConfigReference.BOTH, "automation");

        startUnlit = enumFromConfig(ConfigReference.startUnlit, ConfigReference.NEITHER, "default_unlit");

        rememberState = enumFromConfig(ConfigReference.rememberState, ConfigReference.NEITHER, "remember_state");

        silkNeeded = enumFromConfig(ConfigReference.silkNeeded, ConfigReference.BOTH, "silk");

        putOutByRain = enumFromConfig(ConfigReference.putOutByRain, ConfigReference.NEITHER, "rained_out");

        worksUnderwater = enumFromConfig(ConfigReference.worksUnderwater, ConfigReference.NEITHER, "works_underwater");

        damaging = enumFromConfig(ConfigReference.damaging, ConfigReference.BOTH, "damaging");

        visCosts = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.visCosts, ConfigReference.defaultVisCosts,
                StringParsers.translateComment("vis_costs"), 0.0, Double.MAX_VALUE, true, 4).getDoubleList();

        burnOutTimer = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnOutTimer, ConfigReference.defaultBurnOuts,
                StringParsers.translateComment("burn_out_timer"), -1, Integer.MAX_VALUE, true, 2).getIntList();

        burnOutRules = listFromConfig(ConfigReference.burnOutRules, ConfigReference.empty,
                StringParsers.burnOutRulesPat, "burn_out_rules");

        signalFiresBurnOut = enumFromConfig(ConfigReference.signalFiresBurnOut, ConfigReference.NEITHER, "signals_burn_out");

        burnToNothingChances = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.burnToNothingChances, ConfigReference.defaultBurnToNothingChances,
                StringParsers.translateComment("burn_to_nothing"), 0.0, 1.0, true, 2).getDoubleList();

        burnOutAsItem = enumFromConfig(ConfigReference.burnOutAsItem, ConfigReference.NEITHER, "burn_out_as_item");

        signalFireStrings = listFromConfig(ConfigReference.signalFireStrings, ConfigReference.defaultSignalFireBlocks,
                StringParsers.itemMetaOrePat, "signal_fire_blocks");

        campfireDropsStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.campfireDropsStrings, ConfigReference.defaultCampfireDrops,
                StringParsers.translateComment("campfire_drops"), true, 2, StringParsers.itemMetaAnySimpleDataSizeOREmptyPat)
                .getStringList();

        colourfulSmoke = enumFromConfig(ConfigReference.colourfulSmoke, ConfigReference.NEITHER, "colourful_smoke");

        dispenserBlacklistStrings = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.dispenserBlacklistStrings, ConfigReference.empty,
                StringParsers.translateComment("dispenser_blacklist"), StringParsers.itemPat).setRequiresMcRestart(true).getStringList();

        // due to recipe updates, the old default extinguisher "right/minecraft:water_bucket/stackable>minecraft:bucket" is no longer necessary.
        // also, it would end up giving the player back 2 buckets with the new system. so we remove it here.
        Property regularExtinguishersListProperty = config.get(Configuration.CATEGORY_GENERAL,
                ConfigReference.regularExtinguishersList, ConfigReference.defaultExtinguishersList,
                StringParsers.translateComment("state_changers", ConfigReference.extinguisher(), ConfigReference.regular()),
                StringParsers.stateChangePat);

        regularExtinguishersList = ArrayUtils.removeElement(regularExtinguishersListProperty.getStringList(),
                "right/minecraft:water_bucket/stackable>minecraft:bucket");

        regularExtinguishersListProperty.setValues(regularExtinguishersList);
        //

        soulExtinguishersList = listFromConfig(ConfigReference.soulExtinguishersList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers", ConfigReference.extinguisher(), ConfigReference.soul());

        extinguishersListInheritance = inheritanceFromConfig(ConfigReference.extinguishersListInheritance, "extinguishers_inheritance");

        regularIgnitorsList = listFromConfig(ConfigReference.regularIgnitorsList, ConfigReference.defaultIgnitorsList,
                StringParsers.stateChangePat, "state_changers", ConfigReference.ignitor(), ConfigReference.regular());

        soulIgnitorsList = listFromConfig(ConfigReference.soulIgnitorsList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers", ConfigReference.ignitor(), ConfigReference.soul());

        ignitorsListInheritance = inheritanceFromConfig(ConfigReference.ignitorsListInheritance, "ignitors_inheritance");

        printCustomRecipes = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.printCustomRecipes, false,
                StringParsers.translateComment("print_recipes")).getBoolean();

        suppressInputErrors = config.get(Configuration.CATEGORY_GENERAL, ConfigReference.suppressInputErrors, false,
                StringParsers.translateComment("suppress_errors")).getBoolean();
    }

    private static EnumCampfireType enumFromConfig(String key, String defaultValue, String translationKey)
    {
        return Optional.ofNullable(EnumCampfireType.FROM_NAME.get(config.get(Configuration.CATEGORY_GENERAL, key, defaultValue,
                StringParsers.translateComment(translationKey), ConfigReference.enumSettings).getString()))
                .orElse(EnumCampfireType.FROM_NAME.get(defaultValue));
    }

    private static String inheritanceFromConfig(String key, String translationKey)
    {
        return config.get(Configuration.CATEGORY_GENERAL, key, ConfigReference.SOUL_GETS_REG, StringParsers.translateComment(translationKey),
                ConfigReference.inheritanceSettings).getString();
    }

    private static String[] listFromConfig(String key, String[] defaultList, Pattern pat, String translationKey, Object... translationArgs)
    {
        return config.get(Configuration.CATEGORY_GENERAL, key, defaultList, StringParsers.translateComment(translationKey, translationArgs), pat)
                .getStringList();
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

        // regularRecipeList & soulRecipeList & recipeListInheritance
        CampfireRecipe.clearRecipeLists();

        if (regularRecipeList.length != 0 || soulRecipeList.length != 0)
            ConfigReference.logInfo("parsing_recipes");

        boolean inheritanceBool = recipeListInheritance.equals(ConfigReference.SOUL_GETS_REG);
        for (String recipe : regularRecipeList)
            CampfireRecipe.addToRecipeLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY);

        inheritanceBool = recipeListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulRecipeList)
            CampfireRecipe.addToRecipeLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY);

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
                    if (output[0] instanceof Integer)
                        autoBlacklistOres.add((Integer) output[0]);
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

            addFurnaceRecipes();
        }

        for (CampfireRecipe crecipe : CampfireRecipe.getCraftTweakerList())
            CampfireRecipe.addToRecipeLists(crecipe);

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

        inheritanceBool = extinguishersListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulExtinguishersList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, true);

        inheritanceBool = ignitorsListInheritance.equals(ConfigReference.SOUL_GETS_REG);
        for (String recipe : regularIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY, false);

        inheritanceBool = ignitorsListInheritance.equals(ConfigReference.REG_GETS_SOUL);
        for (String recipe : soulIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe, inheritanceBool ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, false);

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
                if (output[0] instanceof Integer)
                    signalFireOres.add((Integer) output[0]);
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

        // burnOutTimer & burnOutRules
        BurnOutRule.setDefaultBurnOutRules(burnOutTimer[0], burnOutTimer[1]);

        if (burnOutRules.length != 0)
            ConfigReference.logInfo("parsing_burn_out_rules");

        BurnOutRule.clearRules();
        for (String input : burnOutRules)
            BurnOutRule.addToRules(input);

        for (BurnOutRule brule : BurnOutRule.getCraftTweakerRules())
            BurnOutRule.addToRules(brule);

        // campfireDropsStrings
        campfireDropsStacks[0] = ConfigReference.defaultRegDrop.copy();
        campfireDropsStacks[1] = ConfigReference.defaultSoulDrop.copy();

        ConfigReference.logInfo("parsing_drops");

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
     * It's also accessed from {@link connor135246.campfirebackport.common.compat.crafttweaker.CampfireBackportCraftTweaking.PostReloadEventHandler}.
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

            if (resultstack.getItem() instanceof ItemFood)
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
                        if (autoBlacklistOres.contains(id))
                            continue iteratorLoop;
                    }
                }

                CampfireRecipe furnaceCrecipe = CampfireRecipe.createAutoDiscoveryRecipe(inputstack, resultstack, autoRecipe);
                if (furnaceCrecipe != null && furnaceCrecipe.getInputs().length > 0 && furnaceCrecipe.getInputs()[0] != null && furnaceCrecipe.hasOutputs())
                {
                    boolean addIt = true;
                    for (CampfireRecipe masterCrecipe : CampfireRecipe.getMasterList())
                    {
                        if (CampfireRecipe.doStackRecipesMatch(furnaceCrecipe, masterCrecipe))
                        {
                            addIt = false;
                            break;
                        }
                    }
                    if (addIt)
                    {
                        for (CampfireRecipe splitCrecipe : CampfireRecipe.splitRecipeIfNecessary(furnaceCrecipe, null))
                        {
                            CampfireRecipe.getFurnaceList().add(splitCrecipe);
                            CampfireRecipe.addToRecipeLists(splitCrecipe);
                        }
                    }
                }
            }
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
        burnOutOnRespawn = EnumCampfireType.NEITHER;

        automation = EnumCampfireType.BOTH;

        startUnlit = EnumCampfireType.NEITHER;
        rememberState = EnumCampfireType.NEITHER;
        silkNeeded = EnumCampfireType.BOTH;

        putOutByRain = EnumCampfireType.NEITHER;
        worksUnderwater = EnumCampfireType.NEITHER;

        damaging = EnumCampfireType.BOTH;

        visCosts = ConfigReference.defaultVisCosts;

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
