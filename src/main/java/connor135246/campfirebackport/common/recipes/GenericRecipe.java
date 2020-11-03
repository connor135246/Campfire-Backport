package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import connor135246.campfirebackport.util.EnumCampfireType;
import net.minecraft.item.ItemStack;

public abstract class GenericRecipe
{

    /** the types of campfires this applies to. probably shouldn't be NEITHER. */
    protected final EnumCampfireType types;
    /** the inputs! */
    protected final CustomInput[] inputs;
    /** the outputs! may not exist. */
    protected final ItemStack[] outputs;
    /** an additional modifier to the sorting order. 100 is default. smaller numbers = less priority. */
    protected final int sortPriority;

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs)
    {
        this.types = types;
        this.inputs = inputs;
        this.outputs = outputs;
        this.sortPriority = 100;
    }

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs, int sortPriority)
    {
        this.types = types;
        this.inputs = inputs;
        this.outputs = outputs;
        this.sortPriority = sortPriority;
    }

    // Static Methods

    public static String stackToString(ItemStack stack)
    {
        return "[" + stack.getItem().getItemStackDisplayName(stack)
                + (stack.hasTagCompound() ? " with NBT:" + stack.getTagCompound() : "")
                + "]" + (stack.stackSize > 1 ? " x " + stack.stackSize : "");
    }

    // Getters

    public CustomInput[] getInputs()
    {
        return inputs;
    }

    public boolean isMultiInput()
    {
        return inputs.length > 1;
    }

    public ItemStack[] getOutputs()
    {
        return outputs;
    }

    public boolean hasOutputs()
    {
        return outputs != null && outputs.length > 0;
    }

    public EnumCampfireType getTypes()
    {
        return types;
    }

    public int getSortPriority()
    {
        return sortPriority;
    }

    // Sorting
    public int compareTo(GenericRecipe grecipe)
    {
        int value = 0;

        // recipes with higher priority inputs should go closer to the start of the list.
        value += this.getInputs()[0].compareTo(grecipe.getInputs()[0]);
        // recipes with more inputs should go MUCH closer to the start of the list.
        value -= 16 * Integer.compare(getInputs().length, grecipe.getInputs().length);
        // recipes with a smaller sort priority should go MUCH MUCH closer to the end of the list.
        value -= 100 * Integer.compare(this.getSortPriority(), grecipe.getSortPriority());

        return value;
    }

}
