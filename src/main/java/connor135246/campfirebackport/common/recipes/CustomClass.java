package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

public class CustomClass extends CustomInput<Class>
{

    public CustomClass(Class clazz, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(clazz, inputSize, data, inputSizeMatters, clamp);

        if (Block.class.isAssignableFrom(clazz))
        {
            for (Block block : GameData.getBlockRegistry().typeSafeIterable())
                if (clazz.isAssignableFrom(block.getClass()) && Item.getItemFromBlock(block) != null)
                    inputList.add(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
        }
        else
        {
            for (Item item : GameData.getItemRegistry().typeSafeIterable())
                if (clazz.isAssignableFrom(item.getClass()))
                    inputList.add(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
        }

        // class inputs may have empty inputLists at this point, which is a problem
        if (inputList.isEmpty())
        {
            ConfigReference.logError("no_matches_class", clazz.getCanonicalName());
            throw new Exception();
        }

        neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("class_input", clazz.getSimpleName()));

        addDataTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        if (Block.class.isAssignableFrom(this.input))
        {
            Block block = Block.getBlockFromItem(stack.getItem());
            return block != Blocks.air && this.input.isInstance(block);
        }
        else
            return this.input.isInstance(stack.getItem());
    }

    @Override
    public String toStringName()
    {
        return "Any " + this.input.getSimpleName() + ".class";
    }

    @Override
    public int getSortOrder()
    {
        return 400;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        return super.compareTo(other);
    }

}
