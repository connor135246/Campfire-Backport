package connor135246.campfirebackport.common.recipes;

import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

public class CustomData extends CustomInput<NBTTagCompound>
{

    public CustomData(int inputSize, NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(data, inputSize, data, inputSizeMatters, clamp);

        if (getDataType() != 4)
            neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("anything")));

        ItemStack listStack = new ItemStack(Items.written_book);
        inputList.add(listStack);

        finishTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        return true; // the stack is always considered to match - data is checked afterward.
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (stack != null)
        {
            String key = getDataType() != 4 ? "anything" : "any_tinkers";
            stack.setStackDisplayName(EnumChatFormatting.ITALIC + "<" + StringParsers.translateNEI(key) + ">");
        }

        return super.modifyStackForDisplay(stack);
    }

    @Override
    public String toStringName()
    {
        if (getDataType() == 4)
            return "A Tinker's Construct tool";
        else
            return "Anything";
    }

    @Override
    public int getSortOrder()
    {
        return 500;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        return super.compareTo(other);
    }

}
