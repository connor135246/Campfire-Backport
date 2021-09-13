package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import connor135246.campfirebackport.util.StringParsers;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IngredientAny;
import minetweaker.api.item.IngredientAnyAdvanced;
import minetweaker.api.item.IngredientItem;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;
import minetweaker.api.oredict.IngredientOreDict;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/**
 * For when CraftTweaker is loaded.
 */
public class ActiveCraftTweakerIngredient implements ICraftTweakerIngredient
{

    public final IIngredient iingredient;
    public final boolean isWildcard;
    public final boolean hasFunctions;
    public final int sortOrder;

    public ActiveCraftTweakerIngredient(IIngredient iingredient)
    {
        this.iingredient = iingredient;

        IIngredient internal = AbstractItemFunction.reflectIngredientStackInternal(iingredient);
        this.isWildcard = internal instanceof IngredientAny || internal instanceof IngredientAnyAdvanced;

        if (internal instanceof IngredientItem)
            sortOrder = 20;
        else if (internal instanceof IItemStack)
            sortOrder = 40;
        else if (internal instanceof IngredientOreDict)
            sortOrder = 60;
        else if (internal instanceof IOreDictEntry)
            sortOrder = 80;
        else if (internal instanceof IngredientAnyAdvanced)
            sortOrder = 100;
        else
            sortOrder = 200;

        this.hasFunctions = AbstractItemFunction.getFunctions(iingredient).length > 0;
    }

    @Override
    public boolean matches(ItemStack stack, boolean inputSizeMatters)
    {
        return iingredient.matches(inputSizeMatters ? MineTweakerMC.getIItemStack(stack) : MineTweakerMC.getIItemStackWildcardSize(stack));
    }

    @Override
    public boolean hasTransforms()
    {
        return iingredient.hasTransformers();
    }

    @Override
    public ItemStack applyTransform(ItemStack stack, EntityPlayer player)
    {
        if (hasTransforms())
            return MineTweakerMC.getItemStack(iingredient.applyTransform(MineTweakerMC.getIItemStack(stack), MineTweakerMC.getIPlayer(player)));
        else
            return stack;
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

        IIngredient internal = isWildcard() ? iingredient : AbstractItemFunction.reflectIngredientStackInternal(iingredient);

        if (internal instanceof IOreDictEntry || internal instanceof IngredientOreDict)
            tip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("ore_input", internal.getInternal()));

        String nbt = "";

        if (hasFunctions())
        {
            AbstractItemFunction[] functions = AbstractItemFunction.getFunctions(iingredient);

            for (String functionTip : AbstractItemFunction.getInfoStrings(functions))
                tip.add(EnumChatFormatting.GOLD + functionTip);

            nbt = AbstractItemFunction.getCombinedNBTString(functions);
        }

        if (nbt.isEmpty())
        {
            String ingredientString = internal.toString();
            int withTagIndex = ingredientString.indexOf(".withTag");
            if (withTagIndex != -1 && withTagIndex + 9 <= ingredientString.length() - 1)
                nbt = ingredientString.substring(withTagIndex + 9, ingredientString.length() - 1);
        }

        if (!nbt.isEmpty())
        {
            String firstLinePrefix = EnumChatFormatting.GOLD + StringParsers.translateNEI("nbt_data") + " ";
            String otherLinePrefix = EnumChatFormatting.GOLD + "   ";
            tip.addAll(StringParsers.lineifyString(nbt, ",", firstLinePrefix, otherLinePrefix, 50));
        }

        return tip;
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (hasFunctions())
            for (AbstractItemFunction function : AbstractItemFunction.getFunctions(iingredient))
                stack = function.modifyStackForDisplay(stack);
        return stack;
    }

    @Override
    public boolean isWildcard()
    {
        return isWildcard;
    }

    @Override
    public boolean hasFunctions()
    {
        return hasFunctions;
    }

    @Override
    public int getSortOrder()
    {
        return sortOrder;
    }

}
