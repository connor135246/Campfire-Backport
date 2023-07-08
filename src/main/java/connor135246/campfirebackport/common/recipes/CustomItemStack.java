package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

public class CustomItemStack extends CustomInput<ItemStack>
{

    protected final boolean metaSpecified;

    public CustomItemStack(Item item, int meta, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(new ItemStack((Item) item, 1, meta), inputSize, data, inputSizeMatters, clamp);

        this.metaSpecified = meta != OreDictionary.WILDCARD_VALUE;

        inputList.add(ItemStack.copyItemStack(this.input));

        addDataTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        return this.metaSpecified ? stack.isItemEqual(this.input) : this.input.getItem() == stack.getItem();
    }

    @Override
    public String toStringName()
    {
        if (this.metaSpecified || anIffyCheckToJustifyImprovedReadability())
            return this.input.getItem().getItemStackDisplayName(this.input);
        else
            return this.input.getItem().getItemStackDisplayName(this.input) + " (any metadata)";
    }

    /**
     * only used by {@link #toStringName()}
     */
    public boolean anIffyCheckToJustifyImprovedReadability()
    {
        ItemStack input0 = new ItemStack(this.input.getItem(), 1, 0);
        ItemStack input1 = new ItemStack(this.input.getItem(), 1, 1);
        return input0.getItem().getItemStackDisplayName(input0).equals(input1.getItem().getItemStackDisplayName(input1));
    }

    @Override
    public int getSortOrder()
    {
        return 100;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        int value = super.compareTo(other);
        if (value == 0 && other instanceof CustomItemStack)
        {
            CustomItemStack otherStack = (CustomItemStack) other;
            // inputs that specify a meta come first.
            value = Boolean.compare(otherStack.metaSpecified, this.metaSpecified);
            // keeps items with the same id and damage together.
            if (value == 0)
            {
                value = Integer.compare(Item.getIdFromItem(this.input.getItem()), Item.getIdFromItem(otherStack.input.getItem()));
                if (value == 0)
                    value = Integer.compare(this.input.getItemDamage(), otherStack.input.getItemDamage());
            }
        }
        return value;
    }

}
