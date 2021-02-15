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
                    if (!doReplaceable(sourceblock, returned))
                        super.dispenseStack(sourceblock, returned);
                }
            }
            return stack;
        }
        return super.dispenseStack(sourceblock, stack);
    }

    /**
     * Tries to put the replacement stack back in the tile.
     * 
     * @param sourceblock
     * @param returned
     *            - the ItemStack to be put in the dispenser
     * @return true if the ItemStack was put in, false if there's some left over
     */
    private boolean doReplaceable(IBlockSource sourceblock, ItemStack returned)
    {
        if (sourceblock.getBlockTileEntity() instanceof IInventory)
        {
            IInventory inventory = (IInventory) sourceblock.getBlockTileEntity();

            // looking for inventory slots that already have returned, if it can stack
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
                            if (returned.stackSize <= 0)
                                return true;
                        }
                    }
                }
            }

            // looking for any empty inventory slots
            for (int slot = 0; slot < inventory.getSizeInventory(); ++slot)
            {
                if (inventory.getStackInSlot(slot) == null && inventory.isItemValidForSlot(slot, returned)
                        && inventory.getInventoryStackLimit() >= returned.stackSize)
                {
                    inventory.setInventorySlotContents(slot, returned);
                    return true;
                }
            }
        }

        // failure to find space
        return false;
    }
}
