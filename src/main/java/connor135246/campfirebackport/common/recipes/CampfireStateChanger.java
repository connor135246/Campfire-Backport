package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.dispenser.BehaviourGeneric;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.block.BlockDispenser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;

public class CampfireStateChanger extends GenericRecipe implements Comparable<CampfireStateChanger>
{

    public static final String DAMAGEABLE = "damageable", STACKABLE = "stackable", NONE = "none";

    /** dispensable CampfireStateChangers work from a dispenser. */
    protected final boolean dispensable;
    /** true for left click actions, false for right click actions. */
    protected final boolean leftClick;
    /** true for extinguishers, false for ignitors. */
    protected final boolean extinguisher;
    /** controls how the input is consumed. */
    protected final String usageType;

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

            String usageType = afterUse[0];
            boolean damageable = usageType.equals(DAMAGEABLE);

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
            if (cinput.input == null)
                throw new Exception();

            // dispensable

            boolean dispensable = false;

            if (use.length > 1)
            {
                if (!cinput.isDataInput())
                    dispensable = true;
                else
                    ConfigReference.logError("invalid_dispensable", stateChanger);
            }

            // done!

            return new CampfireStateChanger(types, new CustomInput[] { cinput }, leftClick, extinguisher, usageType, outputs, dispensable, 100);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    public CampfireStateChanger(EnumCampfireType types, CustomInput[] inputs, boolean leftClick, boolean extinguisher, String usageType,
            @Nullable ItemStack[] outputs, boolean dispensable, int sortPriority)
    {
        super(types, inputs, outputs, sortPriority);

        this.leftClick = leftClick;
        this.extinguisher = extinguisher;
        this.usageType = usageType;
        this.dispensable = dispensable;

        // damageable tooltip

        if (isUsageTypeDamageable())
        {
            LinkedList<String> tip = getInput().getNEITooltip();
            if (tip.isEmpty())
                tip.add("");

            tip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.damage_by", getInput().getInputSize()));
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
            ConfigReference.logError("invalid_state_changer", extinguisher ? ConfigReference.extinguisher : ConfigReference.ignitor, stateChanger);

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
     * @return a matching CampfireStateChanger, or null if none was found
     */
    public static CampfireStateChanger findStateChanger(ItemStack stack, boolean leftClick, String type, boolean lit)
    {
        if (stack != null)
        {
            for (CampfireStateChanger cstate : getStateChangerList(leftClick))
            {
                if (cstate.matches(stack, type, lit))
                    return cstate;
            }
        }
        return null;
    }

    /**
     * Checks if the given ItemStack and campfire match this CampfireStateChanger.
     */
    public boolean matches(ItemStack stack, String type, boolean lit)
    {
        return stack != null && isExtinguisher() == lit && getTypes().matches(type) && getInput().matches(stack);
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
     * CampfireStateChangers have only one input, so we have a shortcut.
     */
    public CustomInput getInput()
    {
        return getInputs()[0];
    }

    /**
     * CampfireStateChangers have only one output, so we have a shortcut.
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

    public String getUsageType()
    {
        return usageType;
    }

    public boolean isUsageTypeDamageable()
    {
        return usageType.equals(DAMAGEABLE);
    }

    public boolean isUsageTypeStackable()
    {
        return usageType.equals(STACKABLE);
    }

    // Sorting
    @Override
    public int compareTo(CampfireStateChanger cstate)
    {
        return super.compareTo(cstate);
    }

}
