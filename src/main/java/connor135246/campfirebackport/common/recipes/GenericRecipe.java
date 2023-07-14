package connor135246.campfirebackport.common.recipes;

import java.util.Arrays;

import javax.annotation.Nullable;

import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

public abstract class GenericRecipe
{

    /** the types of campfires this applies to. probably shouldn't be NEITHER. */
    protected final EnumCampfireType types;
    /** the inputs! */
    protected final CustomInput[] inputs;
    /** the outputs! may not exist. */
    protected final ItemStack[] outputs;
    /** an additional modifier to the sorting order. larger number = later in the list. */
    protected final int sortOrder;

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs)
    {
        this(types, inputs, outputs, 0);
    }

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs, int sortOrder)
    {
        this.types = types;
        Arrays.sort(inputs);
        this.inputs = inputs;
        this.outputs = outputs;
        this.sortOrder = sortOrder;
    }

    /**
     * To be called for each input when the recipe is being used. Doesn't have anything to do with {@link #outputs}, though.
     * 
     * @param cinputIndex
     *            - the index of the {@link #inputs} being used
     * @param stack
     *            - the stack that applies to this input
     * @param player
     */
    public ItemStack onUsingInput(int cinputIndex, ItemStack stack, EntityPlayer player)
    {
        if (cinputIndex >= 0 && cinputIndex < getInputs().length)
        {
            returnContainer = true;

            CustomInput cinput = getInputs()[cinputIndex];

            // yes we do apply crafttweaker transforms BEFORE running {@link #use(CustomInput, ItemStack, EntityPlayer)}.
            // that's how {@link net.minecraft.inventory.SlotCrafing#onPickupFromSlot} does it.
            if (cinput instanceof CustomCraftTweakerIngredient)
                stack = ((CustomCraftTweakerIngredient) cinput).getInput().applyTransform(stack, player);
            else if (cinput.hasExtraData() && cinput.getDataType() == 3)
                stack = CustomInput.doFluidEmptying(stack, cinput.getExtraData().getCompoundTag(StringParsers.KEY_Fluid).getInteger(StringParsers.KEY_Amount), player);

            if (stack != null)
            {
                stack = useAndHandleContainer(cinput, stack, player);

                if (stack.stackSize < 0)
                    stack.stackSize = 0;
            }
        }

        return stack;
    }

    /**
     * There are some scenarios where we don't want to return a container. This value can be changed to do just that. <br>
     * Unfortunately, I can't do the same for regular crafting recipes...
     */
    public static boolean returnContainer = true;

    /**
     * Does {@link #use(CustomInput, ItemStack, EntityPlayer)}. <br>
     * If {@link #returnContainer} is true, checks if the stack has a container item. If the container item is broken, posts the PlayerDestroyItemEvent. If the stack's size is
     * zero, returns the container item. Otherwise, adds it to the player's inventory.
     */
    protected ItemStack useAndHandleContainer(CustomInput cinput, ItemStack stack, EntityPlayer player)
    {
        if (returnContainer && stack != null && stack.getItem().hasContainerItem(stack))
        {
            ItemStack containerStack = stack.getItem().getContainerItem(stack);
            if (containerStack != null)
            {
                if (containerStack.isItemStackDamageable() && containerStack.getItemDamage() > containerStack.getMaxDamage())
                    ForgeEventFactory.onPlayerDestroyItem(player, containerStack);
                else if (!MiscUtil.putStackInExistingSlots(player.inventory, containerStack, true))
                {
                    // it looks odd to copy the stack here, but ultimately this simulates how {@link net.minecraft.inventory.SlotCrafing#onPickupFromSlot} does it.
                    if (use(cinput, stack.copy(), player).stackSize <= 0)
                        return containerStack;
                    else if (!player.inventory.addItemStackToInventory(containerStack))
                        player.dropPlayerItemWithRandomChoice(containerStack, false);
                }
            }
        }
        return use(cinput, stack, player);
    }

    /**
     * The stack is used. Usually that means it should be reduced in stack size.
     */
    protected abstract ItemStack use(CustomInput cinput, ItemStack stack, EntityPlayer player);

    /**
     * for {@link #toString()}
     */
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

    public int getSortOrder()
    {
        return sortOrder;
    }

    // Sorting
    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    public int compareTo(GenericRecipe grecipe)
    {
        // recipes with a smaller sort order come first.
        int value = Integer.compare(this.getSortOrder(), grecipe.getSortOrder());
        if (value != 0)
            return value;
        // recipes with more inputs come first.
        value = Integer.compare(grecipe.getInputs().length, this.getInputs().length);
        if (value != 0)
            return value;
        // recipes compare the order of their first inputs.
        value = this.getInputs()[0].compareTo(grecipe.getInputs()[0]);
        if (value != 0)
            return value;
        // BOTH comes before REG_ONLY comes before SOUL_ONLY comes before NEITHER.
        return this.getTypes().compareTo(grecipe.getTypes());
    }

}
