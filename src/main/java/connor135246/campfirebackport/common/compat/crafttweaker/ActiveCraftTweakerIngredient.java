package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import minetweaker.api.item.IIngredient;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/**
 * For when CraftTweaker is loaded.
 */
public class ActiveCraftTweakerIngredient implements ICraftTweakerIngredient
{
    // simple recipes don't really need the nei tooltip
    public static final Pattern simpleModidNameMetaPat = Pattern.compile("(?!^<ore:)<\\w+:\\w+(((:\\d+)?)|(:\\*))>");

    public final IIngredient iingredient;
    public final boolean isSimple;

    public ActiveCraftTweakerIngredient(IIngredient iingredient)
    {
        this.iingredient = iingredient;

        this.isSimple = simpleModidNameMetaPat.matcher(iingredient.toString()).matches();
    }

    @Override
    public boolean matches(ItemStack stack, boolean inputSizeMatters)
    {
        return iingredient.matches(inputSizeMatters ? MineTweakerMC.getIItemStack(stack) : MineTweakerMC.getIItemStackWildcardSize(stack));
    }

    @Override
    public List<ItemStack> getItems()
    {
        ItemStack[] stacks = MineTweakerMC.getItemStacks(iingredient.getItems());
        return stacks != null ? Arrays.asList(stacks) : new ArrayList<ItemStack>();
    }

    @Override
    public LinkedList<String> getNEITooltip()
    {
        LinkedList<String> tip = new LinkedList<String>();

        if (!isSimple())
        {
            String ingredientString = iingredient.toString();

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
