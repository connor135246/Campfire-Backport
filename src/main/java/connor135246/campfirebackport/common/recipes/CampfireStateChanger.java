package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.dispenser.BehaviourGeneric;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.ForgeEventFactory;

public class CampfireStateChanger extends GenericRecipe implements Comparable<CampfireStateChanger>
{

    public static final String STACKABLE = "stackable", DAMAGEABLE = "damageable", NONE = "none";

    /** dispensable CampfireStateChangers work from a dispenser. */
    protected final boolean dispensable;
    /** true for left click actions, false for right click actions. */
    protected final boolean leftClick;
    /** true for extinguishers, false for ignitors. */
    protected final boolean extinguisher;
    /** controls how the input is consumed. 1 = stackable, 2 = damageable, 0 = none. */
    protected final byte usageType;

    /** the master list of state changers! */
    private static List<CampfireStateChanger> masterStateChangerList = new ArrayList<CampfireStateChanger>();
    /** the state changers that are left clicked. copied from the master list. */
    private static List<CampfireStateChanger> leftStateChangerList = new ArrayList<CampfireStateChanger>();
    /** the state changers that are right clicked. copied from the master list. */
    private static List<CampfireStateChanger> rightStateChangerList = new ArrayList<CampfireStateChanger>();

    /** the list of state changers created with CraftTweaker. */
    private static List<CampfireStateChanger> crafttweakerStateChangerList = new ArrayList<CampfireStateChanger>();

    /**
     * Converts a string into a CampfireStateChanger.
     * 
     * @param stateChanger
     *            - a string in the proper format (see config explanation)
     * @param types
     *            - the types of campfire this is being added to
     * @param extinguisher
     *            - true if this extinguishes, false if this ignites
     */
    public static CampfireStateChanger createCustomStateChanger(String stateChanger, EnumCampfireType types, boolean extinguisher)
    {
        try
        {
            String[] segment = stateChanger.split("/");

            // left click

            String[] use = segment[0].split("\\+");

            boolean leftClick = use[0].equals("left");

            // usage type

            String[] afterUse = segment[2].split(">");

            byte usageType;

            if (afterUse[0].equals(NONE))
                usageType = 0;
            else if (afterUse[0].equals(STACKABLE))
                usageType = 1;
            else if (afterUse[0].equals(DAMAGEABLE))
                usageType = 2;
            else
                throw new Exception();

            boolean damageable = usageType == 2;

            // outputs
            ItemStack[] outputs = null;

            if (afterUse.length > 1)
            {
                Object[] replaceWith = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(afterUse[1], false);

                outputs = new ItemStack[] {
                        new ItemStack((Item) replaceWith[0], MathHelper.clamp_int((Integer) replaceWith[1], 1, 64), (Integer) replaceWith[2]) };

                if (!((NBTTagCompound) replaceWith[3]).hasNoTags())
                {
                    outputs[0].setTagCompound((NBTTagCompound) replaceWith[3]);
                    outputs[0].stackTagCompound.removeTag(StringParsers.KEY_GCIDataType);
                }
            }

            // input

            CustomInput cinput = CustomInput.createFromParsed(StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[1], true), !damageable,
                    !damageable ? 64 : -1);

            // dispensable

            boolean dispensable = false;

            if (use.length > 1)
            {
                if (!(cinput instanceof CustomData))
                    dispensable = true;
                else
                    ConfigReference.logError("invalid_dispensable", extinguisher ? ConfigReference.extinguisher() : ConfigReference.ignitor(), stateChanger);
            }

            // done!

            return new CampfireStateChanger(types, new CustomInput[] { cinput }, leftClick, extinguisher, usageType, outputs, dispensable, 0);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    public CampfireStateChanger(EnumCampfireType types, CustomInput[] inputs, boolean leftClick, boolean extinguisher, byte usageType,
            @Nullable ItemStack[] outputs, boolean dispensable, int sortOrder)
    {
        super(types, inputs, outputs, sortOrder);

        this.leftClick = leftClick;
        this.extinguisher = extinguisher;
        this.usageType = (byte) MathHelper.clamp_int(usageType, 0, 2);
        this.dispensable = dispensable;

        // damageable tooltip

        if (isUsageTypeDamageable())
        {
            if (getInput().neiTooltipFillers.isEmpty())
                getInput().neiTooltipFillers.add((list) -> list.add(""));

            int damage = getInput().getInputSize();
            getInput().neiTooltipFillers.add((list) -> list.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("damage_by", damage)));
        }

        // register dispensables (only on initial load)

        if (this.dispensable && CampfireBackportConfig.initialLoad)
        {
            for (ItemStack stack : getInput().getInputList())
            {
                if (!CampfireBackportConfig.dispenserBlacklistItems.contains(stack.getItem()))
                    BlockDispenser.dispenseBehaviorRegistry.putObject(stack.getItem(), new BehaviourGeneric(this));
            }
        }
    }

    /**
     * Tries to make a CampfireStateChanger based on user input for the given campfire types and use and add it the state changer lists.<br>
     * See {@link #createCustomStateChanger}.
     * 
     * @param recipe
     *            - the user-input string that represents a state changer
     * @param types
     *            - the types of campfires this state changer should apply to
     * @param extinguisher
     *            - true if this extinguishers, false if this ignites
     * @return true if the state changer was added successfully, false if it wasn't
     */
    public static boolean addToStateChangerLists(String stateChanger, EnumCampfireType types, boolean extinguisher)
    {
        CampfireStateChanger cstate = createCustomStateChanger(stateChanger, types, extinguisher);

        boolean added = addToStateChangerLists(cstate);
        if (!added)
            ConfigReference.logError("invalid_state_changer", extinguisher ? ConfigReference.extinguisher() : ConfigReference.ignitor(), stateChanger);

        return added;
    }

    /**
     * Adds the given CampfireStateChanger to the state changer lists, if it's not null.
     * 
     * @return true if the state changer isn't null, false otherwise
     */
    public static boolean addToStateChangerLists(CampfireStateChanger cstate)
    {
        if (cstate != null)
        {
            masterStateChangerList.add(cstate);

            if (cstate.leftClick)
                leftStateChangerList.add(cstate);
            else
                rightStateChangerList.add(cstate);

            return true;
        }
        else
            return false;
    }

    /**
     * Removes the given CampfireStateChanger from the state changer lists, if it's not null.
     */
    public static void removeFromStateChangerLists(CampfireStateChanger cstate)
    {
        if (cstate != null)
        {
            masterStateChangerList.remove(cstate);

            if (cstate.leftClick)
                leftStateChangerList.remove(cstate);
            else
                rightStateChangerList.remove(cstate);
        }
    }

    /**
     * Sorts the three main recipe lists.
     */
    public static void sortStateChangerLists()
    {
        Collections.sort(masterStateChangerList);
        Collections.sort(leftStateChangerList);
        Collections.sort(rightStateChangerList);
    }

    /**
     * Clears the three main recipe lists.
     */
    public static void clearStateChangerLists()
    {
        masterStateChangerList.clear();
        leftStateChangerList.clear();
        rightStateChangerList.clear();
    }

    public static List<CampfireStateChanger> getMasterList()
    {
        return masterStateChangerList;
    }

    public static List<CampfireStateChanger> getStateChangerList(boolean leftClick)
    {
        return leftClick ? leftStateChangerList : rightStateChangerList;
    }

    public static List<CampfireStateChanger> getCraftTweakerList()
    {
        return crafttweakerStateChangerList;
    }

    /**
     * Finds a CampfireStateChanger with the given click type that applies to the given ItemStack and campfire.
     * 
     * @param reignitable
     *            - if this and lit are true, will search for extinguishers or ignitors
     * @return a matching CampfireStateChanger, or null if none was found
     */
    public static CampfireStateChanger findStateChanger(ItemStack stack, boolean leftClick, int typeIndex, boolean lit, boolean reignitable)
    {
        if (stack != null)
        {
            for (CampfireStateChanger cstate : getStateChangerList(leftClick))
            {
                if (cstate.matches(stack, typeIndex, lit, reignitable))
                    return cstate;
            }
        }
        return null;
    }

    /**
     * Checks if the given ItemStack and campfire match this CampfireStateChanger.
     * 
     * @param reignitable
     *            - if this and lit are true, extinguishers AND ignitors will match
     */
    public boolean matches(ItemStack stack, int typeIndex, boolean lit, boolean reignitable)
    {
        return stack != null && (isExtinguisher() == lit || (lit && reignitable)) && getTypes().matches(typeIndex) && getInput().matches(stack);
    }

    /**
     * CampfireStateChangers have only one input, so we have a shortcut for {@link #onUsingInput(int, ItemStack, EntityPlayer)}.
     */
    public ItemStack onUsingInput(ItemStack stack, EntityPlayer player)
    {
        return onUsingInput(0, stack, player);
    }

    /**
     * Crafting items that store durability in NBT and use container items to damage themselves are annoying...
     */
    @Override
    protected ItemStack useAndHandleContainer(CustomInput cinput, ItemStack stack, EntityPlayer player)
    {
        // none and stackable work fine.
        if (!isUsageTypeDamageable())
            return super.useAndHandleContainer(cinput, stack, player);

        if (stack == null)
            return stack;

        int damage = cinput.getInputSize();

        // check if the item is normally damageable.
        // no container handling here - surely no normally damageable item has a separate container that it wants to eject in this scenario?
        if (stack.getItem().getMaxDamage(stack) > 0 && !stack.getItem().getHasSubtypes())
        {
            stack.damageItem(damage, player);
        }
        // can't damage the item. next, we guess it is crafting item that stores crafting durability in nbt.
        // test examples: IE hammer, gregtech 6 tools, extra utils division sigil
        else
        {
            // if our guess is correct, we plan on taking the container item for each point of damage.
            for (int i = 0; i < damage; ++i)
            {
                ItemStack containerStack = stack.getItem().getContainerItem(stack);
                if (containerStack != null)
                {
                    // same item and damage, but the nbt changed.
                    // or it's simply returned the exact same stack instance. (extra utils division sigil does this)
                    // either way, we decide that our guess is correct. we will do the loop as expected.
                    if (stack == containerStack || (stack.isItemEqual(containerStack) && !ItemStack.areItemStackTagsEqual(stack, containerStack)))
                    {
                        stack = containerStack;
                    }
                    // we have finished using up the durability and some scrap has appeared. (gt6 tools do this)
                    // or it's a normal container item that isn't damageable and has one different container, like a milk bucket.
                    // either way, we do the same thing.
                    else
                    {
                        stack.stackSize--;
                        if (stack.stackSize <= 0)
                        {
                            ForgeEventFactory.onPlayerDestroyItem(player, stack);
                            return containerStack;
                        }
                        else if (!player.inventory.addItemStackToInventory(containerStack))
                            player.dropPlayerItemWithRandomChoice(containerStack, false);
                        return stack;
                    }
                }
                // we have finished using up the durability and there's nothing left. (IE hammer does this)
                // it could also be a normal item that isn't damageable and never had a container item in the first place, like a feather.
                // these two cases are not quite the same.
                else
                {
                    // from what i've seen this is a reliable way of telling the difference between the two cases.
                    // there may be exceptions.
                    if (!stack.getItem().doesContainerItemLeaveCraftingGrid(stack))
                        stack.stackSize--;
                    break;
                }
            }
        }

        // and finally, whether it was damaged and broke normally or was the last container item durability, post the event.
        if (stack.stackSize <= 0)
            ForgeEventFactory.onPlayerDestroyItem(player, stack);

        return stack;
    }

    @Override
    protected ItemStack use(CustomInput cinput, ItemStack stack, EntityPlayer player)
    {
        if (stack != null && stack.stackSize > 0)
        {
            if (isUsageTypeDamageable())
                stack.damageItem(cinput.getInputSize(), player);
            else if (isUsageTypeStackable())
                stack.stackSize = Math.max(stack.stackSize - cinput.getInputSize(), 0);
        }

        return stack;
    }

    @Override
    protected void reuse(CustomInput cinput, ItemStack stack, EntityPlayer player)
    {
        // just like in {@link connor135246.campfirebackport.common.compat.crafttweaker.CampfireBackportCraftTweaking#addCampfireStateChanger}, we don't want to reuse. 
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        return (isLeftClick() ? "Left" : "Right") + "-clicking with " + getInput() + (isExtinguisher() ? " extinguishes" : " ignites")
                + (getTypes() == EnumCampfireType.BOTH ? " all" : (getTypes().acceptsRegular() ? " Regular" : " Soul")) + " campfires"
                + (hasOutputs() ? (" and creates " + stackToString(getOutput())) : "") + (isDispensable() ? ", and also works from a dispenser" : "");
    }

    // Getters

    /**
     * CampfireStateChangers have only one input, so we have a shortcut for {@link #getInputs()}.
     */
    public CustomInput<?> getInput()
    {
        return getInputs()[0];
    }

    /**
     * CampfireStateChangers have only one output, so we have a shortcut for {@link #getOutputs()}.
     */
    public ItemStack getOutput()
    {
        return getOutputs()[0];
    }

    public boolean isDispensable()
    {
        return dispensable;
    }

    public boolean isLeftClick()
    {
        return leftClick;
    }

    public boolean isExtinguisher()
    {
        return extinguisher;
    }

    public byte getUsageType()
    {
        return usageType;
    }

    public boolean isUsageTypeStackable()
    {
        return usageType == 1;
    }

    public boolean isUsageTypeDamageable()
    {
        return usageType == 2;
    }

    // Sorting
    @Override
    public int compareTo(CampfireStateChanger cstate)
    {
        return super.compareTo(cstate);
    }

}
