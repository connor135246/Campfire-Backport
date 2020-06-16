package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

public class CampfireRecipe
{
    private ItemStack input;
    private ItemStack output;
    private int cookingTime;
    private static ArrayList<CampfireRecipe> regRecipeList = new ArrayList<CampfireRecipe>();
    private static ArrayList<CampfireRecipe> soulRecipeList = new ArrayList<CampfireRecipe>();

    public CampfireRecipe(String recipe)
    {
        this(parseRecipe(recipe));
    }

    public CampfireRecipe(Object[] pieces)
    {
        this.input = (ItemStack) pieces[0];
        this.output = (ItemStack) pieces[1];
        this.cookingTime = (Integer) pieces[2];
    }

    public static boolean addToRecipeList(String recipe, String type)
    {
        CampfireRecipe crecipe = new CampfireRecipe(recipe);
        if (crecipe.input != null && crecipe.output != null)
        {
            getRecipeList(type).add(crecipe);
            return true;
        }
        else
        {
            CampfireBackport.proxy.modlog.error("Recipe (type: " + type + ") " + recipe + " has invalid inputs/outputs!");
            return false;
        }
    }

    public static CampfireRecipe findRecipe(ItemStack input, String type)
    {
        for (CampfireRecipe crecipe : getRecipeList(type))
        {
            if (ItemStack.areItemStacksEqual(new ItemStack(input.getItem(), 1, input.getItemDamage()), crecipe.input))
                return crecipe;
        }
        return null;
    }

    /**
     * Converts a string into a campfire recipe. If it's invalid, says so in the console.<br>
     * <br>
     * Time is a number. It's set to 1 if it's less than 1. See {@link StringParsers#parseItemStack(String) parseItemStack} for the other formats.
     * 
     * @param recipe
     *            - a string in the format [ItemStack]/[ItemStack OR ItemStack with a size]/[Time]
     * @return an Object[] where the first element is the input ItemStack, the second element is the output ItemStack, and the third element is the cooking time
     */
    public static Object[] parseRecipe(String recipe)
    {
        try
        {
            String[] segment = recipe.split("/");

            ItemStack input = StringParsers.parseItemStack(segment[0], 1);
            ItemStack output = StringParsers.parseItemStack(segment[1]);
            Integer cookingTime;
            if (segment.length > 2)
                cookingTime = Math.max(Integer.parseInt(segment[2]), 1);
            else
                cookingTime = 600;

            return new Object[] { input, output, cookingTime };
        }
        catch (Exception excep)
        {
            CampfireBackport.proxy.modlog.error("Recipe " + recipe + " is invalid!");
            return new Object[] { null, null, null };
        }
    }

    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        return "{[" + input.getDisplayName() + "] -> [" + output.getDisplayName() + "] x " + output.stackSize + ", " + cookingTime + " Ticks}";
    }

    // Getters and Setters

    public ItemStack getInput()
    {
        return input;
    }

    public ItemStack getOutput()
    {
        return output;
    }

    public int getCookingTime()
    {
        return cookingTime;
    }

    public static ArrayList<CampfireRecipe> getRecipeList(String type)
    {
        return type.equals("regular") ? regRecipeList : soulRecipeList;
    }

    public static void addToRecipeList(ArrayList<CampfireRecipe> clist, String type)
    {
        getRecipeList(type).addAll(clist);
    }

}
