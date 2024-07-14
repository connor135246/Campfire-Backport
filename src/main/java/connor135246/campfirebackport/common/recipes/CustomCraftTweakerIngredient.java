package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

public class CustomCraftTweakerIngredient extends CustomInput<ICraftTweakerIngredient>
{
    
    protected final String stackNameKey;

    public CustomCraftTweakerIngredient(ICraftTweakerIngredient iingredient, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(iingredient, inputSize, data, inputSizeMatters, clamp);

        inputList.addAll(iingredient.getItems());
        if (inputList.isEmpty())
        {
            stackNameKey = iingredient.isWildcard() ? "anything" : "unknown";
            neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + StringParsers.translateNEI(stackNameKey)));

            ItemStack listStack = new ItemStack(Items.written_book);
            inputList.add(listStack);
        }
        else
            stackNameKey = "";

        neiTooltipFillers.add((list) -> list.addAll(iingredient.getNEITooltip()));

        finishTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        return this.input.matches(stack, this.doesInputSizeMatter());
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (stack != null && !stackNameKey.isEmpty())
            stack.setStackDisplayName(EnumChatFormatting.ITALIC + "<" + StringParsers.translateNEI(stackNameKey) + ">");
        stack = super.modifyStackForDisplay(stack);
        if (stack != null)
            stack = this.input.modifyStackForDisplay(stack);
        return stack;
    }

    @Override
    public String toStringName()
    {
        return this.input.toString();
    }

    @Override
    public int getSortOrder()
    {
        return 600;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        int value = super.compareTo(other);
        if (value == 0 && other instanceof CustomCraftTweakerIngredient)
            value = this.input.compareTo(((CustomCraftTweakerIngredient) other).input);
        return value;
    }

}
