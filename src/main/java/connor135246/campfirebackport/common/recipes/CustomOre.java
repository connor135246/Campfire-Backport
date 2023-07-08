package connor135246.campfirebackport.common.recipes;

import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

public class CustomOre extends CustomInput<String>
{

    public CustomOre(String ore, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        super(ore, inputSize, data, inputSizeMatters, clamp);

        // ore inputs may be empty, which is a problem, probably
        if (OreDictionary.getOres(ore, false).isEmpty())
            CampfireBackportConfig.possiblyInvalidOres.add(ore);

        neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("ore_input", ore));

        addDataTooltips();
    }

    @Override
    public boolean matchesStack(ItemStack stack)
    {
        for (int id : OreDictionary.getOreIDs(stack))
        {
            if (this.input.equals(OreDictionary.getOreName(id)))
                return true;
        }
        return false;
    }

    @Override
    public String toStringName()
    {
        return "Ore: " + this.input;
    }

    /**
     * CustomOres don't cache their entries ahead of time.
     */
    @Override
    public List<ItemStack> getInputList()
    {
        return OreDictionary.getOres(this.input, false);
    }

    @Override
    public int getSortOrder()
    {
        return 200;
    }

    @Override
    public int compareTo(CustomInput other)
    {
        return super.compareTo(other);
    }

}
