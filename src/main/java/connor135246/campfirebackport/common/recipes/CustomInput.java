package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public abstract class CustomInput<T> implements Comparable<CustomInput>
{

    /** the input! */
    protected final T input;

    /** the size of the input. (always 1 for a {@link CampfireRecipe}) */
    protected final int inputSize;
    /** whether {@link #inputSize} should be checked when matching. (only true for a {@link CampfireStateChanger} that is not damageable) */
    protected final boolean inputSizeMatters;
    /** extra data associated with this input. if input is NBT, this is a copy of input. */
    protected final NBTTagCompound extraData;
    /** the type of extra/input data. 1 = exact tag, 2 = enchantment, 3 = fluid, 4 = tinkers */
    protected final byte dataType;
    /** the list of ItemStacks to be used for NEI displaying (and for making dispenser behaviours for a {@link CampfireStateChanger}). not used by oredict inputs. */
    protected List<ItemStack> inputList = new ArrayList<ItemStack>();
    /** so that nei tooltips can be translated on the fly. */
    protected List<Consumer<LinkedList<String>>> neiTooltipFillers = new LinkedList<Consumer<LinkedList<String>>>();

    /**
     * Returns a new CustomInput using a recipe from config.
     * 
     * @param parsed
     *            - the Object[] received from {@link StringParsers}
     */
    public static CustomInput createFromParsed(Object[] parsed, boolean inputSizeMatters, int clamp) throws Exception
    {
        Object input = parsed[0];
        int inputSize = (Integer) parsed[1];
        int inputMeta = (Integer) parsed[2];
        @Nullable
        NBTTagCompound data = (NBTTagCompound) parsed[3];
        if (data != null)
            data = (NBTTagCompound) data.copy();

        if (input instanceof Item)
            return new CustomItemStack((Item) input, inputMeta, inputSize, data, inputSizeMatters, clamp);
        else if (input instanceof String)
        {
            String stringInput = (String) input;
            if (stringInput.startsWith("ore:"))
                return new CustomOre(stringInput.substring(4), inputSize, data, inputSizeMatters, clamp);
            else if (stringInput.startsWith("tool:"))
                return new CustomTool(stringInput.substring(5), inputSize, data, inputSizeMatters, clamp);
            else
                throw new Exception();
        }
        else if (input instanceof Class)
            return new CustomClass((Class) input, inputSize, data, inputSizeMatters, clamp);
        else if (input instanceof ICraftTweakerIngredient)
            return new CustomCraftTweakerIngredient((ICraftTweakerIngredient) input, inputSize, data, inputSizeMatters, clamp);
        else if (data != null)
            return new CustomData(inputSize, data, inputSizeMatters, clamp);
        else
            throw new Exception();
    }

    /**
     * Returns a new CustomInput for Auto Recipe Discovery CampfireRecipe inputs.
     * 
     * @param stack
     *            - the input ItemStack
     */
    public static CustomInput createAuto(ItemStack stack) throws Exception
    {
        return new CustomItemStack(stack.getItem(), stack.getItemDamage(), 1, null, false, -1);
    }

    public CustomInput(T input, int inputSize, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        this.input = input;

        this.inputSize = clamp > 0 ? MathHelper.clamp_int(inputSize, 1, clamp) : inputSize;

        if (data != null && !data.hasNoTags())
        {
            this.dataType = data.getByte(StringParsers.KEY_GCIDataType);
            data.removeTag(StringParsers.KEY_GCIDataType);
            this.extraData = (NBTTagCompound) data.copy();
        }
        else
        {
            this.dataType = 0;
            this.extraData = null;
        }

        if (getDataType() == 4)
            this.inputSizeMatters = false;
        else
            this.inputSizeMatters = inputSizeMatters;
    }

    /**
     * Should be called at the very end of a custom input constructor.
     */
    protected void finishTooltips()
    {
        switch (getDataType())
        {
        case 4:
        {
            NBTTagCompound cinputData = getExtraData();
            NBTTagList keyList = cinputData.getTagList(StringParsers.KEY_KeySet, 8);
            NBTTagList typeList = cinputData.getTagList(StringParsers.KEY_TypeSet, 8);

            if (keyList.tagCount() > 0)
                neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.ITALIC + StringParsers.translateNEI("tinkers_mods")));

            for (int i = 0; i < keyList.tagCount(); ++i)
            {
                String key = keyList.getStringTagAt(i);
                String type = typeList.getStringTagAt(i);
                Object value = null;

                if (type.equals(StringParsers.KEY_INT_PREFIX))
                    value = cinputData.getInteger(key);
                else if (type.equals(StringParsers.KEY_BYTE_PREFIX))
                    value = cinputData.getByte(key);
                else if (type.equals(StringParsers.KEY_FLOAT_PREFIX))
                    value = cinputData.getFloat(key);
                else if (type.equals(StringParsers.KEY_INTARRAY_PREFIX))
                    value = cinputData.getIntArray(key)[0];
                else
                    continue;

                String tip = StringParsers.convertTinkersNBTForDisplay(key, value);
                if (!tip.isEmpty())
                    neiTooltipFillers.add((list) -> list.add(tip));
            }

            neiTooltipFillers.add((list) -> list.add(""));
            break;
        }
        case 3:
        {
            NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.KEY_Fluid);
            int fluidAmount = fluidData.getInteger(StringParsers.KEY_Amount);

            FluidStack fluid = new FluidStack(FluidRegistry.getFluid(fluidData.getString(StringParsers.KEY_FluidName)), fluidAmount);
            neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("fluid_data", fluidAmount, fluid.getLocalizedName())));
            break;
        }
        case 2:
        {
            NBTTagCompound ench = getExtraData().getTagList(StringParsers.KEY_ench, 10).getCompoundTagAt(0);
            int enchid = ench.getInteger(StringParsers.KEY_id);
            int enchlvl = ench.getInteger(StringParsers.KEY_lvl);
            neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("enchantment_data", Enchantment.enchantmentsList[enchid].getTranslatedName(enchlvl))));
            break;
        }
        case 1:
        {
            String string = getExtraData().toString();
            neiTooltipFillers.add((list) -> {
                String firstLinePrefix = EnumChatFormatting.GOLD + StringParsers.translateNEI("exact_nbt_data") + " ";
                String otherLinePrefix = EnumChatFormatting.GOLD + "   ";
                list.addAll(StringParsers.lineifyString(string, ",", firstLinePrefix, otherLinePrefix, 50));
            });
            break;
        }
        }

        if (!neiTooltipFillers.isEmpty())
            neiTooltipFillers.add((list) -> list.add(0, ""));
    }

    /**
     * For checking if the ItemStack matches this input, ignoring data.
     */
    public abstract boolean matchesStack(ItemStack stack);

    public boolean matches(ItemStack stack)
    {
        boolean matches = stack != null;

        if (matches)
        {
            if (doesInputSizeMatter())
                matches = stack.stackSize >= getInputSize();

            if (matches)
            {
                matches = matchesStack(stack);

                if (matches)
                {
                    if (hasExtraData())
                        matches = matchesData(stack);
                }
            }
        }
        return matches;
    }

    public boolean matchesData(ItemStack stack)
    {
        if (hasExtraData())
        {
            NBTTagCompound cinputData = getExtraData();

            switch (getDataType())
            {
            case 1:
            {
                if (!stack.hasTagCompound())
                    break;

                return stack.getTagCompound().equals(cinputData);
            }
            case 2:
            {
                if (!stack.hasTagCompound())
                    break;

                NBTTagCompound ench = cinputData.getTagList(StringParsers.KEY_ench, 10).getCompoundTagAt(0);
                return EnchantmentHelper.getEnchantmentLevel(ench.getInteger(StringParsers.KEY_id), stack) >= ench.getInteger(StringParsers.KEY_lvl);
            }
            case 3:
            {
                NBTTagCompound cinputFluidData = cinputData.getCompoundTag(StringParsers.KEY_Fluid);
                return MiscUtil.containsFluid(stack, cinputFluidData.getString(StringParsers.KEY_FluidName), cinputFluidData.getInteger(StringParsers.KEY_Amount));
            }
            case 4:
            {
                if (!stack.hasTagCompound())
                    break;

                NBTTagCompound stackData = stack.getTagCompound().getCompoundTag(StringParsers.KEY_InfiTool);
                if (!stackData.hasNoTags())
                {
                    NBTTagList keyList = cinputData.getTagList(StringParsers.KEY_KeySet, 8);
                    NBTTagList typeList = cinputData.getTagList(StringParsers.KEY_TypeSet, 8);

                    for (int i = 0; i < typeList.tagCount(); ++i)
                    {
                        String key = keyList.getStringTagAt(i);
                        String type = typeList.getStringTagAt(i);

                        if (type.equals(StringParsers.KEY_INT_PREFIX))
                        {
                            if (stackData.getInteger(key) < cinputData.getInteger(key))
                                return false;
                        }
                        else if (type.equals(StringParsers.KEY_BYTE_PREFIX))
                        {
                            if (stackData.getByte(key) != cinputData.getByte(key))
                                return false;
                        }
                        else if (type.equals(StringParsers.KEY_FLOAT_PREFIX))
                        {
                            if (cinputData.getFloat(key) < 0.0F)
                            {
                                if (stackData.getFloat(key) > cinputData.getFloat(key))
                                    return false;
                            }
                            else if (cinputData.getFloat(key) > 0.0F)
                            {
                                if (stackData.getFloat(key) < cinputData.getFloat(key))
                                    return false;
                            }
                            else
                            {
                                if (stackData.getFloat(key) != cinputData.getFloat(key))
                                    return false;
                            }
                        }
                        else if (type.equals(StringParsers.KEY_INTARRAY_PREFIX))
                        {
                            if ((stackData.hasKey(key) ? stackData.getIntArray(key)[0] : 0) < cinputData.getIntArray(key)[0])
                                return false;
                        }
                    }
                    return true;
                }
                break;
            }
            }
        }
        return false;
    }

    /**
     * Modifies an input stack for displaying in NEI.
     */
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (stack != null)
        {
            if (doesInputSizeMatter())
                stack.stackSize = getInputSize();

            if (hasExtraData())
            {
                if (getDataType() == 1 || getDataType() == 2)
                    stack.setTagCompound(MiscUtil.mergeNBT(stack.getTagCompound(), getExtraData()));
                else if (getDataType() == 3)
                    stack = MiscUtil.fillContainerWithFluid(stack, FluidStack.loadFluidStackFromNBT(getExtraData().getCompoundTag(StringParsers.KEY_Fluid)));
            }
        }
        return stack;
    }

    /**
     * If the stack is an IFluidContainerItem, drains the stack's fluid by the amount and returns it. <br>
     * If the stack has a container item, reduces the stack's size by 1 and returns it. (It's assumed that container items will be taken care of elsewhere) <br>
     * If the stack is in the FluidContainerRegistry, reduces the stack's size by 1, then if the stack's size is now zero, returns the empty container. If not, gives the player the
     * empty container.
     */
    public static ItemStack doFluidEmptying(ItemStack stack, int amount, EntityPlayer player)
    {
        if (stack != null && player != null && amount > 0)
        {
            if (stack.getItem() instanceof IFluidContainerItem)
            {
                ((IFluidContainerItem) stack.getItem()).drain(stack, amount, true);
                GenericRecipe.returnContainer = false; // there are some items that are an IFluidContainerItem AND have a container item.
            }
            else if (stack.getItem().hasContainerItem(stack))
                stack.stackSize--;
            else
            {
                ItemStack emptyContainer = FluidContainerRegistry.drainFluidContainer(stack);
                if (emptyContainer != null)
                {
                    stack.stackSize--;
                    if (stack.stackSize <= 0)
                        stack = emptyContainer;
                    else if (!player.inventory.addItemStackToInventory(emptyContainer))
                        player.dropPlayerItemWithRandomChoice(emptyContainer, false);
                }
            }
        }
        return stack;
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        try
        {
            StringBuilder inputToString = new StringBuilder();

            inputToString.append("[");

            inputToString.append(toStringName());

            if (hasExtraData())
                inputToString.append(" with ");

            switch (getDataType())
            {
            case 1:
            {
                inputToString.append("NBT:" + getExtraData());
                break;
            }
            case 2:
            {
                NBTTagCompound ench = getExtraData().getTagList(StringParsers.KEY_ench, 10).getCompoundTagAt(0);
                inputToString.append(Enchantment.enchantmentsList[ench.getInteger(StringParsers.KEY_id)]
                        .getTranslatedName(ench.getInteger(StringParsers.KEY_lvl)) + " or greater");
                break;
            }
            case 3:
            {
                NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.KEY_Fluid);
                inputToString.append("at least " + fluidData.getInteger(StringParsers.KEY_Amount) +
                        " mB of " + fluidData.getString(StringParsers.KEY_FluidName));
                break;
            }
            case 4:
            {
                inputToString.append("some specific modifiers (check NEI)");
                break;
            }
            }

            return inputToString.append("]" + (getInputSize() > 1 ? " x " + getInputSize() : "")).toString();
        }
        catch (Exception excep)
        {
            // CommonProxy.modlog.error("Error while attempting to get an item's name: " + excep.getClass().getName() + ": " + excep.getLocalizedMessage());
            return "[ERROR GETTING NAME]";
        }
    }

    /**
     * @return input name for toString
     */
    public abstract String toStringName();

    // Getters

    public T getInput()
    {
        return input;
    }

    public boolean hasExtraData()
    {
        return extraData != null;
    }

    public int getInputSize()
    {
        return inputSize;
    }

    public NBTTagCompound getExtraData()
    {
        return extraData;
    }

    public byte getDataType()
    {
        return dataType;
    }

    public LinkedList<String> createNEITooltips()
    {
        LinkedList<String> list = new LinkedList<String>();
        neiTooltipFillers.forEach((filler) -> filler.accept(list));
        return list;
    }

    public boolean doesInputSizeMatter()
    {
        return inputSizeMatters;
    }

    public List<ItemStack> getInputList()
    {
        return inputList;
    }

    // Sorting

    /**
     * Smaller numbers have higher priority.
     */
    public abstract int getSortOrder();

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Override
    public int compareTo(CustomInput other)
    {
        // orders inputs by their input type, which generally speaking puts more specific inputs at the start.
        int value = Integer.compare(this.getSortOrder(), other.getSortOrder());
        if (value != 0)
            return value;
        // inputs that have extra data come first.
        return Boolean.compare(other.hasExtraData(), this.hasExtraData());
    }

}
