package connor135246.campfirebackport.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
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

    // config

    public static Configuration config;
    public static File configDirectory;

    public static boolean useDefaultConfig = false;
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

    public static EnumCampfireType spawnpointable;
    public static EnumCampfireType burnOutOnRespawn;

    public static EnumCampfireType automation;

    public static EnumCampfireType startUnlit;
    public static EnumCampfireType rememberState;
    public static EnumCampfireType silkNeeded;

    public static EnumCampfireType putOutByRain;

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

    // lists made from config settings

    public static Set<Item> dispenserBlacklistItems = new HashSet<Item>();

    public static Map<Item, Integer> autoBlacklistStacks = new HashMap<Item, Integer>();
    public static Set<Integer> autoBlacklistOres = new HashSet<Integer>();

    public static Map<Block, Integer> signalFireBlocks = new HashMap<Block, Integer>();
    public static Set<Integer> signalFireOres = new HashSet<Integer>();

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
                useDefaultConfig = true;
            }
        }

        // delete old config explanation file
        File explanation = new File(configDirectory, Reference.MODID + ".readme1.6.cfg");
        if (explanation.exists())
            explanation.delete();
    }

    /**
     * Handles loading/saving config settings.<br>
     * Mode:<br>
     * 0 - Configuration object is loaded from file, settings are taken from it, related values are set, and Configuration object is saved to file.<br>
     * Used for when the file has changed or is being created.<br>
     * 1 - Settings are taken from Configuration object, related values are set, and Configuration object is saved to file.<br>
     * Used for when the Configuration object has changed, but not the file. Ex: changes in config GUI.<br>
     * 2 - Related values are set.<br>
     * Used for when config settings have changed (but not the Configuration object), and related values must be updated (but not saved to file). Ex: receiving external server
     * config settings.
     * 
     * @param mode
     *            - 0, 1, or 2
     * @param stopConsoleSpam
     *            - if true, nothing will be printed to console while setting values, regardless of config settings
     */
    public static void doConfig(int mode, boolean stopConsoleSpam)
    {
        if (-1 < mode || mode < 3)
        {
            if (!useDefaultConfig)
            {
                if (mode == 0)
                    config.load();

                if (mode != 2)
                    getConfig();

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

                if (mode != 2)
                    config.save();
            }
            else
            {
                doDefaultConfig();
            }
        }
    }

    /**
     * grabs settings from Configuration object
     */
    private static void getConfig()
    {
        config.setCategoryPropertyOrder(Configuration.CATEGORY_GENERAL, ConfigReference.configOrder);

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
                StringParsers.recipePat, "recipes", ConfigReference.regular);

        soulRecipeList = listFromConfig(ConfigReference.soulRecipeList, ConfigReference.empty,
                StringParsers.recipePat, "recipes", ConfigReference.soul);

        recipeListInheritance = inheritanceFromConfig(ConfigReference.recipeListInheritance, "recipes_inheritance");

        spawnpointable = enumFromConfig(ConfigReference.spawnpointable, ConfigReference.NEITHER, "spawnpointable");

        burnOutOnRespawn = enumFromConfig(ConfigReference.burnOutOnRespawn, ConfigReference.NEITHER, "burn_out_on_respawn");

        automation = enumFromConfig(ConfigReference.automation, ConfigReference.BOTH, "automation");

        startUnlit = enumFromConfig(ConfigReference.startUnlit, ConfigReference.NEITHER, "default_unlit");

        rememberState = enumFromConfig(ConfigReference.rememberState, ConfigReference.NEITHER, "remember_state");

        silkNeeded = enumFromConfig(ConfigReference.silkNeeded, ConfigReference.BOTH, "silk");

        putOutByRain = enumFromConfig(ConfigReference.putOutByRain, ConfigReference.NEITHER, "rained_out");

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

        regularExtinguishersList = listFromConfig(ConfigReference.regularExtinguishersList, ConfigReference.defaultExtinguishersList,
                StringParsers.stateChangePat, "state_changers", ConfigReference.extinguisher, ConfigReference.regular);

        soulExtinguishersList = listFromConfig(ConfigReference.soulExtinguishersList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers", ConfigReference.extinguisher, ConfigReference.soul);

        extinguishersListInheritance = inheritanceFromConfig(ConfigReference.extinguishersListInheritance, "extinguishers_inheritance");

        regularIgnitorsList = listFromConfig(ConfigReference.regularIgnitorsList, ConfigReference.defaultIgnitorsList,
                StringParsers.stateChangePat, "state_changers", ConfigReference.ignitor, ConfigReference.regular);

        soulIgnitorsList = listFromConfig(ConfigReference.soulIgnitorsList, ConfigReference.empty,
                StringParsers.stateChangePat, "state_changers", ConfigReference.ignitor, ConfigReference.soul);

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

        // startUnlit & charcoalOnly & soulSoilOnly
        if (initialLoad)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(
                    new ItemStack(CampfireBackportBlocks.getBlockFromLitAndType(!startUnlit.acceptsRegular(), EnumCampfireType.regular)),
                    " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 1), 'C', "logWood"));

            if (!charcoalOnly)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(
                                new ItemStack(CampfireBackportBlocks.getBlockFromLitAndType(!startUnlit.acceptsRegular(), EnumCampfireType.regular)),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Items.coal, 1, 0), 'C', "logWood"));

            Block soulSoil = GameData.getBlockRegistry().getObject("netherlicious:SoulSoil");

            if (soulSoil != Blocks.air)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(new ItemStack(CampfireBackportBlocks.getBlockFromLitAndType(!startUnlit.acceptsSoul(), EnumCampfireType.soul)),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(soulSoil), 'C', "logWood"));

            if (soulSoil == Blocks.air || !soulSoilOnly)
                GameRegistry.addRecipe(
                        new ShapedOreRecipe(new ItemStack(CampfireBackportBlocks.getBlockFromLitAndType(!startUnlit.acceptsSoul(), EnumCampfireType.soul)),
                                " A ", "ABA", "CCC", 'A', "stickWood", 'B', new ItemStack(Blocks.soul_sand), 'C', "logWood"));
        }

        // regularRegen & soulRegen
        regularRegen[0] = MathHelper.clamp_int(regularRegen[0], 0, 31);
        regularRegen[2] = MathHelper.clamp_int(regularRegen[2], 0, 100);

        soulRegen[0] = MathHelper.clamp_int(soulRegen[0], 0, 31);
        soulRegen[2] = MathHelper.clamp_int(soulRegen[2], 0, 100);

        // regularRecipeList & soulRecipeList & recipeListInheritance
        CampfireRecipe.clearRecipeLists();

        if (regularRecipeList.length != 0 || soulRecipeList.length != 0)
            ConfigReference.logInfo("parsing_recipes");

        for (String recipe : regularRecipeList)
            CampfireRecipe.addToRecipeLists(recipe,
                    recipeListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY);
        for (String recipe : soulRecipeList)
            CampfireRecipe.addToRecipeLists(recipe,
                    recipeListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY);

        // autoRecipe & autoBlacklistStrings
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
                    if (output[0] == null)
                        throw new Exception();
                    else if (output[0] instanceof Integer)
                        autoBlacklistOres.add((Integer) output[0]);
                    else if (!(autoBlacklistStacks.containsKey((Item) output[0]) && autoBlacklistStacks.get((Item) output[0]) == OreDictionary.WILDCARD_VALUE))
                        autoBlacklistStacks.put((Item) output[0], (Integer) output[2]);
                }
                catch (Exception excep)
                {
                    ConfigReference.logError("invalid_auto_blacklist", input);
                }
            }

            ConfigReference.logInfo("discovering_autos");

            Iterator inputsit = FurnaceRecipes.smelting().getSmeltingList().keySet().iterator();
            Iterator resultsit = FurnaceRecipes.smelting().getSmeltingList().values().iterator();

            iteratorLoop: while (resultsit.hasNext())
            {
                ItemStack inputstack = (ItemStack) inputsit.next();
                ItemStack resultstack = (ItemStack) resultsit.next();

                if (resultstack.getItem() instanceof ItemFood)
                {
                    if (!autoBlacklistStacks.isEmpty() && autoBlacklistStacks.get(inputstack.getItem()) != null)
                    {
                        if (autoBlacklistStacks.get(inputstack.getItem()) == OreDictionary.WILDCARD_VALUE
                                || autoBlacklistStacks.get(inputstack.getItem()) == inputstack.getItemDamage())
                            continue iteratorLoop;
                    }
                    else if (!autoBlacklistOres.isEmpty())
                    {
                        for (int id : OreDictionary.getOreIDs(inputstack))
                        {
                            if (autoBlacklistOres.contains(id))
                                continue iteratorLoop;
                        }
                    }

                    CampfireRecipe furnaceRecipe = CampfireRecipe.createAutoDiscoveryRecipe(inputstack, resultstack, autoRecipe);
                    if (furnaceRecipe != null && furnaceRecipe.getInputs().length > 0 && furnaceRecipe.getInputs()[0] != null && furnaceRecipe.hasOutputs())
                    {
                        boolean addIt = true;
                        for (CampfireRecipe masterCrecipe : CampfireRecipe.getMasterList())
                        {
                            if (CampfireRecipe.doStackRecipesMatch(furnaceRecipe, masterCrecipe))
                            {
                                addIt = false;
                                break;
                            }
                        }
                        if (addIt)
                            CampfireRecipe.addToRecipeLists(furnaceRecipe);
                    }
                }
            }
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

        for (String recipe : regularExtinguishersList)
            CampfireStateChanger.addToStateChangerLists(recipe,
                    extinguishersListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY, true);
        for (String recipe : soulExtinguishersList)
            CampfireStateChanger.addToStateChangerLists(recipe,
                    extinguishersListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, true);

        for (String recipe : regularIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe,
                    ignitorsListInheritance.equals(ConfigReference.SOUL_GETS_REG) ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY, false);
        for (String recipe : soulIgnitorsList)
            CampfireStateChanger.addToStateChangerLists(recipe,
                    ignitorsListInheritance.equals(ConfigReference.REG_GETS_SOUL) ? EnumCampfireType.BOTH : EnumCampfireType.SOUL_ONLY, false);

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
                if (output[0] == null)
                    throw new Exception();
                else if (output[0] instanceof Integer)
                    signalFireOres.add((Integer) output[0]);
                else if (!(signalFireBlocks.containsKey((Block) output[0]) && signalFireBlocks.get((Block) output[0]) == OreDictionary.WILDCARD_VALUE))
                    signalFireBlocks.put((Block) output[0], (Integer) output[2]);
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
                    if (!((NBTTagCompound) output[3]).hasNoTags())
                        drop.setTagCompound((NBTTagCompound) output[3]);

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
     * sets config with the default settings, ignoring the Configuration object
     */
    private static void doDefaultConfig()
    {
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

        automation = EnumCampfireType.BOTH;

        startUnlit = EnumCampfireType.NEITHER;
        rememberState = EnumCampfireType.NEITHER;
        silkNeeded = EnumCampfireType.BOTH;

        putOutByRain = EnumCampfireType.NEITHER;

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

        setConfig();
    }

}
