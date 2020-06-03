package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.CommonProxy;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

public class CampfireRecipe
{
    private ItemStack input;
    private ItemStack output;
    private int cookingTime;
    private static ArrayList<CampfireRecipe> recipeList = new ArrayList<CampfireRecipe>();

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

    public static boolean addToRecipeList(String recipe)
    {
        CampfireRecipe crecipe = new CampfireRecipe(recipe);
        if (crecipe.input != null && crecipe.output != null)
        {
            recipeList.add(crecipe);
            return true;
        }
        else
        {
            CommonProxy.modlog.error("Recipe " + recipe + " has invalid inputs/outputs!");
            return false;
        }
    }

    public static CampfireRecipe findRecipe(ItemStack input)
    {
        for (CampfireRecipe crecipe : recipeList)
        {
            if (ItemStack.areItemStacksEqual(new ItemStack(input.getItem(), 1, input.getItemDamage()), crecipe.input))
                return crecipe;
        }
        return null;
    }

    /**
     * Converts a string into a recipe. If it's invalid, says so in the console. Time is a number. It's set to 1 if it's less than 1. See {@link #parseItemStack(String)
     * parseItemStack} for the other formats.
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

            ItemStack input = parseItemStack(segment[0], 1);
            ItemStack output = parseItemStack(segment[1]);
            Integer cookingTime;
            if (segment.length > 2)
                cookingTime = Math.max(Integer.parseInt(segment[2]), 1);
            else
                cookingTime = 600;

            return new Object[] { input, output, cookingTime };
        }
        catch (Exception excep)
        {
            CommonProxy.modlog.error("Recipe " + recipe + " is invalid!");
            return new Object[] { null, null, null };
        }
    }

    /**
     * Converts a string into an ItemStack.
     * 
     * @param stack
     *            - a string in the format modid:name:meta or modid:name for a normal ItemStack; or modid:name:meta@size or modid:name@size for an ItemStack with a size
     * @return an ItemStack of the item, meta (meta 0 if none is given), and size (size 1 if none is given) given, or null if the item doesn't exist
     */
    public static ItemStack parseItemStack(String stack)
    {
        String[] segment = stack.split("@");

        if (segment.length == 1)
            return parseItemStack(segment[0], 1);
        else
            return parseItemStack(segment[0], MathHelper.clamp_int(Integer.parseInt(segment[1]), 1, 64));
    }

    public static ItemStack parseItemStack(String stack, int size)
    {
        String name;
        int meta = 0;

        if (stack.matches("\\w+:\\w+:\\d+"))
        {
            String[] segment = stack.split(":");
            name = segment[0] + ":" + segment[1];
            meta = Integer.parseInt(segment[2]);
        }
        else
            name = stack;

        if (GameData.getItemRegistry().containsKey(name))
            return new ItemStack(GameData.getItemRegistry().getObject(name), size, meta);
        else if (GameData.getBlockRegistry().containsKey(name))
            return new ItemStack(GameData.getBlockRegistry().getObject(name), size, meta);
        else
            return null;
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

    public static ArrayList<CampfireRecipe> getRecipeList()
    {
        return recipeList;
    }

    public static void addToRecipeList(ArrayList<CampfireRecipe> clist)
    {
        recipeList.addAll(clist);
    }

}
