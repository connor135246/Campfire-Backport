package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

public abstract class GenericCustomInput
{

    protected Object input;

    /** the type of input. 1 = <code>ItemStack</code>, 2 = oredict id, 3 = tool class, 4 = <code>Class</code>, 5 = NBT */
    private byte inputType = -1;
    /** the size of the input. */
    private int inputSize;
    /** whether {@link #inputSize} should be checked when matching (only true for {@link CampfireStateChanger CampfireStateChangers} that are not damageable) */
    private boolean inputSizeMatters;
    /** extra data associated with this input. if input is NBT, this is a copy of input. */
    private NBTTagCompound extraData;
    /** the type of extra/input data. 0 = exact tag, 1 = enchantment, 2 = fluid, 3 = tinkers */
    private byte dataType = -1;
    /** whether or not meta was specified. meta can only be specified by <code>ItemStack</code> inputs. */
    private boolean metaSpecified = false;
    /** the types of campfires that this input applies to. */
    private EnumCampfireType types = EnumCampfireType.NEITHER;
    /** the list of <code>ItemStacks</code> to be used for NEI displaying and for making dispenser behaviours. */
    private List<ItemStack> inputList = new ArrayList<ItemStack>();
    /** lines of text to use for displaying extra info in NEI. */
    private List<String> neiTooltip = new LinkedList<String>();
    /** an additional modifier to the sorting order. it's 100 by default. smaller numbers = less priority. */
    private int sortPriority = 100;

    /**
     * Sets <code>GenericCustomInput</code> values based on custom input.
     * 
     * @param parsed
     *            - the <code>Object[]</code> received from {@link StringParsers}
     * @param types
     *            - {@link #types}
     * @param inputSizeMatters
     *            - {@link #inputSizeMatters}
     * @param clamp
     *            - clamps {@link #inputSize} to be between 1 and this number. if it's less than 1, doesn't clamp.
     */
    public void customGInput(Object[] parsed, EnumCampfireType types, boolean inputSizeMatters, int clamp)
    {
        if (parsed[0] instanceof Item)
        {
            this.input = new ItemStack((Item) parsed[0], 1, (Integer) parsed[2]);
            this.inputType = 1;
            this.metaSpecified = (Integer) parsed[2] != OreDictionary.WILDCARD_VALUE;
        }
        else if (parsed[0] instanceof Integer)
        {
            this.input = (Integer) parsed[0];
            this.inputType = 2;
        }
        else if (parsed[0] instanceof String)
        {
            this.input = (String) parsed[0];
            this.inputType = 3;
        }
        else if (parsed[0] instanceof Class)
        {
            this.input = (Class) parsed[0];
            this.inputType = 4;
        }
        else if (!((NBTTagCompound) parsed[3]).hasNoTags())
        {
            this.inputType = 5;
        }
        else
            return;

        this.inputSize = (Integer) parsed[1];
        if (clamp > 0)
            inputSize = MathHelper.clamp_int(inputSize, 1, clamp);

        this.extraData = ((NBTTagCompound) parsed[3]).hasNoTags() ? null : (NBTTagCompound) parsed[3];

        if (hasExtraData())
        {
            this.dataType = this.extraData.getByte(StringParsers.GCI_DATATYPE);
            this.extraData.removeTag(StringParsers.GCI_DATATYPE);
            if (isDataInput())
                this.input = (NBTTagCompound) this.extraData.copy();
        }

        this.types = types;

        if (getDataType() != 3)
            this.inputSizeMatters = inputSizeMatters;
        else
            this.inputSizeMatters = false;

        prepareInputListAndNEIStuff();
    }

    /**
     * Sets <code>GenericCustomInput</code> values for Auto Recipe Discovery <code>CampfireRecipe</code> inputs.
     * 
     * @param stack
     *            - the input <code>ItemStack</code>
     * @param types
     *            - the campfire types this input applies to
     */
    public void autoGInput(ItemStack stack, EnumCampfireType types)
    {
        this.input = stack;
        this.inputType = 1;
        this.metaSpecified = stack.getItemDamage() != OreDictionary.WILDCARD_VALUE;
        this.types = types;
        this.inputSize = 1;
        this.inputSizeMatters = false;

        prepareInputListAndNEIStuff();
    }

    /**
     * Called after initializing the <code>GenericCustomInput</code>.<br>
     * Sets {@link #inputList} and often {@link #neiTooltip} as well.
     */
    private void prepareInputListAndNEIStuff()
    {
        if (isItemInput())
        {
            ItemStack inputStack = ItemStack.copyItemStack((ItemStack) getInput());

            ItemStack listStack;
            if (metaWasSpecified())
            {
                listStack = new ItemStack(inputStack.getItem(), doesInputSizeMatter() ? getInputSize() : 1, inputStack.getItemDamage());
                if (hasExtraData())
                    listStack.setTagCompound((NBTTagCompound) getExtraData().copy());
            }
            else
                listStack = inputStack;

            inputList.add(listStack);
        }
        else if (isOreDictInput())
        {
            for (ItemStack stack : OreDictionary.getOres(OreDictionary.getOreName((Integer) getInput())))
            {
                ItemStack listStack = ItemStack.copyItemStack(stack);
                listStack.stackSize = doesInputSizeMatter() ? getInputSize() : 1;
                if (hasExtraData())
                    listStack.setTagCompound((NBTTagCompound) getExtraData().copy());
                inputList.add(listStack);
            }

            neiTooltip.add(EnumChatFormatting.GOLD
                    + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.ore_input", OreDictionary.getOreName((Integer) getInput())));
        }
        else if (isToolInput())
        {
            for (Item item : GameData.getItemRegistry().typeSafeIterable())
            {
                if (item.getToolClasses(new ItemStack(item)).contains((String) getInput()))
                    inputList.add(new ItemStack(item, 1, 0));
            }

            neiTooltip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.tool_input", (String) getInput()));
        }
        else if (isClassInput())
        {
            if (Block.class.isAssignableFrom((Class) getInput()))
            {
                for (Block block : GameData.getBlockRegistry().typeSafeIterable())
                {
                    if (((Class) getInput()).isAssignableFrom(block.getClass()))
                        inputList.add(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
                }
            }
            else
            {
                for (Item item : GameData.getItemRegistry().typeSafeIterable())
                {
                    if (((Class) getInput()).isAssignableFrom(item.getClass()))
                        inputList.add(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
                }
            }

            neiTooltip.add(EnumChatFormatting.GOLD
                    + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.class_input", ((Class) getInput()).getSimpleName()));
        }
        else if (isDataInput())
        {
            ItemStack listStack = new ItemStack(Items.written_book, doesInputSizeMatter() && getDataType() != 3 ? getInputSize() : 1);

            if (getDataType() != 3)
                listStack.setTagCompound((NBTTagCompound) getExtraData().copy());

            inputList.add(listStack);
        }

        // tool and class inputs may have empty inputLists at this point, which is a problem, probably
        if (inputList.isEmpty())
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".inputerror.invalid_tool_or_class"));
            input = null;
            return;
        }

        if (hasExtraData())
        {
            switch (getDataType())
            {
            case 3:
            {
                NBTTagCompound ginputData = getExtraData();
                NBTTagList keyList = ginputData.getTagList(StringParsers.KEYSET, 8);
                NBTTagList typeList = ginputData.getTagList(StringParsers.TYPESET, 8);

                if (keyList.tagCount() > 0)
                    neiTooltip.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".nei.tinkers_mods"));

                for (int i = 0; i < typeList.tagCount(); ++i)
                {
                    String key = keyList.getStringTagAt(i);
                    String type = typeList.getStringTagAt(i);

                    Object value = null;

                    if (type.equals(StringParsers.INT_PREFIX))
                    {
                        value = ginputData.getInteger(key);
                    }
                    else if (type.equals(StringParsers.BYTE_PREFIX))
                    {
                        value = ginputData.getByte(key);
                    }
                    else if (type.equals(StringParsers.FLOAT_PREFIX))
                    {
                        value = ginputData.getFloat(key);
                    }
                    else if (type.equals(StringParsers.INTARRAY_PREFIX))
                    {
                        value = ginputData.getIntArray(key)[0];
                    }

                    String tip = translateTinkersNBTForDisplay(key, value);
                    if (!tip.isEmpty())
                        neiTooltip.add(tip);
                }
                
                neiTooltip.add("");
                
                break;
            }
            case 2:
            {
                NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.FLUID_CAPS);
                int fluidAmount = fluidData.getInteger(StringParsers.AMOUNT_CAPS);
                String fluidName = fluidData.getString(StringParsers.FLUIDNAME);

                FluidStack fluid = new FluidStack(FluidRegistry.getFluid(fluidName), fluidAmount);
                neiTooltip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.fluid_data",
                        fluidAmount, fluid.getLocalizedName()));
                break;
            }
            case 1:
            {
                NBTTagCompound ench = getExtraData().getTagList(StringParsers.ENCH, 10).getCompoundTagAt(0);
                int level = ench.getInteger(StringParsers.LVL);
                if (Enchantment.enchantmentsList[ench.getInteger(StringParsers.ID)].getMaxLevel() > level)
                {
                    neiTooltip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.enchantment_data",
                            StatCollector.translateToLocal("enchantment.level." + level)));
                }
                break;
            }
            default:
            {
                neiTooltip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocal(Reference.MODID + ".nei.nbt_data"));
                neiTooltip.add(EnumChatFormatting.ITALIC + getExtraData().toString());
                break;
            }
            }
        }
    }

    private static String translateTinkersNBTForDisplay(String key, Object value)
    {
        String returned = "";
        final String not = EnumChatFormatting.UNDERLINE + "NOT" + EnumChatFormatting.RESET + " ";

        if (key.equals("Shoddy"))
        {
            if ((Float) value > 0.0F)
                returned = EnumChatFormatting.GRAY + "Stonebound Modifier: " + EnumChatFormatting.AQUA + (Float) value + EnumChatFormatting.GRAY
                        + " or greater";
            else if ((Float) value < 0.0F)
                returned = EnumChatFormatting.GREEN + "Jagged Modifier: " + EnumChatFormatting.AQUA + Math.abs((Float) value) + EnumChatFormatting.GRAY
                        + " or greater";
            else
                returned = not + EnumChatFormatting.GRAY + "Stonebound" + EnumChatFormatting.RESET + " or " + EnumChatFormatting.GREEN + "Jagged";
        }
        else if (key.equals("Knockback"))
        {
            returned = EnumChatFormatting.DARK_GRAY + "Knockback";
            if ((Float) value > 0.0F)
                returned += ": " + EnumChatFormatting.GREEN + (Float) value + EnumChatFormatting.GRAY + " or greater";
            else if ((Float) value < 0.0F)
                returned += ": " + EnumChatFormatting.GREEN + (Float) value + EnumChatFormatting.GRAY + " or smaller";
            else
                returned = not + returned;
        }
        else if (key.equals("HarvestLevel"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GRAY + "Harvest Level: " + EnumChatFormatting.GREEN + (Integer) value + EnumChatFormatting.GRAY + " or greater";
        }
        else if (key.equals("MiningSpeed"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GRAY + "Mining Speed: " + EnumChatFormatting.GREEN + (Integer) value + EnumChatFormatting.GRAY + " or greater";
        }
        else if (key.equals("Moss"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GREEN + "Auto-Repair";
        }
        else if (key.equals("Unbreaking"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GRAY + "Reinforced";
            if ((Integer) value > 1)
                returned += " " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater";
        }
        else if (key.equals("Necrotic"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.DARK_GRAY + "Life Steal";
            if ((Integer) value > 1)
                returned += " " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater";
        }
        else if (key.equals("Beheading"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.LIGHT_PURPLE + "Beheading";
            if ((Integer) value > 1)
                returned += " " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater";
        }
        else if (key.equals("SilkTouch"))
        {
            returned = EnumChatFormatting.YELLOW + "Silky";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Emerald"))
        {
            returned = EnumChatFormatting.GREEN + "Durability +50%";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Diamond"))
        {
            returned = EnumChatFormatting.AQUA + "Durability +500";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Lava"))
        {
            returned = EnumChatFormatting.DARK_RED + "Auto-Smelt";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Broken"))
        {
            returned = EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + "Broken";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Flux"))
        {
            returned = EnumChatFormatting.GRAY + "Uses RF";
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (key.equals("Fiery"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GOLD + "Fiery";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / " + ((((Integer) value) / 25) + 1) * 25 + ")";
        }
        else if (key.equals("ModAntiSpider"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.GREEN + "Bane of Arthropods";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / " + ((((Integer) value) / 4) + 1) * 4 + ")";
        }
        else if (key.equals("Redstone"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.RED + "Haste";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / " + ((((Integer) value) / 50) + 1) * 50 + ")";
        }
        else if (key.equals("ModSmite"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.YELLOW + "Smite";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / " + ((((Integer) value) / 36) + 1) * 36 + ")";
        }
        else if (key.equals("ModAttack"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.WHITE + "Sharpness";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / " + ((((Integer) value) / 72) + 1) * 72 + ")";
        }
        else if (key.equals("Lapis"))
        {
            if ((Integer) value > 0)
                returned = EnumChatFormatting.BLUE + "Luck";
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater / 450)";
        }
        else if (value instanceof Integer)
        {
            if ((Integer) value > 0)
                returned = key;
            if ((Integer) value > 1)
                returned += " (" + (Integer) value + " or greater)";
        }
        else if (value instanceof Byte)
        {
            returned = key;
            if ((Byte) value == 0)
                returned = not + returned;
        }
        else if (value instanceof Float)
        {
            if ((Float) value > 0.0F)
                returned = key + ": " + (Float) value + " or greater";
        }
        else
            returned = key;

        return returned;
    }

    // Static Methods
    /**
     * For checking if the player's <code>ItemStack</code> matches a given GenericCustomInput.
     * 
     * @param ginput
     * @param stack
     * @return true if the given GenericCustomInput applies to the given ItemStack, false otherwise
     */
    public static boolean matches(GenericCustomInput ginput, ItemStack stack)
    {
        boolean matches;

        switch (ginput.getInputType())
        {
        case 1:
            matches = matchesTheStack(ginput, stack);
            break;
        case 2:
            matches = matchesTheOre(ginput, stack);
            break;
        case 3:
            matches = matchesTheTool(ginput, stack);
            break;
        case 4:
            matches = matchesTheClass(ginput, stack);
            break;
        case 5:
            matches = true;
            break;
        default:
            matches = false;
            break;
        }

        if (ginput.hasExtraData())
            matches = matches && matchesData(ginput, stack);

        if (ginput.doesInputSizeMatter())
            matches = matches && stack.stackSize >= ginput.getInputSize();

        return matches;
    }

    public static boolean matchesTheStack(GenericCustomInput ginput, ItemStack stack)
    {
        return ginput.metaWasSpecified() ? stack.isItemEqual((ItemStack) ginput.getInput())
                : ((ItemStack) ginput.getInput()).getItem() == stack.getItem();
    }

    public static boolean matchesTheOre(GenericCustomInput ginput, ItemStack stack)
    {
        return matchesTheOre((Integer) ginput.getInput(), stack);
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

    private static boolean matchesTheTool(GenericCustomInput ginput, ItemStack stack)
    {
        return stack.getItem().getToolClasses(stack).contains((String) ginput.getInput());
    }

    public static boolean matchesTheClass(GenericCustomInput ginput, ItemStack stack)
    {
        return matchesTheClass((Class) ginput.getInput(), stack);
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

    public static boolean matchesData(GenericCustomInput ginput, ItemStack stack)
    {
        if (stack.hasTagCompound() && ginput.hasExtraData())
        {
            NBTTagCompound data = ginput.getExtraData();

            switch (ginput.getDataType())
            {
            case 1:
            {
                NBTTagCompound ench = data.getTagList(StringParsers.ENCH, 10).getCompoundTagAt(0);
                return EnchantmentHelper.getEnchantmentLevel(ench.getInteger(StringParsers.ID), stack) >= ench.getInteger(StringParsers.LVL);
            }
            case 2:
            {
                String ginputFluid = ginput.getExtraData().getCompoundTag(StringParsers.FLUID_CAPS).getString(StringParsers.FLUIDNAME);
                int ginputFluidAmount = ginput.getExtraData().getCompoundTag(StringParsers.FLUID_CAPS).getInteger(StringParsers.AMOUNT_CAPS);
                Object[] stackFluidData = getFluidData(stack.getTagCompound());

                if (!ginputFluid.isEmpty() && !((String) stackFluidData[2]).isEmpty())
                {
                    if (((String) stackFluidData[2]).equals(ginputFluid)
                            && ((Integer) stackFluidData[4]) >= ginputFluidAmount)
                    {
                        return true;
                    }
                }
                break;
            }
            case 3:
            {
                NBTTagCompound stackData = stack.getTagCompound().getCompoundTag(StringParsers.INFITOOL);
                if (!stackData.hasNoTags())
                {
                    NBTTagCompound ginputData = ginput.getExtraData();
                    NBTTagList keyList = ginputData.getTagList(StringParsers.KEYSET, 8);
                    NBTTagList typeList = ginputData.getTagList(StringParsers.TYPESET, 8);

                    for (int i = 0; i < typeList.tagCount(); ++i)
                    {
                        String key = keyList.getStringTagAt(i);
                        String type = typeList.getStringTagAt(i);

                        if (type.equals(StringParsers.INT_PREFIX))
                        {
                            if (stackData.getInteger(key) < ginputData.getInteger(key))
                                return false;
                        }
                        else if (type.equals(StringParsers.BYTE_PREFIX))
                        {
                            if (stackData.getByte(key) != ginputData.getByte(key))
                                return false;
                        }
                        else if (type.equals(StringParsers.FLOAT_PREFIX))
                        {
                            if (ginputData.getFloat(key) < 0.0F)
                            {
                                if (stackData.getFloat(key) > ginputData.getFloat(key))
                                    return false;
                            }
                            else if (ginputData.getFloat(key) > 0.0F)
                            {
                                if (stackData.getFloat(key) < ginputData.getFloat(key))
                                    return false;
                            }
                            else
                            {
                                if (stackData.getFloat(key) != ginputData.getFloat(key))
                                    return false;
                            }
                        }
                        else if (type.equals(StringParsers.INTARRAY_PREFIX))
                        {
                            if ((stackData.hasKey(key) ? stackData.getIntArray(key)[0] : 0) < ginputData.getIntArray(key)[0])
                                return false;
                        }
                    }
                    return true;
                }
                break;
            }
            default:
            {
                return stack.getTagCompound().equals(data);
            }
            }

        }
        return false;
    }

    /**
     * Gets the fluid data out of an <code>NBTTagCompound</code>. There are three ways (that I know of) that fluid data can be stored. The first one is the suggested Forge format,
     * which I assume is used with most mods (tested with TE and TiC). The second one is the format used by IE's jerrycans. The third one is the format used by IE's wooden/metal
     * barrels.
     * 
     * @param data
     * @return an <code>Object[]</code> where the first element is the key for the fluid compound tag, the second element is the key for the fluid name, the third element is the
     *         fluid name, the fourth element is the key for the fluid amount, and the fifth element is the fluid amount.
     */
    public static Object[] getFluidData(NBTTagCompound data)
    {
        String dataFluidKey = "";
        String dataFluidNameKey = "";
        String dataFluidName = "";
        String dataFluidAmountKey = "";
        int dataFluidAmount = -1;

        if (data.hasKey(StringParsers.FLUID_CAPS))
        {
            dataFluidKey = StringParsers.FLUID_CAPS;
            dataFluidNameKey = StringParsers.FLUIDNAME;
            dataFluidName = data.getCompoundTag(StringParsers.FLUID_CAPS).getString(StringParsers.FLUIDNAME);
            dataFluidAmountKey = StringParsers.AMOUNT_CAPS;
            dataFluidAmount = data.getCompoundTag(StringParsers.FLUID_CAPS).getInteger(StringParsers.AMOUNT_CAPS);
        }
        else if (data.hasKey(StringParsers.FLUID_LOWER))
        {
            dataFluidKey = StringParsers.FLUID_LOWER;
            dataFluidNameKey = StringParsers.FLUID_LOWER;
            dataFluidName = data.getCompoundTag(StringParsers.FLUID_LOWER).getString(StringParsers.FLUID_LOWER);
            dataFluidAmountKey = StringParsers.AMOUNT_LOWER;
            dataFluidAmount = data.getCompoundTag(StringParsers.FLUID_LOWER).getInteger(StringParsers.AMOUNT_LOWER);
        }
        else if (data.hasKey(StringParsers.TANK))
        {
            dataFluidKey = StringParsers.TANK;
            dataFluidNameKey = StringParsers.FLUIDNAME;
            dataFluidName = data.getCompoundTag(StringParsers.TANK).getString(StringParsers.FLUIDNAME);
            dataFluidAmountKey = StringParsers.AMOUNT_CAPS;
            dataFluidAmount = data.getCompoundTag(StringParsers.TANK).getInteger(StringParsers.AMOUNT_CAPS);
        }

        return new Object[] { dataFluidKey, dataFluidNameKey, dataFluidName, dataFluidAmountKey, dataFluidAmount };
    }

    /**
     * Tries to empty the stack's fluid NBT by <code>GenericCustomInput</code>'s fluid NBT.
     * 
     * @param ginput
     * @param stack
     * @return true if it worked, false if it didn't
     */
    public static boolean doFluidEmptying(GenericCustomInput ginput, ItemStack stack)
    {

        if (stack.hasTagCompound() && ginput.hasExtraData() && ginput.getDataType() == 2)
        {
            String ginputFluid = ginput.getExtraData().getCompoundTag(StringParsers.FLUID_CAPS).getString(StringParsers.FLUIDNAME);
            int ginputFluidAmount = ginput.getExtraData().getCompoundTag(StringParsers.FLUID_CAPS).getInteger(StringParsers.AMOUNT_CAPS);
            Object[] stackFluidData = getFluidData(stack.getTagCompound());

            if (!ginputFluid.isEmpty() && !((String) stackFluidData[2]).isEmpty())
            {
                if (((String) stackFluidData[2]).equals(ginputFluid)
                        && ((Integer) stackFluidData[4]) >= ginputFluidAmount)
                {
                    if (((Integer) stackFluidData[4] - ginputFluidAmount) <= 0)
                    {
                        stack.getTagCompound().removeTag((String) stackFluidData[0]);
                        if (stack.getTagCompound().hasNoTags())
                            stack.setTagCompound(null);
                    }
                    else
                    {
                        stack.getTagCompound().getCompoundTag((String) stackFluidData[0]).setInteger((String) stackFluidData[3],
                                (Integer) stackFluidData[4] - ginputFluidAmount);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Meant for comparing two <code>GenericCustomInputs</code> whose {@link #isItemInput()} is true, the first of which is from Auto Recipe Discovery and so won't have any
     * {@link #extraData}. The order they're given matters!
     * 
     * @param ginputAuto
     * @param ginputCustom
     * @return true if the <code>GenericCustomInputs</code> are the same, false if they aren't
     */
    public static boolean doStackRecipesMatch(GenericCustomInput ginputAuto, GenericCustomInput ginputCustom)
    {
        return !ginputCustom.hasExtraData() && matchesTheStack(ginputCustom, (ItemStack) ginputAuto.getInput());
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        String inputToString = "[";
        if (isDataInput())
        {
            if (getDataType() != 3)
                inputToString += "Anything";
            else
                inputToString += "Any Tinker's Construct tool";
        }
        else
        {
            if (isOreDictInput())
                inputToString += "Ore: " + OreDictionary.getOreName((Integer) getInput());
            else if (isToolInput())
                inputToString += "Any " + getInput() + "-type tool";
            else if (isItemInput() && (metaWasSpecified() || anIffyCheckToJustifyImprovedReadability()))
                inputToString += ((ItemStack) getInput()).getDisplayName();
            else if (isItemInput())
                inputToString += ((ItemStack) getInput()).getDisplayName() + " (any metadata)";
            else if (isClassInput())
                inputToString += "Any " + ((Class) getInput()).getSimpleName() + ".class";
        }

        if (hasExtraData())
            inputToString += " with ";

        if (getDataType() == 0)
            inputToString += "NBT:" + getExtraData();
        else if (getDataType() == 1)
        {
            NBTTagCompound ench = getExtraData().getTagList(StringParsers.ENCH, 10).getCompoundTagAt(0);
            inputToString += Enchantment.enchantmentsList[ench.getInteger(StringParsers.ID)].getTranslatedName(ench.getInteger(StringParsers.LVL))
                    + " or greater";
        }
        else if (getDataType() == 2)
        {
            NBTTagCompound fluidData = getExtraData().getCompoundTag(StringParsers.FLUID_CAPS);
            inputToString += "at least " + fluidData.getInteger(StringParsers.AMOUNT_CAPS) + " mB of " + fluidData.getString(StringParsers.FLUIDNAME);
        }
        else if (getDataType() == 3)
        {
            inputToString += "some specific modifiers. Check NEI.";
        }

        return inputToString + "]";
    }

    /**
     * only used by {@link #toString()}
     */
    public boolean anIffyCheckToJustifyImprovedReadability()
    {
        return ((new ItemStack(((ItemStack) getInput()).getItem(), 1, 0)).getDisplayName()
                .equals(new ItemStack(((ItemStack) getInput()).getItem(), 1, 1).getDisplayName()));
    }

    // Getters and Setters

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

    public boolean hasExtraData()
    {
        return extraData != null;
    }

    public boolean isMultiInput()
    {
        return inputSize > 1;
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

    public List<String> getNEITooltip()
    {
        return neiTooltip;
    }

    public boolean metaWasSpecified()
    {
        return metaSpecified;
    }

    public EnumCampfireType getTypes()
    {
        return types;
    }

    public boolean doesInputSizeMatter()
    {
        return inputSizeMatters;
    }

    public List<ItemStack> getInputList()
    {
        return inputList;
    }

    public int getSortPriority()
    {
        return sortPriority;
    }

    public void setSortPriority(int sortPriority)
    {
        this.sortPriority = sortPriority;
    }

    // Sorting
    public int compareTo(GenericCustomInput ginput)
    {
        int value = 0;

        // inputs with isMultiInput = true should go MUCH MUCH closer to the start of the list.
        value -= 4 * Boolean.compare(this.isMultiInput(), ginput.isMultiInput());
        // inputs with extra data (that aren't data inputs) should go MUCH closer to the start of the list.
        value -= 2 * Boolean.compare(this.hasExtraData() && !this.isDataInput(), ginput.hasExtraData() && !ginput.isDataInput());
        // inputs with doesMetaMatter = true should go closer to the start of the list.
        value -= Boolean.compare(this.metaWasSpecified(), ginput.metaWasSpecified());
        // inputs with a greater input type should go MUCH MUCH MUCH closer to the end of the list.
        value += 8 * Integer.compare(this.getInputType(), ginput.getInputType());
        // recipes with a smaller sort priority should go MUCH MUCH MUCH MUCH closer to the end of the list.
        value -= 16 * Integer.compare(this.getSortPriority(), ginput.getSortPriority());

        return value;
    }

}
