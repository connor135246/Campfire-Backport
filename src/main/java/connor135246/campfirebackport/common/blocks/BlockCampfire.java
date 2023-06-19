package connor135246.campfirebackport.common.blocks;

import java.util.ArrayList;
import java.util.Random;

import javax.annotation.Nullable;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.client.rendering.InterpolatedIcon;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

public class BlockCampfire extends BlockContainer
{
    protected static final Random RAND = new Random();

    protected final boolean lit;
    protected final String type;

    @SideOnly(Side.CLIENT)
    protected IIcon litLog;
    @SideOnly(Side.CLIENT)
    protected IIcon fire;

    /** set to the next render id in client init */
    public static int renderId = -1;

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
            ((TileEntityCampfire) tile).checkSignal();

            if (stack.hasTagCompound())
            {
                if (stack.getTagCompound().hasKey(TileEntityCampfire.KEY_BlockEntityTag, 10))
                    ((TileEntityCampfire) tile).readFromNBTIfItExists(stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag));

                if (stack.hasDisplayName())
                    ((TileEntityCampfire) tile).setCustomInventoryName(stack.getDisplayName());
            }

            if (isLit() && (waterCheck(world, x, y, z) || !CampfireBackportCompat.hasOxygen(world, this, x, y, z)))
                ((TileEntityCampfire) tile).burnOutOrToNothing();
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        super.onNeighborBlockChange(world, x, y, z, block);

        TileEntityCampfire.checkSignal(world, x, y, z);

        if (isLit() && waterCheck(world, x, y, z))
            TileEntityCampfire.burnOutOrToNothing(world, x, y, z);
    }

    /**
     * checks if the block above is a water block and the config option is set
     */
    public boolean waterCheck(World world, int x, int y, int z)
    {
        return !CampfireBackportConfig.worksUnderwater.matches(this) && world.getBlock(x, y + 1, z).getMaterial() == Material.water;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityCampfire();
    }

    /** if thaumcraft is installed, this will become EntityPrimalArrow.class. otherwise, it's just null. */
    private static Class primalArrowClass = CampfireBackport.class;

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        super.onEntityCollidedWithBlock(world, x, y, z, entity);

        if (!world.isRemote)
        {
            if (entity instanceof EntityArrow)
            {
                if (primalArrowClass == CampfireBackport.class)
                    primalArrowClass = (Class) EntityList.stringToClassMapping.get("Thaumcraft.PrimalArrow");
                if (primalArrowClass != null && primalArrowClass.isInstance(entity))
                {
                    thaumcraft.common.entities.projectile.EntityPrimalArrow primalarrow = (thaumcraft.common.entities.projectile.EntityPrimalArrow) entity;
                    boolean canIgnite = primalarrow.type == 1 || entity.isBurning(); // 1 is fire
                    boolean canExtinguish = isLit() && primalarrow.type == 2; // 2 is water
                    if ((canIgnite && igniteOrReigniteCampfire(null, world, x, y, z) != 0) || (canExtinguish && extinguishCampfire(null, world, x, y, z) != 0))
                    {
                        primalarrow.setDead();
                        return;
                    }
                }
            }
            if (entity.isBurning())
            {
                igniteOrReigniteCampfire(null, world, x, y, z);
            }
            else if (isLit() && entity instanceof EntityLivingBase && !entity.isImmuneToFire() && CampfireBackportConfig.damaging.matches(this))
            {
                if (entity.attackEntityFrom(DamageSource.inFire, EnumCampfireType.isSoul(getType()) ? 2.0F : 1.0F))
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "random.fizz", 0.5F, 2.6F + (RAND.nextFloat() - RAND.nextFloat()) * 0.8F);
            }
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        ItemStack stack = player.getCurrentEquippedItem();
        TileEntity tile;
        if (stack != null && (tile = world.getTileEntity(x, y, z)) instanceof TileEntityCampfire)
            doStateChangers(!player.capabilities.isCreativeMode, stack, true, (TileEntityCampfire) tile, player);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null)
        {
            boolean survival = !player.capabilities.isCreativeMode;

            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityCampfire)
            {
                if (((TileEntityCampfire) tile).tryInventoryAdd(survival ? stack : ItemStack.copyItemStack(stack)))
                    return true;
                else
                    return doStateChangers(survival, stack, false, (TileEntityCampfire) tile, player);
            }
        }
        return false;
    }

    /**
     * Finds state changers that match the given ItemStack and campfire, and executes them.
     * 
     * @param survival
     *            - is the player in survival mode
     * @param stack
     *            - the player's held item (should've already verified it isn't null)
     * @param leftClick
     *            - is this being called from {@link #onBlockClicked}? or is it from {@link #onBlockActivated}?
     * @return true if a state changer was found and executed, false otherwise
     */
    protected static boolean doStateChangers(boolean survival, ItemStack stack, boolean leftClick, TileEntityCampfire ctile, EntityPlayer player)
    {
        CampfireStateChanger cstate = CampfireStateChanger.findStateChanger(stack, leftClick, ctile.getType(), ctile.isLit(), ctile.canBeReignited());

        if (cstate != null)
        {
            int result = updateCampfireBlockState(cstate.isExtinguisher(), player, ctile);

            if (result == 1)
            {
                ItemStack returnStack = stack;

                if (survival)
                {
                    ItemStack transformedStack = cstate.onUsingInput(returnStack, player);
                    if (transformedStack != null)
                        returnStack = transformedStack;
                    else
                        returnStack.stackSize = 0;
                }
                else
                {
                    ItemStack returnStackCopy = ItemStack.copyItemStack(returnStack);
                    ItemStack transformedStack = cstate.onUsingInput(returnStackCopy, player);
                    if (transformedStack != null && transformedStack != returnStackCopy)
                    {
                        if (!player.inventory.addItemStackToInventory(transformedStack))
                            player.dropPlayerItemWithRandomChoice(transformedStack, false);
                    }
                }

                if (cstate.hasOutputs())
                {
                    ItemStack outputStack = ItemStack.copyItemStack(cstate.getOutput());
                    if (!MiscUtil.putStackInExistingSlots(player.inventory, outputStack, true))
                    {
                        if (returnStack.stackSize <= 0)
                            returnStack = outputStack;
                        else if (!player.inventory.addItemStackToInventory(outputStack))
                            player.dropPlayerItemWithRandomChoice(outputStack, false);
                    }
                }

                if (returnStack != stack || returnStack.stackSize <= 0)
                {
                    if (returnStack.getItem() != stack.getItem())
                        returnStack.animationsToGo = 5;
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, returnStack.stackSize <= 0 ? null : returnStack);
                    player.inventoryContainer.detectAndSendChanges();
                }
            }

            return result != 0;
        }
        return false;
    }

    /**
     * Updates a campfire's block between the lit and unlit ("base") version. Posts a {@link CampfireStateChangeEvent} before it does so. If there isn't a campfire there, or the
     * campfire can't accept the state change, nothing happens.
     * 
     * @param mode
     *            - 0 to extinguish the campfire. 1 to ignite the campfire. 2 to reignite the campfire.
     * @param player
     *            - may be null
     * @param ctile
     *            - the campfire tile entity
     * @return returns 0 if the block wasn't a valid target to state change. otherwise, returns 1 if the state changer should be used up and 2 if it shouldn't be.
     */
    public static int updateCampfireBlockState(int mode, @Nullable EntityPlayer player, TileEntityCampfire ctile)
    {
        World world = ctile.getWorldObj();
        int x = ctile.xCoord;
        int y = ctile.yCoord;
        int z = ctile.zCoord;
        Block oldBlock = world.getBlock(x, y, z);

        // verify that this state change should happen
        if (mode < 0 || mode > 2 || !(oldBlock instanceof BlockCampfire) || (ctile.isLit() == (mode == 1)) || (!ctile.canBeReignited() && mode == 2))
            return 0;

        int meta = world.getBlockMetadata(x, y, z);

        CampfireStateChangeEvent event = new CampfireStateChangeEvent(x, y, z, world, (BlockCampfire) oldBlock, meta, mode, player);
        MinecraftForge.EVENT_BUS.post(event);

        if (!event.isCanceled())
        {
            stateChanging = true;

            Block newBlock = CampfireBackportBlocks.getBlockFromLitAndType(mode != 0, ((BlockCampfire) oldBlock).getType());

            if (mode != 2)
                world.setBlock(x, y, z, newBlock, meta, 3);

            if (!world.isRemote)
            {
                switch (mode)
                {
                case 1:
                    ctile.resetRegenWaitTimer();
                case 2: // fallthrough
                {
                    ctile.checkSignal();
                    ctile.resetLife(true);
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);
                    break;
                }
                default:
                {
                    ctile.playFizzAndAddSmokeServerSide(20, 0.45);
                    ctile.popItems();
                    break;
                }
                }
            }

            ctile.markDirty();
            ctile.updateContainingBlockInfo();
            ctile.validate();

            stateChanging = false;
        }
        else if (event.useGoods) // if the event was canceled but we're still going to use up state changers, play a sound to indicate that
        {
            if (!world.isRemote)
            {
                if (mode != 0)
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);
                else
                    world.playSoundEffect(x + 0.5, y + 0.4375, z + 0.5, "random.fizz", 0.5F, RAND.nextFloat() * 0.4F + 0.8F);
            }
        }

        return event.useGoods ? 1 : 2;
    }

    /**
     * Attempts to either extinguish, ignite, or reignite the campfire. See {@link #updateCampfireBlockState(int, EntityPlayer, TileEntityCampfire)}.
     * 
     * @param extinguisher
     *            - if true, will try to extinguish the campfire. if false, will try to either ignite or reignite the campfire.
     */
    public static int updateCampfireBlockState(boolean extinguisher, @Nullable EntityPlayer player, TileEntityCampfire ctile)
    {
        return updateCampfireBlockState(extinguisher ? 0 : (ctile.isLit() ? 2 : 1), player, ctile);
    }

    /**
     * Attempts to ignite or reignite the campfire at the position. See {@link #updateCampfireBlockState(int, EntityPlayer, TileEntityCampfire)}.
     */
    public static int igniteOrReigniteCampfire(@Nullable EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityCampfire)
        {
            TileEntityCampfire ctile = (TileEntityCampfire) tile;
            if (!ctile.isLit())
                return updateCampfireBlockState(1, player, ctile);
            else if (ctile.canBeReignited())
                return updateCampfireBlockState(2, player, ctile);
        }
        return 0;
    }

    /**
     * Attempts to extinguish the campfire at the position. See {@link #updateCampfireBlockState(int, EntityPlayer, TileEntityCampfire)}.
     */
    public static int extinguishCampfire(@Nullable EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityCampfire && ((TileEntityCampfire) tile).isLit())
            return updateCampfireBlockState(0, player, (TileEntityCampfire) tile);
        else
            return 0;
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
    public void registerBlockIcons(IIconRegister iconreg)
    {
        this.blockIcon = iconreg.registerIcon(Reference.MODID + ":" + "campfire_log");

        if (EnumCampfireType.isRegular(getType()))
            this.fire = iconreg.registerIcon(Reference.MODID + ":" + "campfire_fire");
        else
            this.fire = iconreg.registerIcon(Reference.MODID + ":" + "soul_campfire_fire");
    }

    /**
     * The lit log icon is an {@link InterpolatedIcon}. It's registered from {@link connor135246.campfirebackport.util.CampfireBackportEventHandler#onTextureStitchPre}.
     */
    @SideOnly(Side.CLIENT)
    public void setLitLogIcon(IIcon litLog)
    {
        this.litLog = litLog;
    }

    /**
     * if meta is -2, returns the lit log icon. if meta is -3, return the fire icon. otherwise, returns the normal icon.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta)
    {
        return meta == -2 ? this.litLog : (meta == -3 ? this.fire : this.blockIcon);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getItemIconName()
    {
        if (EnumCampfireType.isRegular(getType()))
            return Reference.MODID + ":" + "campfire_base";
        else
            return Reference.MODID + ":" + "soul_campfire_base";
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
        return renderId;
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
     * {@link #block} is the campfire block before it attempts to be changed.<br>
     * {@link #mode} is how the campfire's state is changing. 0 means extinguish, 1 means ignite, 2 means reignite.<br>
     * If the campfire's state is changing due to a player using state changer, {@link #player} is that player. Otherwise, it's null!<br>
     * If the campfire's state is changing due to a state changer, you can set {@link #useGoods} to false to stop the state changer from being used up. You may want to set this to
     * false if you cancel the event.<br>
     * <br>
     * This event is posted from {@link BlockCampfire#updateCampfireBlockState} on the {@link MinecraftForge#EVENT_BUS}.<br>
     * Note that this event is posted to both the client and server sides if the campfire is changing due to a player using a state changer. At other times, it's only posted to the
     * server side.<br>
     * This event is {@link Cancelable}.<br>
     */
    @Cancelable
    public static class CampfireStateChangeEvent extends Event
    {
        public final int x;
        public final int y;
        public final int z;
        public final World world;
        /** the campfire block BEFORE changing state */
        public final BlockCampfire block;
        public final int blockMetadata;
        /** 0 means extinguish, 1 means ignite, 2 means reignite. */
        public final int mode;
        public final @Nullable EntityPlayer player;
        /** if true, the state changer (e.g. the player's flint and steel) will be used up afterward, whether the event is canceled or not */
        public boolean useGoods = true;

        public CampfireStateChangeEvent(int x, int y, int z, World world, BlockCampfire block, int blockMetadata, int mode, @Nullable EntityPlayer player)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
            this.block = block;
            this.blockMetadata = blockMetadata;
            this.mode = mode;
            this.player = player;
        }

    }

}
