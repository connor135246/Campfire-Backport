package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;

public class BurnOutRule
{

    /** the types of campfires this applies to. probably shouldn't be NEITHER. */
    protected final EnumCampfireType types;
    /** the biome id. Integer.MAX_VALUE means it doesn't matter. */
    protected final int biomeId;
    /** the dimension id. Integer.MAX_VALUE means it doesn't matter. */
    protected final int dimensionId;
    /** the base burn out timer. */
    protected final int timer;
    /** true if this rule is one of the default rules from {@link CampfireBackportConfig#burnOutTimer}. */
    protected final boolean defaultRule;
    /** lines of text to use for displaying extra info in NEI. */
    protected LinkedList<String> neiTooltip = new LinkedList<String>();

    /** the list of rules! the order is important. */
    private static List<BurnOutRule> rules = new ArrayList<BurnOutRule>();
    /** the two default rules, the first for regular and the second for soul. */
    private static BurnOutRule[] defaultRules = new BurnOutRule[2];

    /** the list of rules created with CraftTweaker. */
    private static List<BurnOutRule> crafttweakerRules = new ArrayList<BurnOutRule>();

    /**
     * Converts a string into a BurnOutRule.
     * 
     * @param input
     *            - a string in the proper format (see config explanation)
     */
    public static BurnOutRule createBurnOutRule(String input)
    {
        try
        {
            String[] segment = input.split("/");

            // types

            EnumCampfireType types = EnumCampfireType.FROM_NAME.get(segment[0]);

            if (types == null || types == EnumCampfireType.NEITHER)
            {
                ConfigReference.logError("invalid_types", segment[0]);
                throw new Exception();
            }

            // dimension/biome

            String segment2[] = segment[1].split("&");
            Integer biomeId = null;
            Integer dimensionId = null;
            for (String segment2pieces : segment2)
            {
                String segment3[] = segment2pieces.split(":");
                if (segment3[0].equals("biome"))
                {
                    biomeId = Integer.parseInt(segment3[1]);
                    if (!checkBiomeID(biomeId))
                    {
                        ConfigReference.logError("invalid_biome_id", biomeId);
                        throw new Exception();
                    }
                }
                else if (segment3[0].equals("dimension"))
                {
                    dimensionId = Integer.parseInt(segment3[1]);
                    if (!checkDimensionID(dimensionId))
                    {
                        ConfigReference.logError("invalid_dim_id", dimensionId);
                        throw new Exception();
                    }
                }
            }

            // timer

            int timer = Integer.parseInt(segment[2]);

            // done!

            return new BurnOutRule(types, biomeId, dimensionId, timer, false);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    public BurnOutRule(EnumCampfireType types, @Nullable Integer biomeId, @Nullable Integer dimensionId, int timer, boolean defaultRule)
    {
        this.types = types;
        this.biomeId = biomeId == null ? Integer.MAX_VALUE : biomeId;
        this.dimensionId = dimensionId == null ? Integer.MAX_VALUE : dimensionId;
        this.timer = Math.max(timer, -1);
        this.defaultRule = defaultRule;

        if (hasBiomeId())
            this.neiTooltip.add(EnumChatFormatting.GRAY + StringParsers.translateNEI("biome") + " " + BiomeGenBase.getBiome(this.biomeId).biomeName);

        if (hasDimensionId())
            this.neiTooltip.add(EnumChatFormatting.GRAY + StringParsers.translateNEI("dimension") + " "
                    + WorldProvider.getProviderForDimension(this.dimensionId).getDimensionName());
    }

    /**
     * Tries to make a BurnOutRule based on user input and add it to the rules list.<br>
     * See {@link #createBurnOutRule(String)}.
     * 
     * @param recipe
     *            - the user-input string that represents a rule
     * @return true if the rule was added successfully, false if it wasn't
     */
    public static boolean addToRules(String input)
    {
        BurnOutRule brule = createBurnOutRule(input);

        boolean added = addToRules(brule);
        if (!added)
            ConfigReference.logError("invalid_burn_out_rule", input);

        return added;
    }

    /**
     * Adds the given BurnOutRule to the rule list, if it's not null.
     * 
     * @return true if the rule isn't null, false otherwise
     */
    public static boolean addToRules(BurnOutRule brule)
    {
        if (brule != null && (brule.hasDimensionId() || brule.hasBiomeId()))
            return rules.add(brule);
        else
            return false;
    }

    /**
     * Removes the given BurnOutRule from the state changer lists, if it's not null.
     */
    public static void removeFromRules(BurnOutRule brule)
    {
        if (brule != null)
            rules.remove(brule);
    }

    public static List<BurnOutRule> getRules()
    {
        return rules;
    }

    public static void clearRules()
    {
        rules.clear();
    }

    public static List<BurnOutRule> getCraftTweakerRules()
    {
        return crafttweakerRules;
    }

    public static void setDefaultBurnOutRules(int regularTimer, int soulTimer)
    {
        defaultRules[0] = new BurnOutRule(EnumCampfireType.REG_ONLY, null, null, regularTimer, true);
        defaultRules[1] = new BurnOutRule(EnumCampfireType.SOUL_ONLY, null, null, soulTimer, true);
    }

    public static BurnOutRule[] getDefaultRules()
    {
        return defaultRules;
    }

    /**
     * Rounds doubles to ints and sends to {@link #findBurnOutRule(World, int, int, int, String)}
     */
    public static BurnOutRule findBurnOutRule(World world, double x, double y, double z, String type)
    {
        return findBurnOutRule(world, MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z), type);
    }

    /**
     * Gets the first BurnOutRule that matches the given location and campfire type, or the default config rule if no matching BurnOutRules were found.
     */
    public static BurnOutRule findBurnOutRule(World world, int x, int y, int z, String type)
    {
        if (!rules.isEmpty())
        {
            int biomeId = world.getBiomeGenForCoords(x, z).biomeID;
            int dimensionId = world.provider.dimensionId;

            for (BurnOutRule brule : rules)
            {
                if (brule.matches(biomeId, dimensionId, type))
                    return brule;
            }
        }
        return defaultRules[EnumCampfireType.toInt(type)];
    }

    /**
     * Checks if the given biome/dimension ids and campfire type match this BurnOutRule.
     */
    public boolean matches(int biomeId, int dimensionId, String type)
    {
        return getTypes().matches(type) && (!hasBiomeId() || getBiomeId() == biomeId) && (!hasDimensionId() || getDimensionId() == dimensionId);
    }

    /**
     * @return whether the biome id is a valid biome id
     */
    public static boolean checkBiomeID(int biomeId)
    {
        if (biomeId > 255 || biomeId < 0 || BiomeGenBase.getBiomeGenArray()[biomeId] == null)
            return false;
        else
            return true;
    }

    /**
     * @return whether the dimension id is a valid dimension id
     */
    public static boolean checkDimensionID(int dimensionId)
    {
        return DimensionManager.isDimensionRegistered(dimensionId);
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        return (isDefaultRule() ? "Default: " : "")
                + (getTypes() == EnumCampfireType.BOTH ? "All" : (getTypes() == EnumCampfireType.REG_ONLY ? "Regular" : "Soul")) + " campfires "
                + (getTimer() == -1 ? "don't burn out" : "burn out after around " + getTimer() + " ticks")
                + (hasBiomeId() ? " in biome id: " + getBiomeId() : "") + (hasDimensionId() ? " in dimension id: " + getDimensionId() : "");
    }

    // Getters

    public EnumCampfireType getTypes()
    {
        return types;
    }

    public boolean hasBiomeId()
    {
        return biomeId != Integer.MAX_VALUE;
    }

    public int getBiomeId()
    {
        return biomeId;
    }

    public boolean hasDimensionId()
    {
        return dimensionId != Integer.MAX_VALUE;
    }

    public int getDimensionId()
    {
        return dimensionId;
    }

    public int getTimer()
    {
        return timer;
    }

    public LinkedList<String> getNEITooltip()
    {
        return neiTooltip;
    }

    public boolean isDefaultRule()
    {
        return defaultRule;
    }

}
