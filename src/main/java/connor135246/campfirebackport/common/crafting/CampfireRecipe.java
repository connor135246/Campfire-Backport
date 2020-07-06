package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;

public class CampfireRecipe extends GenericCustomInput implements Comparable<CampfireRecipe>
{

    public static final String SIGNAL = "signal";
 
    private ItemStack output;
    private int cookingTime;
    private Boolean signalFire = null;

    /** the master list of recipes! */
    private static ArrayList<CampfireRecipe> masterRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the regular campfire. copied from the master list. */
    private static ArrayList<CampfireRecipe> regRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the soul campfire. copied from the master list. */
    private static ArrayList<CampfireRecipe> soulRecipeList = new ArrayList<CampfireRecipe>();

    /**
     * Converts a string into a campfire recipe. If it's invalid, says so in the console.<br>
     * <br>
     * Cooking Time is a number. It's set to 1 if it's less than 1. <br>
     * signal means the recipe only works on signal fires. notsignal means the recipe doesn't work on signal fires.<br>
     * See {@link StringParsers#parseItemStackOrOreOrClassWithNBTOrDataWithSize(String)} for the input/output format.
     * 
     * @param recipe
     *            - a string in the format [ANY INPUT]/[ANY OUTPUT]/[Cooking Time]/[signal|notsignal]
     * @param types
     *            - the types of campfire this is being added to
     */
    public CampfireRecipe(String recipe, EnumCampfireType types)
    {
        try
        {
            String[] segment = recipe.split("/");

            // input

            customGInput(StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[0], true), types, false, 4);

            // output

            Object[] output = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[1], false);

            ItemStack outputStack = new ItemStack((Item) output[0], MathHelper.clamp_int((Integer) output[1], 1, 64), (Integer) output[2]);

            if (!((NBTTagCompound) output[3]).hasNoTags())
            {
                outputStack.setTagCompound((NBTTagCompound) output[3]);
                outputStack.stackTagCompound.removeTag(StringParsers.GCI_DATATYPE);
            }

            this.output = outputStack;

            // cooking time

            if (segment.length > 2)
                this.cookingTime = Math.max(Integer.parseInt(segment[2]), 1);
            else
                this.cookingTime = 600;

            // signal fire setting

            if (segment.length > 3)
                this.signalFire = segment[3].equals(SIGNAL);
        }
        catch (Exception excep)
        {
            this.input = null;
            this.output = null;
        }
    }

    /**
     * for creating recipes from Auto Recipe Discovery. {@link GenericCustomInput#sortPriority} is reduced. cookingTime is 600.
     */
    public CampfireRecipe(ItemStack input, ItemStack output, EnumCampfireType types)
    {
        try
        {
            autoGInput(ItemStack.copyItemStack(input), types);

            this.output = output;
            this.cookingTime = 600;

            setSortPriority(50);
        }
        catch (Exception excep)
        {
            this.input = null;
            this.output = null;
        }
    }

    /**
     * Tries to make a <code>CampfireRecipe</code> based on user input for the given campfire types and add it the master recipe list.<br>
     * See {@link #CampfireRecipe(String, EnumCampfireType)}.
     * 
     * @param recipe
     *            - the user-input string that represents a recipe
     * @param types
     *            - the types of campfire this recipe should apply to
     * @return true if the recipe was added successfully, false if it wasn't
     */
    public static boolean addToMasterList(String recipe, EnumCampfireType types)
    {
        CampfireRecipe crecipe = new CampfireRecipe(recipe, types);
        if (crecipe.getInput() != null && crecipe.getOutput() != null)
        {
            getMasterList().add(crecipe);
            return true;
        }
        else
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_recipe", recipe));
            return false;
        }
    }

    public static ArrayList<CampfireRecipe> getMasterList()
    {
        return masterRecipeList;
    }

    public static ArrayList<CampfireRecipe> getRecipeList(String type)
    {
        return type.equals(EnumCampfireType.REGULAR) ? regRecipeList : (type.equals(EnumCampfireType.SOUL) ? soulRecipeList : new ArrayList<CampfireRecipe>());
    }

    /**
     * Finds a <code>CampfireRecipe</code> in the given campfire type that applies to the given <code>ItemStack</code>.
     * 
     * @param stack
     * @param type
     * @param signalFire
     *            - is the campfire calling this a signal fire?
     * @return a matching <code>CampfireRecipe</code>, or null if none was found
     */
    public static CampfireRecipe findRecipe(ItemStack stack, String type, boolean signalFire)
    {
        for (CampfireRecipe crecipe : getRecipeList(type))
        {
            if (matches(crecipe, stack) && (crecipe.doesSignalMatter() ? (crecipe.requiresSignalFire() == signalFire) : true))
                return crecipe;
        }
        return null;
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        return super.toString() + (isMultiInput() ? " x " + getInputSize() : "") + " -> [" + getOutput().getDisplayName()
                + (getOutput().hasTagCompound() ? " with NBT:" + getOutput().getTagCompound() : "")
                + "]" + (getOutput().stackSize > 1 ? " x " + getOutput().stackSize : "") + ", " + getCookingTime() + " Ticks"
                + (doesSignalMatter() ? " (" + (requiresSignalFire() ? "must" : "must not") + " be a signal fire)" : "");
    }

    // Getters and Setters

    public ItemStack getOutput()
    {
        return output;
    }

    public int getCookingTime()
    {
        return cookingTime;
    }

    public boolean doesSignalMatter()
    {
        return signalFire != null;
    }

    public Boolean requiresSignalFire()
    {
        return signalFire;
    }

    // Sorting
    @Override
    public int compareTo(CampfireRecipe crecipe)
    {
        return super.compareTo(crecipe);
    }

}
