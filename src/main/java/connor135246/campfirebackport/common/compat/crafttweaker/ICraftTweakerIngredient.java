package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * An interface for checking if an ItemStack matches a CraftTweaker IIngredient, which may or may not be loaded.
 */
public interface ICraftTweakerIngredient
{

    public boolean matches(ItemStack stack, boolean inputSizeMatters);

    public List<ItemStack> getItems();

    public LinkedList<String> getNEITooltip();

    public boolean isSimple();

}
