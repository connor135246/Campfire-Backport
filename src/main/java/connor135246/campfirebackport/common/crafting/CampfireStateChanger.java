package connor135246.campfirebackport.common.crafting;

import java.util.ArrayList;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
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

public class CampfireStateChanger extends GenericCustomInput implements Comparable<CampfireStateChanger>
{

    public static final String DAMAGEABLE = "damageable", STACKABLE = "stackable", NONE = "none";

    private boolean dispensable = false;
    private boolean leftClick;
    private boolean extinguisher;
    private String usageType = NONE;
    private ItemStack returnStack;

    /** the master list of state changers! */
    private static ArrayList<CampfireStateChanger> masterStateChangerList = new ArrayList<CampfireStateChanger>();
    /** the state changers that are left clicked. copied from the master list. */
    private static ArrayList<CampfireStateChanger> leftStateChangerList = new ArrayList<CampfireStateChanger>();
    /** the state changers that are right clicked. copied from the master list. */
    private static ArrayList<CampfireStateChanger> rightStateChangerList = new ArrayList<CampfireStateChanger>();

    /**
     * Converts a string into a campfire state changer. If it's invalid, says so in the console.<br>
     * <br>
     * See {@link StringParsers#parseItemStackOrOreOrClassWithNBTOrDataWithSize(String)} for the input/output format.
     * 
     * @param stateChanger
     *            - a string in the format [left|right](+dispensable)/[ANY INPUT]/[none|stackable|dispensable](>ANY OUTPUT)
     * @param types
     *            - the types of campfire this is being added to
     * @param extinguisher
     *            - true if this extinguishers, false if this ignites
     */
    public CampfireStateChanger(String stateChanger, EnumCampfireType types, boolean extinguisher)
    {
        try
        {
            String[] segment = stateChanger.split("/");

            // how it's used

            String[] use = segment[0].split("\\+");

            this.leftClick = use[0].equals("left");

            this.extinguisher = extinguisher;

            // after it's used

            String[] afterUse = segment[2].split(">");

            this.usageType = afterUse[0];

            if (afterUse.length > 1)
            {
                Object[] replaceWith = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(afterUse[1], false);

                ItemStack retStack = new ItemStack((Item) replaceWith[0], MathHelper.clamp_int((Integer) replaceWith[1], 1, 64), (Integer) replaceWith[2]);

                if (!((NBTTagCompound) replaceWith[3]).hasNoTags())
                {
                    retStack.setTagCompound((NBTTagCompound) replaceWith[3]);
                    retStack.stackTagCompound.removeTag(StringParsers.GCI_DATATYPE);
                }

                this.returnStack = retStack;
            }

            // input

            customGInput(StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[1], true), types, !isUsageTypeDamageable(),
                    !isUsageTypeDamageable() ? 64 : -1);

            if (isUsageTypeDamageable())
                getNEITooltip().add(EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(Reference.MODID + ".nei.damage_by", getInputSize()));

            // register dispensables (if the input type allows) (only on initial load)

            if (use.length > 1)
            {
                if (!this.isDataInput())
                    this.dispensable = true;
                else if (!CampfireBackportConfig.suppressInputErrors)
                    CampfireBackport.proxy.modlog
                            .warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_dispensable", stateChanger));
            }

            if (CampfireBackportConfig.initialLoad && this.dispensable)
            {
                for (ItemStack stack : this.getInputList())
                {
                    if (!CampfireBackportConfig.dispenserBlacklistItems.contains(stack.getItem()))
                        BlockDispenser.dispenseBehaviorRegistry.putObject(stack.getItem(), new BehaviourGeneric(this));
                }
            }
        }
        catch (Exception excep)
        {
            this.input = null;
        }
    }

    /**
     * Tries to make a <code>CampfireStateChanger</code> based on user input for the given campfire types and use and add it the master recipe list.<br>
     * See {@link #CampfireStateChanger(String, EnumCampfireType, boolean)}.
     * 
     * @param recipe
     *            - the user-input string that represents a state changer
     * @param types
     *            - the types of campfire this recipe should apply to
     * @param extinguisher
     *            - true if this extinguishers, false if this ignites
     * @return true if the recipe was added successfully, false if it wasn't
     */
    public static boolean addToMasterList(String stateChanger, EnumCampfireType types, boolean extinguisher)
    {
        CampfireStateChanger cstate = new CampfireStateChanger(stateChanger, types, extinguisher);
        if (cstate.getInput() != null)
        {
            getMasterList().add(cstate);
            return true;
        }
        else
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog
                        .warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_state_changer",
                                extinguisher ? ConfigReference.extinguisher : ConfigReference.ignitor,
                                stateChanger));
            return false;
        }
    }

    public static ArrayList<CampfireStateChanger> getMasterList()
    {
        return masterStateChangerList;
    }

    public static ArrayList<CampfireStateChanger> getStateChangerList(boolean leftClick)
    {
        return leftClick ? leftStateChangerList : rightStateChangerList;
    }

    /**
     * Finds a <code>CampfireStateChanger</code> with the click type that applies to the given <code>ItemStack</code> and <code>BlockCampfire</code>.
     * 
     * @param stack
     * @param leftClick
     * @param cblock
     * @return a matching <code>CampfireStateChanger</code>, or null if none was found
     */
    public static CampfireStateChanger findStateChanger(ItemStack stack, boolean leftClick, BlockCampfire cblock)
    {
        for (CampfireStateChanger cstate : getStateChangerList(leftClick))
        {
            if (cstate.isExtinguisher() == cblock.isLit() && cstate.getTypes().matches(cblock) && matches(cstate, stack))
                return cstate;
        }
        return null;
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        return (isLeftClick() ? "Left" : "Right") + "-clicking with "
                + (super.toString() + (isMultiInput() && doesInputSizeMatter() ? " x " + getInputSize() : ""))
                + (isExtinguisher() ? " extinguishes" : " ignites")
                + (getTypes() == EnumCampfireType.BOTH ? "" : (getTypes().matches(EnumCampfireType.SOUL) ? " soul" : " regular")) + " campfires"
                + (hasReturnStack() ? (" and creates [" + getReturnStack().getDisplayName()
                        + (getReturnStack().hasTagCompound() ? " with NBT:" + getReturnStack().getTagCompound() : "")
                        + "]" + (getReturnStack().stackSize > 1 ? " x " + getReturnStack().stackSize : "")) : "")
                + (isDispensable() ? ", and also works from a dispenser" : "");
    }

    // Getters and Setters

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

    public ItemStack getReturnStack()
    {
        return returnStack;
    }

    public boolean hasReturnStack()
    {
        return returnStack != null;
    }

    @Override
    public boolean isMultiInput()
    {
        return isUsageTypeDamageable() ? false : super.isMultiInput();
    }

    // Sorting
    @Override
    public int compareTo(CampfireStateChanger cstate)
    {
        return super.compareTo(cstate);
    }

}
