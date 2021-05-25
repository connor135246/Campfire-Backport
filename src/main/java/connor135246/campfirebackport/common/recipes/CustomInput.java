package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import connor135246.campfirebackport.common.dispenser.BehaviourGeneric;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

public class CustomInput implements Comparable<CustomInput>
{

    /** the input! */
    protected final Object input;

    /**
     * the type of input. 1 = ItemStack, 2 = oredict id (Integer), 3 = tool class (String), 4 = Class, 5 = NBTTagCompound, 6 = an ICraftTweakerIngredient that should be an
     * {@link connor135246.campfirebackport.common.compat.crafttweaker.ActiveCraftTweakerIngredient ActiveCraftTweakerIngredient}
     */
    protected final byte inputType;
    /** the size of the input. (always 1 for a {@link CampfireRecipe}) */
    protected final int inputSize;
    /** whether {@link #inputSize} should be checked when matching. (only true for a {@link CampfireStateChanger} that is not damageable) */
    protected final boolean inputSizeMatters;
    /** extra data associated with this input. if input is NBT, this is a copy of input. */
    protected final NBTTagCompound extraData;
    /** the type of extra/input data. 1 = exact tag, 2 = enchantment, 3 = fluid, 4 = tinkers */
    protected final byte dataType;
    /** whether or not meta was specified. only applies to ItemStack inputs. */
    protected final boolean metaSpecified;
    /** the list of ItemStacks to be used for NEI displaying (and for making dispenser behaviours for a {@link CampfireStateChanger}). */
    protected List<ItemStack> inputList = new ArrayList<ItemStack>();
    /** lines of text to use for displaying extra info in NEI. */
    protected LinkedList<String> neiTooltip = new LinkedList<String>();

    /**
     * Returns a new CustomInput using a recipe from config.
     * 
     * @param parsed
     *            - the Object[] received from {@link StringParsers}
     */
    public static CustomInput createFromParsed(Object[] parsed, boolean inputSizeMatters, int clamp) throws Exception
    {
        return new CustomInput(parsed[0], (Integer) parsed[1], (Integer) parsed[2], (NBTTagCompound) parsed[3], inputSizeMatters, clamp);
    }

    /**
     * Returns a new CustomInput for Auto Recipe Discovery CampfireRecipe inputs.
     * 
     * @param stack
     *            - the input ItemStack
     */
    public static CustomInput createAuto(ItemStack stack) throws Exception
    {
        return new CustomInput(stack.getItem(), 1, stack.getItemDamage(), null, false, -1);
    }

    /**
     * Creates a CustomInput.
     * 
     * @param input
     *            - must be an Item, Integer, String, Class, null, or ICraftTweakerIngredient
     * @param inputSize
     * @param inputMeta
     * @param data
     * @param inputSizeMatters
     *            - {@link #inputSizeMatters}
     * @param clamp
     *            - clamps {@link #inputSize} to be between 1 and this number. if it's less than 1, doesn't clamp.
     */
    public CustomInput(Object input, int inputSize, int inputMeta, @Nullable NBTTagCompound data, boolean inputSizeMatters, int clamp) throws Exception
    {
        this.inputSize = clamp > 0 ? MathHelper.clamp_int(inputSize, 1, clamp) : inputSize;

        this.extraData = data == null || data.hasNoTags() ? null : (NBTTagCompound) data.copy();

        if (hasExtraData())
        {
            this.dataType = this.extraData.getByte(StringParsers.KEY_GCIDataType);
            this.extraData.removeTag(StringParsers.KEY_GCIDataType);
        }
        else
            this.dataType = 0;

        if (getDataType() == 4)
            this.inputSizeMatters = false;
        else
            this.inputSizeMatters = inputSizeMatters;

        ItemStack listStack;
        if (input instanceof Item)
        {
            this.input = new ItemStack((Item) input, 1, inputMeta);
            this.inputType = 1;
            this.metaSpecified = inputMeta != OreDictionary.WILDCARD_VALUE;

            inputList.add(ItemStack.copyItemStack((ItemStack) getInput()));
        }
        else
        {
            this.metaSpecified = false;

            if (input instanceof Integer)
            {
                for (ItemStack stack : OreDictionary.getOres(OreDictionary.getOreName((Integer) input), false))
                    inputList.add(new ItemStack(stack.getItem(), 1, stack.getItemDamage()));

                // ore inputs may have empty inputLists at this point, which is a problem, probably
                if (inputList.isEmpty())
                {
                    ConfigReference.logError("no_matches_ore", OreDictionary.getOreName((Integer) input));
                    throw new Exception();
                }

                this.input = (Integer) input;
                this.inputType = 2;

                neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("ore_input", OreDictionary.getOreName((Integer) getInput())));
            }
            else if (input instanceof String)
            {
                for (Item item : GameData.getItemRegistry().typeSafeIterable())
                {
                    listStack = new ItemStack(item);
                    if (item.getToolClasses(listStack).contains((String) input))
                    {
                        if (item.isDamageable())
                            listStack.setItemDamage(OreDictionary.WILDCARD_VALUE);
                        inputList.add(listStack);
                    }
                }

                // tool inputs may have empty inputLists at this point, which is a problem, probably
                if (inputList.isEmpty())
                {
                    ConfigReference.logError("no_matches_tool", (String) input);
                    throw new Exception();
                }

                this.input = (String) input;
                this.inputType = 3;

                neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("tool_input", (String) input));
            }
            else if (input instanceof Class)
            {
                if (Block.class.isAssignableFrom((Class) input))
                {
                    for (Block block : GameData.getBlockRegistry().typeSafeIterable())
                        if (((Class) input).isAssignableFrom(block.getClass()))
                            inputList.add(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
                }
                else
                {
                    for (Item item : GameData.getItemRegistry().typeSafeIterable())
                        if (((Class) input).isAssignableFrom(item.getClass()))
                            inputList.add(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
                }

                // class inputs may have empty inputLists at this point, which is a problem, probably
                if (inputList.isEmpty())
                {
                    ConfigReference.logError("no_matches_class", ((Class) input).getCanonicalName());
                    throw new Exception();
                }

                this.input = (Class) input;
                this.inputType = 4;

                neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("class_input", ((Class) getInput()).getSimpleName()));
            }
            else
            {
                listStack = new ItemStack(Items.written_book);

                if (input instanceof ICraftTweakerIngredient)
                {
                    this.input = (ICraftTweakerIngredient) input;
                    this.inputType = 6;

                    inputList.addAll(((ICraftTweakerIngredient) getInput()).getItems());
                    if (inputList.isEmpty())
                    {
                        String name = StringParsers.translateNEI(((ICraftTweakerIngredient) getInput()).isWildcard() ? "anything" : "unknown");

                        neiTooltip.add(EnumChatFormatting.GOLD + name);

                        listStack.setStackDisplayName(EnumChatFormatting.ITALIC + "<" + name + ">");

                        inputList.add(listStack);
                    }

                    neiTooltip.addAll(((ICraftTweakerIngredient) getInput()).getNEITooltip());
                }
                else if (hasExtraData())
                {
                    this.input = (NBTTagCompound) this.extraData.copy();
                    this.inputType = 5;

                    String name = StringParsers.translateNEI(getDataType() != 4 ? "anything" : "any_tinkers");

                    if (getDataType() != 4)
                        neiTooltip.add(EnumChatFormatting.GOLD + name);

                    listStack.setStackDisplayName(EnumChatFormatting.ITALIC + "<" + name + ">");

                    inputList.add(listStack);
                }
                else
                    throw new Exception();
            }
        }

        switch (getDataType())
        {
        case 4:
        {
            NBTTagCompound cinputData = getExtraData();
            NBTTagList keyList = cinputData.getTagList(StringParsers.KEY_KeySet, 8);
            NBTTagList typeList = cinputData.getTagList(StringParsers.KEY_TypeSet, 8);

            if (keyList.tagCount() > 0)
                neiTooltip.add(EnumChatFormatting.ITALIC + StringParsers.translateNEI("tinkers_mods"));

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
                    neiTooltip.add(tip);
            }

            neiTooltip.add("");
            break;
        }
        case 3:
        {
            NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.KEY_Fluid);
            int fluidAmount = fluidData.getInteger(StringParsers.KEY_Amount);

            FluidStack fluid = new FluidStack(FluidRegistry.getFluid(fluidData.getString(StringParsers.KEY_FluidName)), fluidAmount);
            neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("fluid_data", fluidAmount, fluid.getLocalizedName()));
            break;
        }
        case 2:
        {
            NBTTagCompound ench = getExtraData().getTagList(StringParsers.KEY_ench, 10).getCompoundTagAt(0);
            neiTooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("enchantment_data",
                    Enchantment.enchantmentsList[ench.getInteger(StringParsers.KEY_id)].getTranslatedName(ench.getInteger(StringParsers.KEY_lvl))));
            break;
        }
        case 1:
        {
            String firstLinePrefix = EnumChatFormatting.GOLD + StringParsers.translateNEI("exact_nbt_data") + " ";
            String otherLinePrefix = EnumChatFormatting.GOLD + "   ";
            neiTooltip.addAll(StringParsers.lineifyString(getExtraData().toString(), ",", firstLinePrefix, otherLinePrefix, 50));
            break;
        }
        }

        if (!neiTooltip.isEmpty())
            neiTooltip.add(0, "");
    }

    /**
     * For checking if the player's ItemStack matches this CustomInput.
     * 
     * @return true if this CustomInput applies to the given ItemStack, false otherwise
     */
    public boolean matches(ItemStack stack)
    {
        return matches(this, stack);
    }

    public static boolean matches(CustomInput cinput, ItemStack stack)
    {
        boolean matches = stack != null;

        if (matches)
        {
            if (cinput.doesInputSizeMatter())
                matches = stack.stackSize >= cinput.getInputSize();

            if (matches)
            {
                switch (cinput.getInputType())
                {
                case 1:
                    matches = matchesTheStack(cinput, stack);
                    break;
                case 2:
                    matches = matchesTheOre(cinput, stack);
                    break;
                case 3:
                    matches = matchesTheTool(cinput, stack);
                    break;
                case 4:
                    matches = matchesTheClass(cinput, stack);
                    break;
                case 5:
                    matches = true;
                    break;
                case 6:
                    matches = matchesTheIIngredient(cinput, stack);
                    break;
                default:
                    matches = false;
                    break;
                }

                if (matches)
                {
                    if (cinput.hasExtraData())
                        matches = matchesData(cinput, stack);
                }
            }
        }
        return matches;
    }

    public static boolean matchesTheStack(CustomInput cinput, ItemStack stack)
    {
        return cinput.metaWasSpecified() ? stack.isItemEqual((ItemStack) cinput.getInput())
                : ((ItemStack) cinput.getInput()).getItem() == stack.getItem();
    }

    public static boolean matchesTheOre(CustomInput cinput, ItemStack stack)
    {
        return matchesTheOre((Integer) cinput.getInput(), stack);
    }

    public static boolean matchesTheOre(int oreID, ItemStack stack)
    {
        for (int id : OreDictionary.getOreIDs(stack))
        {
            if (oreID == id)
                return true;
        }
        return false;
    }

    public static boolean matchesTheTool(CustomInput cinput, ItemStack stack)
    {
        return stack.getItem().getToolClasses(stack).contains((String) cinput.getInput());
    }

    public static boolean matchesTheClass(CustomInput cinput, ItemStack stack)
    {
        return matchesTheClass((Class) cinput.getInput(), stack);
    }

    public static boolean matchesTheClass(Class clazz, ItemStack stack)
    {
        if (Block.class.isAssignableFrom(clazz))
        {
            Block block = Block.getBlockFromItem(stack.getItem());
            return clazz.isInstance(block);
        }
        else
            return clazz.isInstance(stack.getItem());
    }

    public static boolean matchesTheIIngredient(CustomInput cinput, ItemStack stack)
    {
        return ((ICraftTweakerIngredient) cinput.getInput()).matches(stack, cinput.doesInputSizeMatter());
    }

    public static boolean matchesData(CustomInput cinput, ItemStack stack)
    {
        if (cinput.hasExtraData())
        {
            NBTTagCompound cinputData = cinput.getExtraData();

            switch (cinput.getDataType())
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
                return containsFluid(stack, cinputFluidData.getString(StringParsers.KEY_FluidName), cinputFluidData.getInteger(StringParsers.KEY_Amount));
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
     * Returns a new tag that has merged the tags of the base with the merger using {@link #mergeNBT(NBTTagCompound, String, NBTBase)}.
     */
    public static NBTTagCompound mergeNBT(final NBTTagCompound base, final NBTTagCompound merger)
    {
        if (base == null)
            return (NBTTagCompound) merger.copy();

        if (merger != null)
        {
            try
            {
                NBTTagCompound returnTag = (NBTTagCompound) base.copy();

                Iterator iterator = merger.func_150296_c().iterator();
                while (iterator.hasNext())
                {
                    String mergerKey = (String) iterator.next();
                    NBTBase mergerTag = merger.getTag(mergerKey).copy();
                    mergeNBT(returnTag, mergerKey, mergerTag);
                }

                return returnTag;
            }
            catch (ClassCastException excep)
            {
                // CommonProxy.modlog.error("Error while attempting to merge NBT: " + excep.getClass().getName() + ": " + excep.getLocalizedMessage());
            }
        }

        return (NBTTagCompound) base.copy();
    }

    /**
     * Merges the merger into the base with the key. <br>
     * If the merger is a compound, its tags are recursively added to the base's compound. <br>
     * If the merger is a list of the same type as the base's list, its tags are appended to the base's list. <br>
     * Otherwise, the merger replaces the value in the base.
     */
    public static NBTTagCompound mergeNBT(final NBTTagCompound base, final String key, NBTBase merger) throws ClassCastException
    {
        NBTBase baseTag = base.getTag(key);

        if (baseTag == null || baseTag.getId() != merger.getId() || (baseTag.getId() != 9 && baseTag.getId() != 10)
                || (baseTag.getId() == 10 && ((NBTTagCompound) baseTag).hasNoTags()))
        {
            base.setTag(key, merger);
        }
        else if (baseTag.getId() == 10)
        {
            Iterator iterator = ((NBTTagCompound) merger).func_150296_c().iterator();
            while (iterator.hasNext())
            {
                String mergerKey = (String) iterator.next();
                NBTBase mergerTag = ((NBTTagCompound) merger).getTag(mergerKey).copy();
                mergeNBT((NBTTagCompound) baseTag, mergerKey, mergerTag);
            }
        }
        else if (baseTag.getId() == 9 && ((NBTTagList) merger).tagCount() > 0)
        {
            if (((NBTTagList) baseTag).func_150303_d() != ((NBTTagList) merger).func_150303_d())
                base.setTag(key, merger);
            else
            {
                for (int i = 0; i < ((NBTTagList) merger).tagCount(); ++i)
                    ((NBTTagList) baseTag).appendTag(((NBTTagList) merger).removeTag(i));
            }
        }

        return base;
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
                    stack.setTagCompound(mergeNBT(stack.getTagCompound(), getExtraData()));
                else if (getDataType() == 3)
                    stack = fillContainerWithFluid(stack, FluidStack.loadFluidStackFromNBT(getExtraData().getCompoundTag(StringParsers.KEY_Fluid)));
            }

            if (isIIngredientInput())
                stack = ((ICraftTweakerIngredient) getInput()).modifyStackForDisplay(stack);
        }
        return stack;
    }

    /**
     * @return true if the stack contains the fluid given by name and amount
     */
    public static boolean containsFluid(ItemStack stack, String name, int amount)
    {
        if (stack != null && name != null)
        {
            Fluid fluid = FluidRegistry.getFluid(name);
            if (fluid != null)
                return containsFluid(stack, new FluidStack(fluid, amount));
        }
        return false;
    }

    /**
     * @return true if the stack contains the fluidStack
     */
    public static boolean containsFluid(ItemStack stack, FluidStack fluidStack)
    {
        if (stack != null && fluidStack != null)
        {
            if (stack.getItem() instanceof IFluidContainerItem)
            {
                FluidStack result = ((IFluidContainerItem) stack.getItem()).drain(stack, fluidStack.amount, false);
                return result != null && result.containsFluid(fluidStack);
            }
            else
                return FluidContainerRegistry.containsFluid(stack, fluidStack);
        }
        return false;
    }

    /**
     * @return a copy of the fluid container after it's been filled with the fluidStack
     */
    public static ItemStack fillContainerWithFluid(ItemStack stack, FluidStack fluidStack)
    {
        try
        {
            if (fluidStack != null && stack != null && stack.getItem() instanceof IFluidContainerItem)
            {
                FluidStack currentFluid = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
                if (currentFluid == null || currentFluid.isFluidEqual(fluidStack))
                {
                    ItemStack modifiedStack = stack.copy();
                    ((IFluidContainerItem) stack.getItem()).fill(modifiedStack, fluidStack, true);
                    return modifiedStack;
                }
            }
        }
        catch (Exception excep)
        {
            // CommonProxy.modlog.error("Error while attempting to fill a fluid container: " + excep.getClass().getName() + ": " + excep.getLocalizedMessage());
        }

        return stack;
    }

    /**
     * If the stack is an IFluidContainerItem, drains the stack's fluid by the amount and returns it. <br>
     * If the stack has a container item, reduces the stack's size by 1 and returns it. (It's assumed that container items will be taken care of elsewhere) <br>
     * If the stack is in the FluidContainerRegistry, reduces the stack's size by 1, then if the stack's size is now zero, returns the empty container. If not, gives the player the
     * emptyContainer.
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
                    if (!BehaviourGeneric.putStackInExistingSlots(player.inventory, emptyContainer, true))
                    {
                        if (stack.stackSize <= 0)
                            stack = emptyContainer;
                        else if (!player.inventory.addItemStackToInventory(emptyContainer))
                            player.dropPlayerItemWithRandomChoice(emptyContainer, false);
                    }
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

            if (isDataInput())
            {
                if (getDataType() == 4)
                    inputToString.append("A Tinker's Construct tool");
                else
                    inputToString.append("Anything");
            }
            else
            {
                if (isOreDictInput())
                    inputToString.append("Ore: " + OreDictionary.getOreName((Integer) getInput()));
                else if (isToolInput())
                    inputToString.append("Any " + getInput() + "-type tool");
                else if (isItemInput() && (metaWasSpecified() || anIffyCheckToJustifyImprovedReadability()))
                    inputToString.append(((ItemStack) getInput()).getItem().getItemStackDisplayName((ItemStack) getInput()));
                else if (isItemInput())
                    inputToString.append(((ItemStack) getInput()).getItem().getItemStackDisplayName((ItemStack) getInput()) + " (any metadata)");
                else if (isClassInput())
                    inputToString.append("Any " + ((Class) getInput()).getSimpleName() + ".class");
                else if (isIIngredientInput())
                    inputToString.append("Unknown CraftTweaker IIngredient (check NEI)");
            }

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
                inputToString
                        .append(Enchantment.enchantmentsList[ench.getInteger(StringParsers.KEY_id)].getTranslatedName(ench.getInteger(StringParsers.KEY_lvl))
                                + " or greater");
                break;
            }
            case 3:
            {
                NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.KEY_Fluid);
                inputToString
                        .append("at least " + fluidData.getInteger(StringParsers.KEY_Amount) + " mB of " + fluidData.getString(StringParsers.KEY_FluidName));
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
     * only used by {@link #toString()}
     */
    public boolean anIffyCheckToJustifyImprovedReadability()
    {
        ItemStack input0 = new ItemStack(((ItemStack) getInput()).getItem(), 1, 0);
        ItemStack input1 = new ItemStack(((ItemStack) getInput()).getItem(), 1, 1);
        return input0.getItem().getItemStackDisplayName(input0).equals(input1.getItem().getItemStackDisplayName(input1));
    }

    // Getters

    public Object getInput()
    {
        return input;
    }

    public byte getInputType()
    {
        return inputType;
    }

    public boolean isItemInput()
    {
        return inputType == 1;
    }

    public boolean isOreDictInput()
    {
        return inputType == 2;
    }

    public boolean isToolInput()
    {
        return inputType == 3;
    }

    public boolean isClassInput()
    {
        return inputType == 4;
    }

    public boolean isDataInput()
    {
        return inputType == 5;
    }

    public boolean isIIngredientInput()
    {
        return inputType == 6;
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

    public LinkedList<String> getNEITooltip()
    {
        return neiTooltip;
    }

    public boolean metaWasSpecified()
    {
        return metaSpecified;
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
    @Override
    public int compareTo(CustomInput cinput)
    {
        int value = 0;

        // inputs with extra data (that aren't data inputs) should go MUCH closer to the start of the list.
        value -= 2 * Boolean.compare(this.hasExtraData() && !this.isDataInput(), cinput.hasExtraData() && !cinput.isDataInput());
        // inputs with doesMetaMatter = true should go closer to the start of the list.
        value -= Boolean.compare(this.metaWasSpecified(), cinput.metaWasSpecified());
        // inputs with a greater input type should go MUCH MUCH closer to the end of the list.
        value += 4 * Integer.compare(this.getInputType(), cinput.getInputType());

        return value;
    }

}
