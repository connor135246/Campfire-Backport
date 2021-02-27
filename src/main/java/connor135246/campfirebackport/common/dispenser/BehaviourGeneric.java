package connor135246.campfirebackport.common.dispenser;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BehaviourGeneric extends BehaviorDefaultDispenseItem
{
    private final CampfireStateChanger cstate;

    public BehaviourGeneric(CampfireStateChanger cstate)
    {
        this.cstate = cstate;
    }

    @Override
    protected ItemStack dispenseStack(IBlockSource sourceblock, ItemStack stack)
    {
        EnumFacing enumfacing = BlockDispenser.func_149937_b(sourceblock.getBlockMetadata());
        World world = sourceblock.getWorld();
        int i = sourceblock.getXInt() + enumfacing.getFrontOffsetX();
        int j = sourceblock.getYInt() + enumfacing.getFrontOffsetY();
        int k = sourceblock.getZInt() + enumfacing.getFrontOffsetZ();

        Block block = world.getBlock(i, j, k);
        if (block instanceof BlockCampfire)
        {
            BlockCampfire cblock = (BlockCampfire) block;

            if (cstate.matches(stack, cblock.getType(), cblock.isLit()) && cblock.toggleCampfireBlockState(world, i, j, k) == 1)
            {
                if (cstate.getInput().getDataType() == 3)
                    cstate.getInput().doFluidEmptying(stack);

                if (cstate.isUsageTypeDamageable())
                {
                    if (stack.attemptDamageItem(cstate.getInput().getInputSize(), world.rand))
                        stack.stackSize = 0;
                }
                else if (cstate.isUsageTypeStackable())
                {
                    stack.stackSize -= cstate.getInput().getInputSize();
                    if (stack.stackSize < 0)
                        stack.stackSize = 0;
                }

                if (cstate.hasOutputs())
                {
                    ItemStack returned = ItemStack.copyItemStack(cstate.getOutput());
                    if (!(sourceblock.getBlockTileEntity() instanceof IInventory
                            && putStackInInventory((IInventory) sourceblock.getBlockTileEntity(), returned, false)))
                        super.dispenseStack(sourceblock, returned);
                }
            }
            return stack;
        }
        return super.dispenseStack(sourceblock, stack);
    }

    /**
     * Tries to put returned in the inventory.
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
        if (returned.isStackable())
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
     * Finds empty inventory slots and puts returned there.
     * 
     * @return true if all of returned was put in the inventory, false otherwise
     */
    public static boolean putStackInEmptySlots(IInventory inventory, ItemStack returned, boolean animate)
    {
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot)
        {
            if (inventory.getStackInSlot(slot) == null && inventory.isItemValidForSlot(slot, returned))
            {
                if (inventory.getInventoryStackLimit() >= returned.stackSize)
                {
                    if (animate)
                        returned.animationsToGo = 5;
                    inventory.setInventorySlotContents(slot, returned);
                    return true;
                }
                else
                {
                    ItemStack sideReturned = returned.splitStack(inventory.getInventoryStackLimit());
                    if (animate)
                        sideReturned.animationsToGo = 5;
                    inventory.setInventorySlotContents(slot, sideReturned);
                }
            }
        }
        return false;
    }

}
