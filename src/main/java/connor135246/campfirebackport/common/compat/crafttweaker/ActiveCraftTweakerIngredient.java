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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

/**
 * For when CraftTweaker is loaded. <br>
 * This class makes me wonder if I'm proving Hyrum's Law.
 */
public class ActiveCraftTweakerIngredient implements ICraftTweakerIngredient
{

    public final IIngredient iingredient;
    public final IIngredient iingredUsable;
    public final boolean isWildcard;
    public final boolean hasFunctions;
    public final int sortOrder;

    public ActiveCraftTweakerIngredient(IIngredient iingredient)
    {
        this.iingredient = iingredient;

        this.iingredUsable = AbstractItemFunction.reflectIngredientStackInternal(iingredient);
        this.isWildcard = this.iingredUsable instanceof IngredientAny || this.iingredUsable instanceof IngredientAnyAdvanced;

        if (iingredUsable instanceof IngredientItem)
            sortOrder = 20;
        else if (iingredUsable instanceof IItemStack)
            sortOrder = 40;
        else if (iingredUsable instanceof IngredientOreDict)
            sortOrder = 60;
        else if (iingredUsable instanceof IOreDictEntry)
            sortOrder = 80;
        else if (iingredUsable instanceof IngredientAnyAdvanced)
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

        if ((iingredUsable instanceof IngredientOreDict || iingredUsable instanceof IOreDictEntry) && iingredUsable.getInternal() instanceof String)
            tip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("ore_input", iingredUsable.getInternal()));

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
            String ingredientString = iingredUsable.toString();
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

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Override
    public int compareTo(ICraftTweakerIngredient other)
    {
        // orders CraftTweaker IIngredients by their sort order, which generally speaking puts more specific inputs at the start.
        int value = Integer.compare(this.getSortOrder(), other.getSortOrder());
        if (value != 0)
            return value;
        if (other instanceof ActiveCraftTweakerIngredient)
        {
            ActiveCraftTweakerIngredient otherCT = (ActiveCraftTweakerIngredient) other;
            // keeps items with the same id and damage together.
            if (accessibleStack(this.iingredUsable) && accessibleStack(otherCT.iingredUsable))
            {
                ItemStack stack = (ItemStack) this.iingredUsable.getInternal();
                ItemStack otherStack = (ItemStack) otherCT.iingredUsable.getInternal();
                value = Integer.compare(Item.getIdFromItem(stack.getItem()), Item.getIdFromItem(otherStack.getItem()));
                if (value == 0)
                    value = Integer.compare(stack.getItemDamage(), otherStack.getItemDamage());
            }
            // keeps the same oredicts together.
            else if (accessibleOre(this.iingredUsable) && accessibleOre(otherCT.iingredUsable))
            {
                String ore = (String) this.iingredUsable.getInternal();
                String otherOre = (String) otherCT.iingredUsable.getInternal();
                value = Integer.compare(OreDictionary.getOreID(ore), OreDictionary.getOreID(otherOre));
            }
        }
        return value;
    }

    protected static boolean accessibleStack(IIngredient iingredient)
    {
        return (iingredient instanceof IngredientItem || iingredient instanceof IItemStack) && iingredient.getInternal() instanceof ItemStack;
    }

    protected static boolean accessibleOre(IIngredient iingredient)
    {
        return (iingredient instanceof IngredientOreDict || iingredient instanceof IOreDictEntry) && iingredient.getInternal() instanceof String
                && OreDictionary.doesOreNameExist((String) iingredient.getInternal());
    }

}
