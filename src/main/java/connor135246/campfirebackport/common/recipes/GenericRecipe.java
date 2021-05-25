package connor135246.campfirebackport.common.recipes;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
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
    /** an additional modifier to the sorting order. 100 is default. smaller numbers = less priority. */
    protected final int sortPriority;

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs)
    {
        this(types, inputs, outputs, 100);
    }

    protected GenericRecipe(EnumCampfireType types, CustomInput[] inputs, @Nullable ItemStack[] outputs, int sortPriority)
    {
        this.types = types;
        this.inputs = inputs;
        this.outputs = outputs;
        this.sortPriority = sortPriority;
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

            if (cinput.isIIngredientInput())
                stack = ((ICraftTweakerIngredient) cinput.getInput()).applyTransform(stack, player);
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
                    // an odd thing we have to do, but ultimately this simulates how {@link net.minecraft.inventory.SlotCrafing#onPickupFromSlot} does it
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
