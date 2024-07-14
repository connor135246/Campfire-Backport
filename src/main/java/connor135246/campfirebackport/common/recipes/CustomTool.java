package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

public class CustomTool extends CustomInput<String>
{

    public CustomTool(String tool, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(tool, inputSize, data, inputSizeMatters, clamp);

        for (Item item : GameData.getItemRegistry().typeSafeIterable())
        {
            ItemStack listStack = new ItemStack(item);
            if (item.getToolClasses(listStack).contains(tool))
            {
                if (item.isDamageable())
                    listStack.setItemDamage(OreDictionary.WILDCARD_VALUE);
                inputList.add(listStack);
            }
        }

        // tool inputs may have empty inputLists at this point, which is a problem
        if (inputList.isEmpty())
        {
            ConfigReference.logError("no_matches_tool", tool);
            throw new Exception();
        }

        neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("tool_input", tool)));

        finishTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        return stack.getItem().getToolClasses(stack).contains(this.input);
    }

    @Override
    public String toStringName()
    {
        return "Any " + this.input + "-type tool";
    }

    @Override
    public int getSortOrder()
    {
        return 300;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        int value = super.compareTo(other);
        if (value == 0 && other instanceof CustomTool)
        {
            CustomTool otherTool = (CustomTool) other;
            // keeps the same tools together.
            value = this.input.compareTo(otherTool.input);
        }
        return value;
    }

}
