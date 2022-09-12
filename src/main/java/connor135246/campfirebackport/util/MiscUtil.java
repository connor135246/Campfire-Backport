package connor135246.campfirebackport.util;

import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * String-related utils go in {@link StringParsers}. Everything else goes here.
 */
public class MiscUtil
{

    /** 15 << 20 | 15 << 4 is max brightness (see {@link net.minecraft.world.World#getLightBrightnessForSkyBlocks}) */
    public static final int MAX_LIGHT_BRIGHTNESS = 15 << 20 | 15 << 4;

    /**
     * Tries to put returned in the inventory. <br>
     * Note: don't use this for player inventories! {@link net.minecraft.entity.player.InventoryPlayer#isItemValidForSlot} doesn't actually stop non-armor items from going into
     * armor slots.
     * 
     * @return true if all of returned was put in the inventory, false otherwise
     */
    public static boolean putStackInInventory(IInventory inventory, ItemStack returned, boolean animate)
    {
        return putStackInExistingSlots(inventory, returned, animate) || putStackInEmptySlots(inventory, returned, animate);
    }

    /**
     * If returned can stack, finds inventory slots that already have it and puts it there.
     * 
     * @return true if all of returned was put in the inventory, false otherwise
     */
    public static boolean putStackInExistingSlots(IInventory inventory, ItemStack returned, boolean animate)
    {
        if (returned != null && returned.isStackable())
        {
            for (int slot = 0; slot < inventory.getSizeInventory(); ++slot)
            {
                ItemStack thisslot = inventory.getStackInSlot(slot);
                if (thisslot != null && inventory.isItemValidForSlot(slot, returned) && thisslot.isStackable() && thisslot.getItem() == returned.getItem()
                        && (!thisslot.getHasSubtypes() || thisslot.getItemDamage() == returned.getItemDamage())
                        && ItemStack.areItemStackTagsEqual(thisslot, returned))
                {
                    int space = Math.min(returned.stackSize, Math.min(thisslot.getMaxStackSize(), inventory.getInventoryStackLimit()) - thisslot.stackSize);
                    if (space > 0)
                    {
                        thisslot.stackSize += space;
                        returned.stackSize -= space;
                        if (animate)
                            thisslot.animationsToGo = 5;
                        if (returned.stackSize <= 0)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Finds empty inventory slots and puts returned there. <br>
     * Note: don't use this for player inventories! {@link net.minecraft.entity.player.InventoryPlayer#isItemValidForSlot} doesn't actually stop non-armor items from going into
     * armor slots.
     * 
     * @return true if all of returned was put in the inventory, false otherwise
     */
    public static boolean putStackInEmptySlots(IInventory inventory, ItemStack returned, boolean animate)
    {
        if (returned != null)
        {
            int space = Math.min(returned.getMaxStackSize(), inventory.getInventoryStackLimit());

            for (int slot = 0; slot < inventory.getSizeInventory(); ++slot)
            {
                if (inventory.getStackInSlot(slot) == null && inventory.isItemValidForSlot(slot, returned))
                {
                    if (space >= returned.stackSize)
                    {
                        if (animate)
                            returned.animationsToGo = 5;
                        inventory.setInventorySlotContents(slot, returned);
                        return true;
                    }
                    else
                    {
                        ItemStack sideReturned = returned.splitStack(space);
                        if (animate)
                            sideReturned.animationsToGo = 5;
                        inventory.setInventorySlotContents(slot, sideReturned);
                    }
                }
            }
        }
        return false;
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
                FluidStack currentFluid = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
                return currentFluid != null && currentFluid.containsFluid(fluidStack);
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
     * Returns a new tag that has merged the tags of the base with the merger using {@link #mergeNBT(NBTTagCompound, String, NBTBase)}. <br>
     * If one of the compounds is null, simply returns a copy of the non-null one. If both are null, returns null.
     */
    public static NBTTagCompound mergeNBT(final NBTTagCompound base, final NBTTagCompound merger)
    {
        if (base == null)
        {
            if (merger == null)
                return null;
            else
                return (NBTTagCompound) merger.copy();
        }

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
     * If the merger is a list of the same type as the base's list, its tags are appended to the base's list. (There's a special case for enchantment lists, see
     * {@link #mergeEnchNBTHigherLevel}) <br>
     * Otherwise, the merger replaces the value in the base.
     */
    public static NBTTagCompound mergeNBT(final NBTTagCompound base, final String key, final NBTBase merger) throws ClassCastException
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
            else if (((NBTTagList) merger).func_150303_d() == 10 && (key.equals(StringParsers.KEY_ench) || key.equals(StringParsers.KEY_StoredEnchantments)))
                base.setTag(key, mergeEnchNBTHigherLevel(((NBTTagList) baseTag), ((NBTTagList) merger)));
            else
            {
                for (int i = 0; i < ((NBTTagList) merger).tagCount(); ++i)
                    ((NBTTagList) baseTag).appendTag(((NBTTagList) merger).removeTag(i));
            }
        }

        return base;
    }

    /**
     * Merges two lists of enchantments. If there are IDs that are in both lists, the mergerList's enchantments take priority over the baseList's enchantments.
     */
    public static NBTTagList mergeEnchNBT(final NBTTagList baseList, final NBTTagList mergerList)
    {
        NBTTagList returnList = (NBTTagList) mergerList.copy();

        for (int i = 0; i < baseList.tagCount(); ++i)
        {
            NBTTagCompound baseCompound = baseList.getCompoundTagAt(i);
            if (!hasMatchingEnchID(returnList, baseCompound.getInteger(StringParsers.KEY_id)))
                returnList.appendTag(baseCompound);
        }

        return returnList;
    }

    /**
     * Merges two lists of enchantments. If there are IDs that are in both lists, the higher level one takes priority.
     */
    public static NBTTagList mergeEnchNBTHigherLevel(final NBTTagList list1, final NBTTagList list2)
    {
        NBTTagList returnList = (NBTTagList) list1.copy();

        for (int i = 0; i < list2.tagCount(); ++i)
        {
            NBTTagCompound list2Compound = list2.getCompoundTagAt(i);
            NBTTagCompound returnListCompound = getMatchingEnchID(returnList, list2Compound.getInteger(StringParsers.KEY_id));
            if (returnListCompound == null)
                returnList.appendTag(list2Compound);
            else
            {
                int list2Level = list2Compound.getInteger(StringParsers.KEY_lvl);
                if (list2Level > returnListCompound.getInteger(StringParsers.KEY_lvl))
                    returnListCompound.setInteger(StringParsers.KEY_lvl, list2Level);
            }
        }

        return returnList;
    }

    /**
     * @return true if the list of enchantments has the enchantment with the given id, false otherwise
     */
    public static boolean hasMatchingEnchID(final NBTTagList enchList, final int id)
    {
        return getMatchingEnchID(enchList, id) != null;
    }

    /**
     * @return the enchantment compound with the given id from the given list, or null if it didn't have one
     */
    public static NBTTagCompound getMatchingEnchID(final NBTTagList enchList, final int id)
    {
        for (int i = 0; i < enchList.tagCount(); ++i)
        {
            NBTTagCompound thisEnch = enchList.getCompoundTagAt(i);
            if (thisEnch.getInteger(StringParsers.KEY_id) == id)
                return thisEnch;
        }
        return null;
    }

}
