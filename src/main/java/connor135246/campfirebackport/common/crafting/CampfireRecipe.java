package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.oredict.OreDictionary;

public class CampfireRecipe implements Comparable<CampfireRecipe>
{
    /**
     * for recipes that were defined by an ItemStack, this is the ItemStack with a size of 1.<br>
     * for recipes that were defined by an OreDict, this is null.
     */
    private ItemStack inputStack;
    /**
     * for recipes that were defined by an ItemStack, this is null.<br>
     * for recipes that were defined by an OreDict, this is the name of the OreDict.
     */
    private String inputOre;
    /** this is the input stack size. */
    private int inputSize;
    private ItemStack output;
    private int cookingTime;
    /** whether or not meta was specified. */
    private boolean metaMatters;
    /** the types of campfires that this recipe applies to */
    private EnumCampfireType types = EnumCampfireType.NEITHER;
    /** a modifier to the sorting order. it's 0 by default. it's set to 1 for recipes created by Auto Recipe Discovery. */
    private int sortPriority = 0;

    /** the master list of recipes! */
    private static ArrayList<CampfireRecipe> masterRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the regular campfire. copied from the master list. */
    private static ArrayList<CampfireRecipe> regRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the soul campfire. copied from the master list. */
    private static ArrayList<CampfireRecipe> soulRecipeList = new ArrayList<CampfireRecipe>();

    /**
     * Converts a string into a campfire recipe. If it's invalid, says so in the console.<br>
     * <br>
     * Time is a number. It's set to 1 if it's less than 1. See {@link StringParsers#parseItemStackOrOreWithSize(String)} for the other formats.
     * 
     * @param recipe
     *            - a string in the format [ItemStack OR ItemStack with a size OR Ore Dictionary Name]/[ItemStack OR ItemStack with a size]/[Time]
     * @param types
     *            - the types of campfire this is being added to
     */
    public CampfireRecipe(String recipe, EnumCampfireType types)
    {
        try
        {
            String[] segment = recipe.split("/");

            Object[] input = StringParsers.parseItemStackOrOreWithSize(segment[0]);

            this.inputSize = MathHelper.clamp_int((Integer) input[1], 1, 4);

            if (input[0] instanceof String)
                this.inputOre = (String) input[0];
            else
                this.inputStack = new ItemStack((Item) input[0], 1, (Integer) input[2]);

            this.metaMatters = (Integer) input[2] != -1;

            Object[] output = StringParsers.parseItemStackOrOreWithSize(segment[1]);
            this.output = new ItemStack((Item) output[0], (Integer) output[1], (Integer) output[2]);

            if (segment.length > 2)
                this.cookingTime = Math.max(Integer.parseInt(segment[2]), 1);
            else
                this.cookingTime = 600;

            this.types = types;
        }
        catch (Exception excep)
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn("Recipe " + recipe + " has invalid inputs/outputs!");
            this.inputOre = null;
            this.inputStack = null;
            this.output = null;
            this.types = null;
        }
    }

    /**
     * for creating recipes from Auto Recipe Discovery. sortPriority is reduced.
     */
    public CampfireRecipe(ItemStack input, ItemStack output, int cookingTime, EnumCampfireType types)
    {
        this.inputSize = input.stackSize;
        this.inputStack = input.copy();
        this.inputStack.stackSize = 1;

        this.metaMatters = input.getItemDamage() != 32767;

        this.output = output;
        this.cookingTime = cookingTime;

        this.types = types;

        this.sortPriority = 1;
    }

    /**
     * Tries to make a CampfireRecipe based on user input for the given campfire types and add it the master recipe list.<br>
     * See {@link #CampfireRecipe(String, EnumCampfireType)}.
     * 
     * @param recipe
     *            - the user-input string that represents a recipe
     * @param types
     *            - the types of campfire this recipe should apply to
     * @return true if the recipe was added successfully, false if it wasn't
     */
    public static boolean addToRecipeList(String recipe, EnumCampfireType types)
    {
        CampfireRecipe crecipe = new CampfireRecipe(recipe, types);
        if ((crecipe.getInputStack() != null || crecipe.getInputOre() != null) && crecipe.getOutput() != null)
        {
            getMasterList().add(crecipe);
            return true;
        }
        else
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn("Recipe " + recipe + " has invalid inputs/outputs!");
            return false;
        }
    }

    public static ArrayList<CampfireRecipe> getRecipeList(String type)
    {
        return type.equals(EnumCampfireType.REGULAR) ? regRecipeList : (type.equals(EnumCampfireType.SOUL) ? soulRecipeList : new ArrayList<CampfireRecipe>());
    }

    public static ArrayList<CampfireRecipe> getMasterList()
    {
        return masterRecipeList;
    }

    /**
     * Finds a CampfireRecipe in the given campfire type that applies to the given ItemStack.
     * 
     * @param stack
     * @param type
     * @return a matching CampfireRecipe, or null if none was found
     */
    public static CampfireRecipe findRecipe(ItemStack stack, String type)
    {
        for (CampfireRecipe crecipe : getRecipeList(type))
        {
            if (matches(crecipe, stack))
                return crecipe;
        }
        return null;
    }

    /**
     * @param crecipe
     * @param stack
     * @return true if the given CampfireRecipe applies to the given ItemStack
     */
    public static boolean matches(CampfireRecipe crecipe, ItemStack stack)
    {
        return crecipe.isOreDictRecipe() ? matchesTheOre(crecipe, stack) : matchesTheStack(crecipe, stack);
    }

    public static boolean matchesTheStack(CampfireRecipe crecipe, ItemStack stack)
    {
        return crecipe.doesMetaMatter() ? OreDictionary.itemMatches(crecipe.getInputStack(), stack, true)
                : crecipe.getInputStack().getItem() == stack.getItem();
    }

    public static boolean matchesTheOre(CampfireRecipe crecipe, ItemStack stack)
    {
        return matchesTheOre(crecipe.getInputOre(), stack);
    }

    public static boolean matchesTheOre(String ore, ItemStack stack)
    {
        for (int id : OreDictionary.getOreIDs(stack))
        {
            if (id == OreDictionary.getOreID(ore))
                return true;
        }
        return false;
    }

    /**
     * meant for comparing two campfire recipes whose {@link #isOreDictRecipe()} is false
     * 
     * @param crecipe1
     * @param crecipe2
     * @return true if the campfire recipes are the same, false if they aren't
     */
    public static boolean doStackRecipesMatch(CampfireRecipe crecipe1, CampfireRecipe crecipe2)
    {
        return matchesTheStack(crecipe1, crecipe2.getInputStack()) && crecipe1.doesMetaMatter() == crecipe2.doesMetaMatter()
                && crecipe1.getTypes() == crecipe2.getTypes();
    }

    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        String inputToString;
        if (isOreDictRecipe())
            inputToString = "{[OreDict: " + getInputOre() + "]";
        else if (doesMetaMatter() || anIffyCheckToJustifyImprovedReadability())
            inputToString = "{[" + getInputStack().getDisplayName() + "]";
        else
            inputToString = "{[" + getInputStack().getDisplayName() + " (any metadata)]";

        return inputToString + (isMultiInput() ? " x " + inputSize : "") + " -> [" + output.getDisplayName() + "] x " + output.stackSize + ", " + cookingTime
                + " Ticks} ";// + getTypes();
    }

    /**
     * only used by {@link #toString()}
     */
    public boolean anIffyCheckToJustifyImprovedReadability()
    {
        return ((new ItemStack(getInputStack().getItem(), 1, 0)).getDisplayName().equals(new ItemStack(getInputStack().getItem(), 1, 1).getDisplayName()));
    }

    // Getters and Setters

    /**
     * @return true if this recipe has a value set in {@link #inputOre}, false otherwise
     */
    public boolean isOreDictRecipe()
    {
        return inputOre != null ? true : false;
    }

    public ItemStack getInputStack()
    {
        return inputStack;
    }

    public String getInputOre()
    {
        return inputOre;
    }

    public int getInputSize()
    {
        return inputSize;
    }

    public boolean doesMetaMatter()
    {
        return metaMatters;
    }

    public ItemStack getOutput()
    {
        return output;
    }

    public int getCookingTime()
    {
        return cookingTime;
    }

    public boolean isMultiInput()
    {
        return inputSize > 1;
    }

    public EnumCampfireType getTypes()
    {
        return types;
    }

    public void setTypes(EnumCampfireType types)
    {
        this.types = types;
    }

    public int getSortPriority()
    {
        return sortPriority;
    }

    @Override
    public int compareTo(CampfireRecipe crecipe)
    {
        int value = 0;

        // recipes with isMultiInput = true should go MUCH closer to the start of the list.
        value -= 2 * Boolean.compare(this.isMultiInput(), crecipe.isMultiInput());
        // recipes with doesMetaMatter = true should go closer to the start of the list.
        value -= Boolean.compare(this.doesMetaMatter(), crecipe.doesMetaMatter());
        // recipes with isOreDicted = true should go MUCH MUCH closer to the end of the list.
        value += 4 * Boolean.compare(this.isOreDictRecipe(), crecipe.isOreDictRecipe());
        // recipes created by Auto Recipe Discovery should go MUCH MUCH MUCH closer to the end of the list.
        value += 8 * Integer.compare(this.getSortPriority(), crecipe.getSortPriority());

        return value;
    }

}
