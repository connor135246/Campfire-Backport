package connor135246.campfirebackport.common.dispenser;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.CampfireBackportFakePlayer;
import connor135246.campfirebackport.util.MiscUtil;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

public class BehaviourGeneric extends BehaviorDefaultDispenseItem
{

    private final CampfireStateChanger cstate;

    public BehaviourGeneric(CampfireStateChanger cstate)
    {
        this.cstate = cstate;
    }

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack)
    {
        EnumFacing enumfacing = BlockDispenser.func_149937_b(source.getBlockMetadata());
        World world = source.getWorld();
        boolean hasInv = source.getBlockTileEntity() instanceof IInventory;
        int i = source.getXInt() + enumfacing.getFrontOffsetX();
        int j = source.getYInt() + enumfacing.getFrontOffsetY();
        int k = source.getZInt() + enumfacing.getFrontOffsetZ();

        TileEntity tile = world.getTileEntity(i, j, k);
        if (tile instanceof TileEntityCampfire)
        {
            TileEntityCampfire ctile = (TileEntityCampfire) tile;

            if (cstate.matches(stack, ctile.getTypeIndex(), ctile.isLit(), ctile.canBeReignited())
                    && BlockCampfire.updateCampfireBlockState(cstate.isExtinguisher(), null, ctile) == 1)
            {
                if (world instanceof WorldServer)
                {
                    FakePlayer fakePlayer = CampfireBackportFakePlayer.getFakePlayer((WorldServer) world);

                    stack = cstate.onUsingInput(stack, fakePlayer);

                    for (int slot = 0; slot < fakePlayer.inventory.getSizeInventory(); ++slot)
                    {
                        ItemStack fakePlayerStack = fakePlayer.inventory.getStackInSlotOnClosing(slot);
                        if (fakePlayerStack != null)
                            if (!(hasInv && MiscUtil.putStackInInventory((IInventory) source.getBlockTileEntity(), fakePlayerStack, false)))
                                super.dispenseStack(source, fakePlayerStack);
                    }

                    if (cstate.hasOutputs())
                    {
                        ItemStack returned = ItemStack.copyItemStack(cstate.getOutput());
                        if (!(hasInv && MiscUtil.putStackInInventory((IInventory) source.getBlockTileEntity(), returned, false)))
                            super.dispenseStack(source, returned);
                    }
                }
            }
            return stack;
        }
        return super.dispenseStack(source, stack);
    }

}
