package connor135246.campfirebackport.config;

import java.util.ArrayList;
import java.util.List;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public class ConfigReference
{

    public static final String README_FILENAME = Reference.MODID + ".readme.txt";

    // default settings
    public static final String NEITHER = EnumCampfireType.NEITHER.toString(),
            REG_ONLY = EnumCampfireType.REG_ONLY.toString(),
            SOUL_ONLY = EnumCampfireType.SOUL_ONLY.toString(),
            BOTH = EnumCampfireType.BOTH.toString(),
            SOUL_GETS_REG = "soul inherits regular",
            REG_GETS_SOUL = "regular inherits soul",
            NO_GETS = "no inheritance";

    public static final String[] empty = new String[0],
            enumSettings = new String[] { NEITHER, REG_ONLY, SOUL_ONLY, BOTH },
            inheritanceSettings = new String[] { SOUL_GETS_REG, REG_GETS_SOUL, NO_GETS },
            defaultRecipeList = new String[] { "minecraft:porkchop/minecraft:cooked_porkchop", "minecraft:beef/minecraft:cooked_beef",
                    "minecraft:chicken/minecraft:cooked_chicken", "minecraft:potato/minecraft:baked_potato", "minecraft:fish:0/minecraft:cooked_fished:0",
                    "minecraft:fish:1/minecraft:cooked_fished:1" },
            defaultExtinguishersList = new String[] { "right/minecraft:water_bucket/stackable>minecraft:bucket",
                    "right+dispensable/tool:shovel/damageable", "right/[Fluid:\"water\",MinAmount:1000]/none" },
            defaultIgnitorsList = new String[] { "right/minecraft:flint_and_steel/damageable", "right/minecraft:fire_charge/stackable",
                    "left+dispensable/class:net.minecraft.item.ItemSword[ench:20,1]/damageable", "left/[ench:20,1]/damageable",
                    "left/[Tinkers:[I:{Fiery:1}]]/damageable", "left/[Tinkers:[B:{Lava:1}]]/damageable" },
            defaultCampfireDrops = new String[] { "", "netherlicious:SoulSoil" },
            defaultSignalFireBlocks = new String[] { "minecraft:hay_block" };

    public static final int[] defaultRegRegen = new int[] { 0, 50, 5, 900 },
            defaultSoulRegen = new int[] { 1, 50, 10, 750 },
            defaultBurnOuts = new int[] { -1, -1 };

    public static final double[] defaultBurnToNothingChances = new double[] { 0.0, 0.0 },
            defaultVisCosts = new double[] { 0.5, 0.5, 0.5, 0.5 };

    public static final ItemStack defaultRegDrop = new ItemStack(Items.coal, 2, 1),
            defaultSoulDrop = new ItemStack(Blocks.soul_sand);

    // translating
    public static final String regular = StatCollector.translateToLocal(Reference.MODID + ".reg"),
            soul = StatCollector.translateToLocal(Reference.MODID + ".soul"),
            extinguisher = StatCollector.translateToLocal(Reference.MODID + ".extinguisher"),
            ignitor = StatCollector.translateToLocal(Reference.MODID + ".ignitor");

    /**
     * if input error suppression is off, translates the key/args using the "config.inputerror" prefix and logs it
     */
    public static void logError(String key, Object... args)
    {
        if (!CampfireBackportConfig.suppressInputErrors)
            CommonProxy.modlog.warn(StringParsers.translateInputError(key, args));
    }

    /**
     * if printing info is on, translates the key using the "config.info" prefix and logs it
     */
    public static void logInfo(String key)
    {
        if (CampfireBackportConfig.printCustomRecipes)
            CommonProxy.modlog.info(StringParsers.translateInfo(key));
    }

    // config option names
    public static final String charcoalOnly = "Charcoal Only",
            soulSoilOnly = "Soul Soil Only (Netherlicious)",
            regenCampfires = "Regeneration Campfires",
            regularRegen = "Regeneration Settings (Regular Campfires)",
            soulRegen = "Regeneration Settings (Soul Campfires)",
            autoRecipe = "Auto Recipe Discovery",
            autoBlacklistStrings = "Auto Recipe Discovery Blacklist",
            regularRecipeList = "Custom Recipes (Regular)",
            soulRecipeList = "Custom Recipes (Soul)",
            recipeListInheritance = "Custom Recipe Inheritance",
            spawnpointable = "Set Respawn Point",
            burnOutOnRespawn = "Burn Out on Respawn",
            automation = "Automation",
            startUnlit = "Unlit by Default",
            rememberState = "Remember Lit/Unlit State",
            silkNeeded = "Silk Touch Needed",
            putOutByRain = "Put Out by Rain",
            damaging = "Damage",
            visCosts = "Vis Costs",
            burnOutTimer = "Burn Out Timers",
            burnOutRules = "Burn Out Biome/Dimension Timers",
            signalFiresBurnOut = "Burn Out (Signal Fires)",
            burnToNothingChances = "Burn to Nothing Chances",
            burnOutAsItem = "Burn Out As An Item",
            signalFireStrings = "Signal Fire Blocks",
            campfireDropsStrings = "Campfire Drops",
            colourfulSmoke = "Colorful Campfire Smoke",
            dispenserBlacklistStrings = "Dispenser Behaviours Blacklist",
            regularExtinguishersList = "Custom Extinguishers (Regular)",
            soulExtinguishersList = "Custom Extinguishers (Soul)",
            extinguishersListInheritance = "Custom Extinguishers Inheritance",
            regularIgnitorsList = "Custom Ignitors (Regular)",
            soulIgnitorsList = "Custom Ignitors (Soul)",
            ignitorsListInheritance = "Custom Ignitors Inheritance",
            printCustomRecipes = "#Debug: Print Campfire Recipes",
            suppressInputErrors = "#Debug: Suppress Input Error Warnings";

    // config order
    public static final List<String> configOrder = new ArrayList<String>();

    static
    {
        configOrder.add(charcoalOnly);
        configOrder.add(soulSoilOnly);
        configOrder.add(automation);
        configOrder.add(startUnlit);
        configOrder.add(rememberState);
        configOrder.add(silkNeeded);
        configOrder.add(putOutByRain);
        configOrder.add(damaging);
        configOrder.add(spawnpointable);
        configOrder.add(burnOutOnRespawn);
        configOrder.add(visCosts);
        configOrder.add(signalFireStrings);
        configOrder.add(signalFiresBurnOut);
        configOrder.add(colourfulSmoke);
        configOrder.add(campfireDropsStrings);
        configOrder.add(regenCampfires);
        configOrder.add(regularRegen);
        configOrder.add(soulRegen);
        configOrder.add(burnOutTimer);
        configOrder.add(burnOutRules);
        configOrder.add(burnToNothingChances);
        configOrder.add(burnOutAsItem);
        configOrder.add(autoRecipe);
        configOrder.add(autoBlacklistStrings);
        configOrder.add(regularRecipeList);
        configOrder.add(soulRecipeList);
        configOrder.add(recipeListInheritance);
        configOrder.add(regularExtinguishersList);
        configOrder.add(soulExtinguishersList);
        configOrder.add(extinguishersListInheritance);
        configOrder.add(regularIgnitorsList);
        configOrder.add(soulIgnitorsList);
        configOrder.add(ignitorsListInheritance);
        configOrder.add(dispenserBlacklistStrings);
        configOrder.add(printCustomRecipes);
        configOrder.add(suppressInputErrors);
    }
}
