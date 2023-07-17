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

        String name = StringParsers.translateNEI(getDataType() != 4 ? "anything" : "any_tinkers");

        if (getDataType() != 4)
            neiTooltip.add(EnumChatFormatting.GOLD + name);

        ItemStack listStack = new ItemStack(Items.written_book);
        listStack.setStackDisplayName(EnumChatFormatting.ITALIC + "<" + name + ">");

        inputList.add(listStack);

        finishTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        return true; // the stack is always considered to match - data is checked afterward.
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
