package connor135246.campfirebackport.common.dispenser;

import java.util.Random;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.crafting.CampfireStateChanger;
import connor135246.campfirebackport.common.crafting.GenericCustomInput;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
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
            if (cblock.isLit() == cstate.isExtinguisher() && cstate.getTypes().matches(cblock)
                    && GenericCustomInput.matches(cstate, stack))
            {
                BlockCampfire.updateCampfireBlockState(!cstate.isExtinguisher(), world, i, j, k, cblock.getType());

                if (cstate.getDataType() == 2)
                    GenericCustomInput.doFluidEmptying(cstate, stack);

                if (cstate.getUsageType().equals(CampfireStateChanger.DAMAGEABLE))
                {
                    if (stack.attemptDamageItem(cstate.getInputSize(), new Random()))
                        stack.stackSize = 0;
                }
                else if (cstate.getUsageType().equals(CampfireStateChanger.STACKABLE))
                {
                    stack.stackSize -= cstate.getInputSize();
                    if (stack.stackSize < 0)
                        stack.stackSize = 0;
                }

                if (cstate.hasReturnStack())
                {
                    ItemStack returned = cstate.getReturnStack().copy();
                    if (!doReplaceable(world, sourceblock, returned))
                        super.dispenseStack(sourceblock, returned);
                }
            }
            return stack;
        }
        return super.dispenseStack(sourceblock, stack);
    }

    /**
     * Tries to put the replacement stack back in the dispenser.
     * 
     * @param world
     * @param sourceblock
     * @param returned
     *            - the ItemStack to be put in the dispenser
     * @return true if the ItemStack was put in, false if there's some left over
     */
    private boolean doReplaceable(World world, IBlockSource sourceblock, ItemStack returned)
    {
        TileEntityDispenser dispenser = (TileEntityDispenser) sourceblock.getBlockTileEntity();

        // looking for dispenser slots that already have returned, if it can stack
        if (returned.isStackable())
        {
            for (int n = 0; n < dispenser.getSizeInventory(); ++n)
            {
                ItemStack thisslot = dispenser.getStackInSlot(n);
                if (thisslot != null && dispenser.isItemValidForSlot(n, returned) && thisslot.isStackable() && thisslot.getItem() == returned.getItem()
                        && (!thisslot.getHasSubtypes() || thisslot.getItemDamage() == returned.getItemDamage())
                        && ItemStack.areItemStackTagsEqual(thisslot, returned))
                {
                    int space = Math.min(returned.stackSize, Math.min(thisslot.getMaxStackSize(), dispenser.getInventoryStackLimit()) - thisslot.stackSize);
                    if (space > 0)
                    {
                        thisslot.stackSize += space;
                        returned.stackSize -= space;
                        if (returned.stackSize == 0)
                            return true;
                    }
                }
            }
        }

        // looking for any empty dispenser slots
        for (int n = 0; n < dispenser.getSizeInventory(); ++n)
        {
            ItemStack thisslot = dispenser.getStackInSlot(n);
            if (thisslot == null && dispenser.isItemValidForSlot(n, returned))
            {
                dispenser.setInventorySlotContents(n, returned);
                return true;
            }
        }

        // failure to find space
        return false;
    }
}
