package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import minetweaker.api.item.IIngredient;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import scala.actors.threadpool.Arrays;

/**
 * For when CraftTweaker is loaded.
 */
public class ActiveCraftTweakerIngredient implements ICraftTweakerIngredient
{
    // simple recipes don't really need the nei tooltip
    public static final Pattern simpleModidNameMetaPat = Pattern.compile("(?!^<ore:)<\\w+:\\w+(((:\\d+)?)|(:\\*))>");

    public final IIngredient ingredient;
    public final boolean isSimple;

    public ActiveCraftTweakerIngredient(IIngredient ingredient)
    {
        this.ingredient = ingredient;
        this.isSimple = simpleModidNameMetaPat.matcher(ingredient.toString()).matches();
    }

    @Override
    public boolean matches(ItemStack stack, boolean inputSizeMatters)
    {
        return ingredient.matches(new MCItemStack(stack, !inputSizeMatters));
    }

    @Override
    public List<ItemStack> getItems()
    {
        return Arrays.asList(MineTweakerMC.getExamples(ingredient));
    }

    @Override
    public LinkedList<String> getNEITooltip()
    {
        LinkedList<String> tip = new LinkedList<String>();

        if (!isSimple())
        {
            String ingredientString = ingredient.toString();

            if (ingredientString.startsWith("(Ingredient) "))
                ingredientString = ingredientString.substring(13);

            int withTagIndex = ingredientString.indexOf(".withTag");
            if (withTagIndex != -1)
            {
                tip.add(EnumChatFormatting.GOLD + ingredientString.substring(0, withTagIndex));
                tip.add(EnumChatFormatting.GOLD + "   " + ingredientString.substring(withTagIndex));
            }
            else
                tip.add(EnumChatFormatting.GOLD + ingredientString);
        }

        return tip;
    }

    @Override
    public boolean isSimple()
    {
        return isSimple;
    }

}
