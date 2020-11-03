package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * For when CraftTweaker isn't loaded.
 */
public class DummyCraftTweakerIngredient implements ICraftTweakerIngredient
{

    public DummyCraftTweakerIngredient(Object unused)
    {
        ;
    }

    @Override
    public boolean matches(ItemStack stack, boolean inputSizeMatters)
    {
        return false;
    }

    @Override
    public List<ItemStack> getItems()
    {
        return new ArrayList<ItemStack>(0);
    }

    @Override
    public LinkedList<String> getNEITooltip()
    {
        return new LinkedList<String>();
    }

    @Override
    public boolean isSimple()
    {
        return true;
    }

}
