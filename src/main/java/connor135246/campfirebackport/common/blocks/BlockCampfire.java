package connor135246.campfirebackport.common.blocks;

import java.util.ArrayList;
import java.util.Random;

import javax.annotation.Nullable;

import connor135246.campfirebackport.common.dispenser.BehaviourGeneric;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;

public class BlockCampfire extends BlockContainer
{
    protected static final Random RAND = new Random();

    protected final boolean lit;
    protected final String type;

    protected static boolean stateChanging = false;

    protected BlockCampfire(boolean lit, String type)
    {
        super(Material.wood);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.4375F, 1.0F);
        this.setHardness(2.0F);
        this.setResistance(2.0F);
        this.setStepSound(Block.soundTypeWood);
        this.setCreativeTab(CreativeTabs.tabDecorations);

        this.lit = lit;
        this.type = type;
    }

    /**
     * player facing:<br>
     * 2 5 3 4<br>
     * S W N E<br>
     * block metadata:<br>
     * 2 5 3 4<br>
     * N E S W
     */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack)
    {
        int facing = MathHelper.floor_double(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

        switch (facing)
        {
        case 0:
            world.setBlockMetadataWithNotify(x, y, z, 2, 2);
            break;
        case 1:
            world.setBlockMetadataWithNotify(x, y, z, 5, 2);
            break;
        case 2:
            world.setBlockMetadataWithNotify(x, y, z, 3, 2);
            break;
        case 3:
            world.setBlockMetadataWithNotify(x, y, z, 4, 2);
            break;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityCampfire)
        {
            if (stack.hasTagCompound())
            {
                if (stack.getTagCompound().hasKey(TileEntityCampfire.KEY_BlockEntityTag))
                    ((TileEntityCampfire) tile).readFromNBTIfItExists(stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag));

                if (stack.hasDisplayName())
                    ((TileEntityCampfire) tile).setCustomInventoryName(stack.getDisplayName());
            }

            ((TileEntityCampfire) tile).checkSignal();
            ((TileEntityCampfire) tile).burnOutDueToLackOfOxygen();
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        super.onNeighborBlockChange(world, x, y, z, block);

        TileEntityCampfire.checkSignal(world, x, y, z);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityCampfire();
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        super.onEntityCollidedWithBlock(world, x, y, z, entity);

        if (!world.isRemote)
        {
            if (!isLit() && entity.isBurning())
            {
                toggleCampfireBlockState(world, x, y, z);
            }
            else if (isLit() && entity instanceof EntityLivingBase && !entity.isImmuneToFire() && CampfireBackportConfig.damaging.matches(this))
            {
                if (entity.attackEntityFrom(DamageSource.inFire, EnumCampfireType.option(getType(), 1.0F, 2.0F)))
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "random.fizz", 0.5F, 2.6F + (RAND.nextFloat() - RAND.nextFloat()) * 0.8F);
            }
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null)
            doStateChangers(!player.capabilities.isCreativeMode, stack, true, world, x, y, z, player);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null)
        {
            boolean survival = !player.capabilities.isCreativeMode;

            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityCampfire && ((TileEntityCampfire) tile).tryInventoryAdd(survival ? stack : ItemStack.copyItemStack(stack)))
                return true;

            return doStateChangers(survival, stack, false, world, x, y, z, player);
        }
        return false;
    }

    /**
     * Finds state changers that match the given ItemStack and this campfire block, and executes them.
     * 
     * @param survival
     *            - is the player in survival mode
     * @param stack
     *            - the player's held item (should've already verified it isn't null)
     * @param leftClick
     *            - is this being called from {@link #onBlockClicked}? or is it from {@link #onBlockActivated}?
     * @return true if a state changer was found and executed, false otherwise
     */
    protected boolean doStateChangers(boolean survival, ItemStack stack, boolean leftClick, World world, int x, int y, int z, EntityPlayer player)
    {
        CampfireStateChanger cstate = CampfireStateChanger.findStateChanger(stack, leftClick, getType(), isLit());

        if (cstate != null)
        {
            int result = updateCampfireBlockState(!isLit(), player, world, x, y, z);

            if (result == 1)
            {
                if (survival)
                {
                    if (cstate.getInput().getDataType() == 3)
                        cstate.getInput().doFluidEmptying(stack);

                    if (cstate.isUsageTypeDamageable())
                        stack.damageItem(cstate.getInput().getInputSize(), player);
                    else if (cstate.isUsageTypeStackable())
                        stack.stackSize -= cstate.getInput().getInputSize();

                    if (stack.stackSize <= 0 && player.getCurrentEquippedItem() == stack)
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                }

                if (cstate.hasOutputs())
                {
                    ItemStack returned = ItemStack.copyItemStack(cstate.getOutput());
                    if (!BehaviourGeneric.putStackInExistingSlots(player.inventory, returned, true))
                    {
                        if (player.getCurrentEquippedItem() == null)
                        {
                            returned.animationsToGo = 5;
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, returned);
                            if (player instanceof EntityPlayerMP)
                                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
                        }
                        else if (!BehaviourGeneric.putStackInEmptySlots(player.inventory, returned, true))
                            player.dropPlayerItemWithRandomChoice(returned, false);
                    }
                }
            }

            return result != 0;
        }
        return false;
    }

    /**
     * Updates a campfire's block between the lit and unlit ("base") version. Posts a {@link CampfireStateChangeEvent} before it does so. If there isn't a campfire there, or the
     * campfire is already in that state, nothing happens.
     * 
     * @param light
     *            - true to update to lit, false to update to unlit
     * @param player
     *            - may be null
     * @return returns 0 if the block wasn't a valid target to state change. otherwise, returns 1 if the state changer should be used up and 2 if it shouldn't be.
     */
    public static int updateCampfireBlockState(boolean light, @Nullable EntityPlayer player, World world, int x, int y, int z)
    {
        Block oldCblock = world.getBlock(x, y, z);
        TileEntity tile = world.getTileEntity(x, y, z);
        TileEntityCampfire ctile;

        // verify that this state change should happen
        if (oldCblock instanceof BlockCampfire && ((BlockCampfire) oldCblock).isLit() != light && tile instanceof TileEntityCampfire)
            ctile = (TileEntityCampfire) tile;
        else
            return 0;

        int meta = world.getBlockMetadata(x, y, z);

        CampfireStateChangeEvent event = new CampfireStateChangeEvent(x, y, z, world, oldCblock, meta, player);
        MinecraftForge.EVENT_BUS.post(event);

        if (!event.isCanceled())
        {
            BlockCampfire newCblock = (BlockCampfire) CampfireBackportBlocks.getBlockFromLitAndType(light, ((BlockCampfire) oldCblock).getType());

            stateChanging = true;

            world.setBlock(x, y, z, newCblock, meta, 3);

            if (!world.isRemote)
            {
                if (light)
                {
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);
                    ctile.checkSignal();
                    ctile.resetLife();
                    ctile.resetRegenWaitTimer();
                }
                else
                {
                    ctile.playFizzAndAddSmokeServerSide(20, 0.45);
                    ctile.popItems();
                }
            }

            ctile.updateContainingBlockInfo();
            ctile.validate();

            stateChanging = false;
        }
        else if (event.useGoods) // if the event was canceled but we're still going to use up state changers, play a sound to indicate that
        {
            if (!world.isRemote)
            {
                if (light)
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);
                else
                    world.playSoundEffect(x, y, z, "random.fizz", 0.5F, RAND.nextFloat() * 0.4F + 0.8F);
            }
        }

        return event.useGoods ? 1 : 2;
    }

    /**
     * Helper function that does {@link #updateCampfireBlockState} for this block, with a null player.
     */
    public int toggleCampfireBlockState(World world, int x, int y, int z)
    {
        return updateCampfireBlockState(!isLit(), null, world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        if (!stateChanging)
        {
            if (!world.isRemote)
            {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileEntityCampfire)
                    ((TileEntityCampfire) tile).popItems();
            }
            super.breakBlock(world, x, y, z, block, meta);
        }
    }

    @Override
    public boolean hasComparatorInputOverride()
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IInventory)
            return Container.calcRedstoneFromInventory((IInventory) tile);
        else
            return 0;
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis)
    {
        if (!world.isRemote)
        {
            switch (world.getBlockMetadata(x, y, z))
            {
            default:
                return world.setBlockMetadataWithNotify(x, y, z, 5, 3);
            case 5:
                return world.setBlockMetadataWithNotify(x, y, z, 3, 3);
            case 3:
                return world.setBlockMetadataWithNotify(x, y, z, 4, 3);
            case 4:
                return world.setBlockMetadataWithNotify(x, y, z, 2, 3);
            }
        }
        else
            return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random)
    {
        if (isLit())
        {
            if (random.nextInt(10) == 0)
            {
                world.playSound(x + 0.5, y + 0.4375, z + 0.5, Reference.MODID + ":" + "block.campfire.crackle", 0.5F + random.nextFloat(),
                        random.nextFloat() * 0.7F + 0.6F, false);
            }

            if (random.nextInt(5) == 0 && EnumCampfireType.isRegular(getType()))
            {
                world.spawnParticle("lava", x + 0.5, y + 0.4375, z + 0.5, random.nextFloat() / 2.0F, 0.00005, random.nextFloat() / 2.0F);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Item getItem(World world, int x, int y, int z)
    {
        return getCampfireBlockItem();
    }

    /**
     * @return either the campfire_base block item or the campfire block item, depending on two config settings
     */
    public Item getCampfireBlockItem()
    {
        if (CampfireBackportConfig.rememberState.matches(this))
            return Item.getItemFromBlock(this);
        else
            return Item.getItemFromBlock(CampfireBackportBlocks.getBlockFromLitAndType(!CampfireBackportConfig.startUnlit.matches(this), getType()));
    }

    /**
     * so that waila harvestability recognizes this block as needing silk touch
     */
    @Override
    public int quantityDropped(Random p_149745_1_)
    {
        return 0;
    }

    @Override
    protected boolean canSilkHarvest()
    {
        return CampfireBackportConfig.silkNeeded.matches(this);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
    {
        if (willHarvest)
            return true;
        else
            return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta)
    {
        player.addStat(StatList.mineBlockStatArray[getIdFromBlock(this)], 1);
        player.addExhaustion(0.025F);

        ArrayList<ItemStack> drops = getDropsInAllScenarios(player, world, x, y, z, meta);

        ForgeEventFactory.fireBlockHarvesting(drops, world, this, x, y, z, meta, EnchantmentHelper.getFortuneModifier(player), 1.0F,
                EnchantmentHelper.getSilkTouchModifier(player), player);

        for (ItemStack stack : drops)
            dropBlockAsItem(world, x, y, z, stack);

        world.setBlockToAir(x, y, z);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune)
    {
        return getDropsInAllScenarios(null, world, x, y, z, meta);
    }

    /**
     * Sometimes you have silk touch, sometimes you don't. Sometimes silk touch matters, sometimes it doesn't. Sometimes I need to copy stuff from the tile entity, sometimes I
     * don't. There's a whole lot to cover.
     * 
     * @param player
     *            - can be null, if it wasn't broken by a player
     */
    protected ArrayList<ItemStack> getDropsInAllScenarios(@Nullable EntityPlayer player, World world, int x, int y, int z, int meta)
    {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();

        if (!canSilkHarvest() || (player != null && EnchantmentHelper.getSilkTouchModifier(player)))
        {
            ItemStack dropstack = new ItemStack(getCampfireBlockItem(), 1, 0);

            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityCampfire)
            {
                TileEntityCampfire ctile = (TileEntityCampfire) tile;

                boolean lifeMatters = isLit() && CampfireBackportConfig.burnOutAsItem.matches(this);
                boolean nameMatters = ctile.hasCustomInventoryName();

                if (lifeMatters || nameMatters)
                {
                    NBTTagCompound droptag = new NBTTagCompound();
                    NBTTagCompound droptiletag = new NBTTagCompound();

                    if (lifeMatters)
                    {
                        droptiletag.setInteger(TileEntityCampfire.KEY_Life, ctile.getLife());
                        droptiletag.setInteger(TileEntityCampfire.KEY_StartingLife, ctile.getStartingLife());
                        droptiletag.setLong(TileEntityCampfire.KEY_PreviousTimestamp, world.getTotalWorldTime());
                    }

                    if (nameMatters)
                    {
                        droptag.setTag("display", new NBTTagCompound());
                        droptag.getCompoundTag("display").setString("Name", ctile.getInventoryName());
                        droptiletag.setString(TileEntityCampfire.KEY_CustomName, ctile.getInventoryName());
                    }

                    if (!droptiletag.hasNoTags() || !droptag.hasNoTags())
                    {
                        dropstack.setTagCompound(droptag);
                        if (!droptiletag.hasNoTags())
                            dropstack.getTagCompound().setTag(TileEntityCampfire.KEY_BlockEntityTag, droptiletag);
                    }
                }
            }
            drops.add(dropstack);
        }
        else
            drops.add(ItemStack.copyItemStack(CampfireBackportConfig.campfireDropsStacks[getTypeIndex()]));

        return drops;
    }

    @Override
    public boolean isBed(IBlockAccess world, int x, int y, int z, EntityLivingBase player)
    {
        return isLit() && CampfireBackportConfig.spawnpointable.matches(this);
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return -1;
    }

    // Getters

    public boolean isLit()
    {
        return lit;
    }

    public String getType()
    {
        return type;
    }

    public int getTypeIndex()
    {
        return EnumCampfireType.index(getType());
    }

    //

    /**
     * Allows you to control what happens when a campfire's state attempts to be changed.<br>
     * If canceled, the campfire's state won't change.<br>
     * The block given is the campfire block before it attempts to be changed.<br>
     * If the campfire's state is changing due to a player using state changer, {@link #player} is that player. Otherwise, it's null!<br>
     * If the campfire's state is changing due to a state changer, you can set {@link #useGoods} to false to stop the state changer from being used up. You probably want to set
     * this to false if you cancel the event!<br>
     * <br>
     * This event is posted from {@link BlockCampfire#updateCampfireBlockState} on the {@link MinecraftForge#EVENT_BUS}.<br>
     * Note that this event is posted to both the client and server sides if the campfire is changing due to a player using a state changer. At other times, it's only posted to the
     * server side.<br>
     * This event is {@link Cancelable}.<br>
     */
    @Cancelable
    public static class CampfireStateChangeEvent extends BlockEvent
    {
        public final EntityPlayer player;
        public boolean useGoods = true;

        public CampfireStateChangeEvent(int x, int y, int z, World world, Block oldCblock, int blockMetadata, @Nullable EntityPlayer player)
        {
            super(x, y, z, world, oldCblock, blockMetadata);
            this.player = player;
        }

    }

}
