package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

public class CampfireRecipe extends GenericRecipe implements Comparable<CampfireRecipe>
{

    public static final String SIGNAL = "signal", NOTSIGNAL = "notsignal", ANY = "any";

    /** a second output. this one can have a chance associated with it. */
    protected final ItemStack byproduct;
    /** {@link #byproduct} */
    protected final double byproductChance;
    /** the time until the recipe is complete. all inputs must have reached this time. */
    protected final int cookingTime;
    /** 1 if this recipe requires a signal fire, -1 if it requires not a signal fire. 0 means it doesn't matter. */
    protected final byte signalFire;

    /** the master list of recipes! */
    private static List<CampfireRecipe> masterRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the regular campfire. copied from the master list. */
    private static List<CampfireRecipe> regRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the soul campfire. copied from the master list. */
    private static List<CampfireRecipe> soulRecipeList = new ArrayList<CampfireRecipe>();

    /** the list of recipes created with CraftTweaker. */
    private static List<CampfireRecipe> crafttweakerRecipeList = new ArrayList<CampfireRecipe>();

    /**
     * Converts a string into a CampfireRecipe.
     * 
     * @param recipe
     *            - a string in the proper format (see config explanation)
     * @param types
     *            - the types of campfire this is being added to
     */
    public static CampfireRecipe createCustomRecipe(String recipe, EnumCampfireType types)
    {
        try
        {
            String[] segment = recipe.split("/");

            // input

            String[] inputs = segment[0].split("&");

            ArrayList<CustomInput> tempInputsList = new ArrayList<CustomInput>(4);

            inputLoop: for (String input : inputs)
            {
                Object[] anInput = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(input, true);
                int stackSize = (Integer) anInput[1];
                anInput[1] = 1;
                for (int i = 0; i < stackSize; ++i)
                {
                    if (tempInputsList.size() < 4)
                    {
                        CustomInput cinput = CustomInput.createFromParsed(anInput, false, -1);

                        if (cinput.input == null)
                            throw new Exception();

                        tempInputsList.add(cinput);
                    }
                    else
                        break inputLoop;
                }
            }

            if (tempInputsList.size() == 0)
                throw new Exception();

            Collections.sort(tempInputsList);

            // output

            Object[] output = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[1], false);

            ItemStack outputStack = new ItemStack((Item) output[0], MathHelper.clamp_int((Integer) output[1], 1, 64), (Integer) output[2]);

            if (!((NBTTagCompound) output[3]).hasNoTags())
            {
                outputStack.setTagCompound((NBTTagCompound) output[3]);
                outputStack.getTagCompound().removeTag(StringParsers.KEY_GCIDataType);
            }

            // cooking time

            int cookingTime = 600;

            if (segment.length > 2)
                cookingTime = Integer.parseInt(segment[2]);

            // signal fire setting

            byte signalFire = 0;

            if (segment.length > 3 && !segment[3].equals(ANY))
                signalFire = (byte) (segment[3].equals(SIGNAL) ? 1 : -1);

            // byproduct

            ItemStack byproductStack = null;

            if (segment.length > 4)
            {
                Object[] byproduct = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[4], false);

                byproductStack = new ItemStack((Item) byproduct[0], MathHelper.clamp_int((Integer) byproduct[1], 1, 64), (Integer) byproduct[2]);

                if (!((NBTTagCompound) byproduct[3]).hasNoTags())
                {
                    byproductStack.setTagCompound((NBTTagCompound) byproduct[3]);
                    byproductStack.getTagCompound().removeTag(StringParsers.KEY_GCIDataType);
                }
            }

            double byproductChance = 1.0;

            if (segment.length > 5)
                byproductChance = Double.parseDouble(segment[5]);

            // done!

            return new CampfireRecipe(types, tempInputsList.toArray(new CustomInput[0]), new ItemStack[] { outputStack }, cookingTime, signalFire,
                    byproductStack, byproductChance, 100);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    /**
     * for creating recipes from Auto Recipe Discovery. {@link #sortPriority} is reduced. everything else is default.
     */
    public static CampfireRecipe createAutoDiscoveryRecipe(ItemStack input, ItemStack output, EnumCampfireType types)
    {
        try
        {
            return new CampfireRecipe(types, new CustomInput[] { CustomInput.createAuto(ItemStack.copyItemStack(input)) }, new ItemStack[] { output }, null,
                    null, null, null, 50);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    public CampfireRecipe(EnumCampfireType types, CustomInput[] inputs, ItemStack[] outputs, @Nullable Integer cookingTime, @Nullable Byte signalFire,
            @Nullable ItemStack byproduct, @Nullable Double byproductChance, int sortPriority)
    {
        super(types, inputs, outputs, sortPriority);

        this.cookingTime = cookingTime == null ? 600 : Math.max(1, cookingTime);
        this.signalFire = (byte) (signalFire == null ? 0 : MathHelper.clamp_int(signalFire, -1, 1));
        this.byproduct = ItemStack.copyItemStack(byproduct);
        this.byproductChance = byproductChance == null ? 1.0 : MathHelper.clamp_double(byproductChance, 0.0, 1.0);
    }

    /**
     * Tries to make a CampfireRecipe based on user input for the given campfire types and add it to the recipe lists.<br>
     * See {@link #createCustomRecipe}.
     * 
     * @param recipe
     *            - the user-input string that represents a recipe
     * @param types
     *            - the types of campfires this recipe should apply to
     * @return true if the recipe was added successfully, false if it wasn't
     */
    public static boolean addToRecipeLists(String recipe, EnumCampfireType types)
    {
        CampfireRecipe crecipe = createCustomRecipe(recipe, types);

        boolean added = addToRecipeLists(crecipe);
        if (!added)
            ConfigReference.logError("invalid_recipe", recipe);

        return added;
    }

    /**
     * Adds the given CampfireRecipe to the recipe lists, if it's not null.
     * 
     * @return true if the recipe isn't null, false otherwise
     */
    public static boolean addToRecipeLists(CampfireRecipe crecipe)
    {
        if (crecipe != null)
        {
            masterRecipeList.add(crecipe);

            if (crecipe.getTypes().acceptsRegular())
                regRecipeList.add(crecipe);
            if (crecipe.getTypes().acceptsSoul())
                soulRecipeList.add(crecipe);

            return true;
        }
        else
            return false;
    }

    /**
     * Removes the given CampfireRecipe from the recipe lists, if it's not null.
     */
    public static void removeFromRecipeLists(CampfireRecipe crecipe)
    {
        if (crecipe != null)
        {
            masterRecipeList.remove(crecipe);

            if (crecipe.getTypes().acceptsRegular())
                regRecipeList.remove(crecipe);
            if (crecipe.getTypes().acceptsSoul())
                soulRecipeList.remove(crecipe);
        }
    }

    /**
     * Sorts the three main recipe lists.
     */
    public static void sortRecipeLists()
    {
        Collections.sort(masterRecipeList);
        Collections.sort(regRecipeList);
        Collections.sort(soulRecipeList);
    }

    /**
     * Clears the three main recipe lists.
     */
    public static void clearRecipeLists()
    {
        masterRecipeList.clear();
        regRecipeList.clear();
        soulRecipeList.clear();
    }

    public static List<CampfireRecipe> getMasterList()
    {
        return masterRecipeList;
    }

    public static List<CampfireRecipe> getRecipeList(String type)
    {
        return EnumCampfireType.option(type, regRecipeList, soulRecipeList);
    }

    public static List<CampfireRecipe> getCraftTweakerList()
    {
        return crafttweakerRecipeList;
    }

    /**
     * Finds a CampfireRecipe in the given campfire type that applies to the given ItemStack, isn't contained in the given list, and has at most the given number of inputs.
     * 
     * @param stack
     * @param type
     * @param signalFire
     *            - is the campfire calling this a signal fire?
     * @param skips
     *            - the CampfireRecipes to skip over checking
     * @param maxInputs
     *            - the maximum number of inputs the CampfireRecipe should have
     * @return a matching CampfireRecipe, or null if none was found
     */
    public static CampfireRecipe findRecipe(ItemStack stack, String type, boolean signalFire, List<CampfireRecipe> skips, int maxInputs)
    {
        if (stack != null)
        {
            for (CampfireRecipe crecipe : getRecipeList(type))
            {
                if (crecipe.getInputs().length <= maxInputs && crecipe.matches(stack, signalFire) && !skips.contains(crecipe))
                    return crecipe;
            }
        }
        return null;
    }

    /**
     * sends to {@link #findRecipe(ItemStack, String, boolean, List, int)} with an empty List<CampfireRecipe> and maxInputs = 4
     */
    public static CampfireRecipe findRecipe(ItemStack stack, String type, boolean signalFire)
    {
        return findRecipe(stack, type, signalFire, new ArrayList<CampfireRecipe>(0), 4);
    }

    /**
     * Checks if the given ItemStack and signalFire state match this CampfireRecipe.
     */
    public boolean matches(ItemStack stack, boolean signalFire)
    {
        if (stack != null && (doesSignalFireMatter() ? (requiresSignalFire() == signalFire) : true))
        {
            for (CustomInput cinput : getInputs())
            {
                if (cinput.matches(stack))
                    return true;
            }
        }
        return false;
    }

    /**
     * Meant for comparing two CampfireRecipes, the first of which was created from {@link #createAutoDiscoveryRecipe}. The order they're given matters!
     * 
     * @return true if the CampfireRecipes are the same, false if they aren't
     */
    public static boolean doStackRecipesMatch(CampfireRecipe crecipeAuto, CampfireRecipe crecipeCustom)
    {
        if (crecipeCustom.getInputs().length == 1)
        {
            CustomInput cinputCustom = crecipeCustom.getInputs()[0];
            CustomInput cinputAuto = crecipeAuto.getInputs()[0];

            return cinputCustom.isItemInput() && !cinputCustom.hasExtraData() && CustomInput.matchesTheStack(cinputCustom, (ItemStack) cinputAuto.getInput());
        }
        return false;
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        StringBuilder recipeToString = new StringBuilder(48);

        for (CustomInput cinput : inputs)
            recipeToString.append(cinput.toString() + ", ");

        return recipeToString.substring(0, recipeToString.length() - 2) + " -> " + stackToString(getOutput())
                + (hasByproduct() ? ((getByproductChance() < 100 ? " with a " + getByproductChance() * 100 + "% chance of " : " and ")
                        + stackToString(getByproduct())) : "")
                + ", after " + getCookingTime() + " Ticks on "
                + (getTypes() == EnumCampfireType.BOTH ? "all" : (getTypes().acceptsRegular() ? "Regular" : "Soul")) + " campfires"
                + (doesSignalFireMatter() ? " (" + (requiresSignalFire() ? "must" : "must not") + " be a signal fire)" : "");
    }

    // Getters

    /**
     * CampfireRecipes have only one output, so we have a shortcut.
     */
    public ItemStack getOutput()
    {
        return getOutputs()[0];
    }

    public int getCookingTime()
    {
        return cookingTime;
    }

    public byte getSignalFire()
    {
        return signalFire;
    }

    public boolean doesSignalFireMatter()
    {
        return signalFire != 0;
    }

    public boolean requiresSignalFire()
    {
        return signalFire == 1;
    }

    public boolean hasByproduct()
    {
        return byproduct != null;
    }

    public ItemStack getByproduct()
    {
        return byproduct;
    }

    public double getByproductChance()
    {
        return byproductChance;
    }

    // Sorting
    @Override
    public int compareTo(CampfireRecipe crecipe)
    {
        return super.compareTo(crecipe);
    }

}
