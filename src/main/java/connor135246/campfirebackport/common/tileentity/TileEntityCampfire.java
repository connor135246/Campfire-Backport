package connor135246.campfirebackport.common.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.WorldServer;
import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.common.crafting.GenericCustomInput;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityCampfire extends TileEntity implements ISidedInventory
{
    // final variables
    private static final Random RAND = new Random();
    private static final int[] slotsSides = new int[] { 0, 1, 2, 3 };
    private static final int[] slotsEnds = new int[] {};
    // NBT keys
    public static final String items = "Items",
            slot = "Slot",
            cookTimes = "CookingTimes",
            totalTimes = "CookingTotalTimes",
            signal = "SignalFire",
            regenWait = "RegenWaitTimer",
            campType = "CampfireType",
            campLit = "CampfireLit",
            campMeta = "CampfireMeta",
            lifetime = "Life",
            name = "CustomName";
    // main variables
    private ItemStack[] inventory = new ItemStack[4];
    private int[] cookingTimes = new int[4];
    private int[] cookingTotalTimes = new int[4];
    private boolean signalFire = false;
    private String customName;
    private int regenWaitTimer = -1;
    private String thisType;
    private Boolean thisLit;
    private Integer thisMeta;
    private int life = 0;
    // variables that don't need to be saved to NBT
    private boolean rainAndSky = false;
    private boolean firstTick = true;
    private boolean refreshThis = false;
    // only used client side
    private int animTimer = RAND.nextInt(32);

    @Override
    public void updateEntity()
    {
        if (getWorldObj().getTotalWorldTime() % 100L == 0L && RAND.nextInt(5) == 0)
            rainAndSky = getWorldObj().isRaining() && getWorldObj().canBlockSeeTheSky(xCoord, yCoord, zCoord)
                    && getWorldObj().getBiomeGenForCoords(xCoord, zCoord).canSpawnLightningBolt();

        if (!getWorldObj().isRemote)
        {
            if (refreshThis)
                refreshThis();

            if (firstTick)
            {
                checkSignal();
                resetLife();
                firstTick = false;
            }

            // literally just for the waila display
            if (getWorldObj().getTotalWorldTime() % 40L == 0)
                this.markDirty();

            if (getThisLit())
            {
                cookAndDrop();

                if (CampfireBackportConfig.regenCampfires.matches(getThisType()))
                    heal();

                if (rainAndSky && CampfireBackportConfig.putOutByRain.matches(getThisType()))
                {
                    BlockCampfire.updateCampfireBlockState(false, getWorldObj(), xCoord, yCoord, zCoord, getThisType());
                    rainAndSky = false;
                }

                if (CampfireBackportConfig.burnOutTimer[getThisTypeToInt()] != -1
                        && (!isSignalFire() || (isSignalFire() && CampfireBackportConfig.signalFiresBurnOut.matches(getThisType()))))
                {
                    decrementLife();

                    if (getLife() <= 0)
                    {
                        if (RAND.nextDouble() < CampfireBackportConfig.burnToNothingChances[getThisTypeToInt()])
                        {
                            popItem(ItemStack.copyItemStack(CampfireBackportConfig.campfireDropsStacks[getThisTypeToInt()]), getWorldObj(), xCoord, yCoord,
                                    zCoord);

                            playFizzAndAddSmokeServerSide(65, 0.25);

                            getWorldObj().setBlock(xCoord, yCoord, zCoord, Blocks.air);
                        }
                        else
                            BlockCampfire.updateCampfireBlockState(false, getWorldObj(), xCoord, yCoord, zCoord, getThisType());
                    }
                }
            }
        }
        else
        {
            if (getThisLit())
                addParticles();

            // fixes the animation looking a little weird right when placed
            if (!firstTick)
                incrementAnimTimer();

            if (firstTick)
                firstTick = false;
        }

    }

    /**
     * Applies regeneration effects periodically.
     */
    private void heal()
    {
        if (regenWaitTimer == 0)
        {
            int[] regenValues = getThisType().equals(EnumCampfireType.REGULAR) ? CampfireBackportConfig.regularRegen
                    : CampfireBackportConfig.soulRegen;

            List<EntityPlayer> playerlist = getWorldObj().getEntitiesWithinAABB(EntityPlayer.class,
                    AxisAlignedBB.getBoundingBox(xCoord - regenValues[2], yCoord - regenValues[2],
                            zCoord - regenValues[2],
                            xCoord + regenValues[2], yCoord + regenValues[2],
                            zCoord + regenValues[2]));

            for (EntityPlayer player : playerlist)
            {
                if (!player.isPotionActive(Potion.regeneration))
                    player.addPotionEffect(new PotionEffect(Potion.regeneration.id, regenValues[1], regenValues[0], true));
            }
        }

        if (regenWaitTimer <= 0)
            resetRegenWaitTimer();

        --regenWaitTimer;
    }

    /**
     * Cooks inventory items and drops them when done.
     */
    private void cookAndDrop()
    {
        ItemStack itemstack;
        // if a multi-input recipe is found, we go over all the items in the campfire there. no reason to go over them again.
        boolean[] skips = new boolean[] { false, false, false, false };

        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            itemstack = getStackInSlot(slot);
            if (itemstack != null)
            {
                incrementCookingTimeInSlot(slot);

                if (skips[slot])
                    continue;

                if (getCookingTimeInSlot(slot) >= getTotalCookingTimeInSlot(slot))
                {
                    CampfireRecipe crecipe = CampfireRecipe.findRecipe(itemstack, getThisType(), isSignalFire());

                    if (crecipe != null)
                    {
                        if (crecipe.isMultiInput())
                        {
                            skips = doMultiInputCooking(crecipe, slot);
                        }
                        else
                        {
                            popStackedItem(crecipe.getOutput(), getWorldObj(), xCoord, yCoord, zCoord);
                            setInventorySlotContents(slot, null);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds matching stacks for multi-input recipes. Moved to its own method just because {@link #cookAndDrop()} was getting huge.
     * 
     * @param crecipe
     * @param startSlot
     *            - the slot that this method was called from
     * @return the slots to skip checking in {@link #cookAndDrop()}, since they're already being checked here
     */
    private boolean[] doMultiInputCooking(CampfireRecipe crecipe, int startSlot)
    {
        int target = crecipe.getInputSize();
        ArrayList<Integer> found = new ArrayList<Integer>(4);
        found.add(startSlot);
        boolean[] skips = new boolean[] { false, false, false, false };

        for (int s = 0; s < getSizeInventory(); ++s)
        {
            if (s == startSlot)
                continue;

            ItemStack sStack = getStackInSlot(s);
            if (sStack != null)
            {
                if (GenericCustomInput.matches(crecipe, sStack))
                {
                    skips[s] = true;

                    if (getCookingTimeInSlot(s) >= getTotalCookingTimeInSlot(s))
                    {
                        found.add(s);

                        if (found.size() == target)
                        {
                            popStackedItem(crecipe.getOutput(), getWorldObj(), xCoord, yCoord, zCoord);
                            for (int k : found)
                                setInventorySlotContents(k, null);
                            return skips;
                        }
                    }
                }
            }
        }
        return skips;
    }

    /**
     * Generates big smoke particles above the campfire, vanilla smoke particles above the items in the campfire, and some vanilla smoke particles above the middle if it's raining.
     */
    private void addParticles()
    {
        int setting = (Minecraft.getMinecraft().gameSettings.particleSetting + 1);
        int multiplier = setting % 2 == 1 ? setting ^ 2 : setting;

        ItemStack stack;
        int[] iro = RenderCampfire.getRenderSlotMappingFromMeta(getBlockMetadata());
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            stack = getStackInSlot(slot);

            if (stack != null)
            {
                if (RAND.nextFloat() < multiplier / 15F)
                {
                    double[] position = RenderCampfire.getRenderPositionFromRenderSlot(iro[slot], true);
                    getWorldObj().spawnParticle("smoke", position[0] + xCoord, position[1] + 0.06 + yCoord, position[2] + zCoord, 0.0, 5.0E-4, 0.0);
                }
            }
        }

        if (RAND.nextFloat() < 0.11F)
        {
            Block block = Blocks.air;

            if (CampfireBackportConfig.colourfulSmoke.matches(getThisType()))
            {
                Block blockBelow = getWorldObj().getBlock(xCoord, yCoord - 1, zCoord);

                if (blockBelow != Blocks.air && (getWorldObj().getBlockPowerInput(xCoord, yCoord, zCoord) > 0
                        || getWorldObj().isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)))
                {
                    block = blockBelow;
                }
            }

            for (int i = 0; i < RAND.nextInt(2) + 2; ++i)
            {
                CampfireBackport.proxy.generateBigSmokeParticles(getWorldObj(), xCoord, yCoord, zCoord, isSignalFire(), block);
            }
        }

        if (rainAndSky)
        {
            for (int i = 0; i < RAND.nextInt(multiplier); ++i)
                getWorldObj().spawnParticle("smoke", (double) xCoord + RAND.nextDouble(), (double) yCoord + 0.9, (double) zCoord + RAND.nextDouble(), 0.0D,
                        0.0D, 0.0D);
        }
    }

    /**
     * Generates a lot of vanilla smoke particles above the campfire and plays a fizzing sound.<br>
     * It's server side particle spawning for when a campfire is extinguished.
     * 
     * @param particles
     *            - approx number of particles to spawn
     * @param height
     *            - additional height to add to particle spawn location
     */
    public void playFizzAndAddSmokeServerSide(int particles, double height)
    {
        if (!getWorldObj().isRemote)
        {
            getWorldObj().playSoundEffect((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D, "random.fizz", 0.5F,
                    RAND.nextFloat() * 0.4F + 0.8F);

            ((WorldServer) getWorldObj()).func_147487_a("smoke", (double) xCoord + 0.5D, (double) yCoord + height, (double) zCoord + 0.5D,
                    MathHelper.getRandomIntegerInRange(RAND, particles - 5, particles + 5), 0.3D, 0.1D, 0.3D, 0.005D);
        }
    }

    /**
     * Checks if the block below is a signal fire block, and updates the {@link #signalFire} state.
     * 
     * @return {@link #signalFire}
     */
    public boolean checkSignal()
    {
        Block block = getWorldObj().getBlock(xCoord, yCoord - 1, zCoord);

        if (block == Blocks.air)
        {
            if (isSignalFire())
                setSignalFire(false);
            return false;
        }

        int meta = getWorldObj().getBlockMetadata(xCoord, yCoord - 1, zCoord);

        if (CampfireBackportConfig.signalFireBlocks.get(block) != null)
        {
            if (CampfireBackportConfig.signalFireBlocks.get(block) == OreDictionary.WILDCARD_VALUE
                    || CampfireBackportConfig.signalFireBlocks.get(block) == meta)
            {
                if (!isSignalFire())
                    setSignalFire(true);
                return true;
            }
        }
        else
        {
            for (int id : OreDictionary.getOreIDs(new ItemStack(block)))
            {
                if (CampfireBackportConfig.signalFireOres.contains(id))
                {
                    if (!isSignalFire())
                        setSignalFire(true);
                    return true;
                }
            }
        }
        if (isSignalFire())
            setSignalFire(false);
        return false;
    }

    /**
     * Helper function that redirects to {@link #checkSignal()} for the campfire at the world and block given.
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public static void checkSignal(World world, int x, int y, int z)
    {
        TileEntityCampfire tilecamp = (TileEntityCampfire) world.getTileEntity(x, y, z);
        tilecamp.checkSignal();
    }

    @Override
    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z)
    {
        if (newBlock instanceof BlockCampfire)
            return false;

        return true;
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
    {
        super.onDataPacket(net, pkt);
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tagCompound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        if (!compound.hasKey(campType))
            refreshThis = true;

        setSignalFire(compound.getBoolean(signal));

        // make sure we don't get an IndexOutOfBoundsError later if the data wasn't found
        int[] nbtintarray1 = compound.hasKey(cookTimes) ? compound.getIntArray(cookTimes) : new int[] { 0, 0, 0, 0 };
        int[] nbtintarray2 = compound.hasKey(totalTimes) ? compound.getIntArray(totalTimes) : new int[] { 600, 600, 600, 600 };

        this.regenWaitTimer = compound.getInteger(regenWait);

        setThisType(compound.getString(campType));
        setThisLit(compound.getBoolean(campLit));
        setThisMeta(compound.getInteger(campMeta));

        this.life = compound.getInteger(lifetime);

        NBTTagList nbttaglist = compound.getTagList(items, 10);
        inventory = new ItemStack[getSizeInventory()];
        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            byte slot = nbttagcompound1.getByte(this.slot);
            if (slot >= 0 && slot < getSizeInventory())
            {
                setStackInSlot(slot, ItemStack.loadItemStackFromNBT(nbttagcompound1));
                setCookingTimeInSlot(slot, nbtintarray1[slot]);
                setTotalCookingTimeInSlot(slot, nbtintarray2[slot]);
            }
        }

        if (compound.hasKey(name, 8))
            func_145951_a(compound.getString(name));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        compound.setBoolean(signal, isSignalFire());

        compound.setIntArray(cookTimes, cookingTimes);
        compound.setIntArray(totalTimes, cookingTotalTimes);

        compound.setInteger(regenWait, regenWaitTimer);

        compound.setString(campType, getThisType());
        compound.setBoolean(campLit, getThisLit());
        compound.setInteger(campMeta, getThisMeta());

        compound.setInteger(lifetime, life);

        NBTTagList nbttaglist = new NBTTagList();
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            if (inventory[slot] != null)
            {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte(this.slot, (byte) slot);
                inventory[slot].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        compound.setTag(items, nbttaglist);

        if (hasCustomInventoryName())
            compound.setString(name, getInventoryName());
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        if (hasWorldObj())
            getWorldObj().markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public ItemStack decrStackSize(int slot, int decrease)
    {
        ItemStack itemstack = getStackInSlot(slot);

        if (itemstack != null)
        {
            if (itemstack.stackSize <= decrease)
            {
                setStackInSlot(slot, null);
                return itemstack;
            }
            else
            {
                itemstack.splitStack(decrease);

                if (getStackInSlot(slot).stackSize == 0)
                    setStackInSlot(slot, null);

                return itemstack;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot)
    {
        ItemStack itemstack = getStackInSlot(slot);

        if (itemstack != null)
        {
            setStackInSlot(slot, null);
            return itemstack;
        }
        else
            return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        setStackInSlot(slot, stack == null ? null : stack.splitStack(1));
        setTotalCookingTimeInSlot(slot, stack);

        resetCookingTimeInSlot(slot);

        markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return CampfireRecipe.findRecipe(stack, getThisType(), isSignalFire()) != null;
    }

    /**
     * Tries to add the <code>ItemStack</code> to the campfire's inventory. Called by
     * {@link BlockCampfire#onBlockActivated(World, int, int, int, EntityPlayer, int, float, float, float) BlockCampfire#onBlockActivated}.
     * 
     * @param stack
     *            - the stack (in the player's hand)
     * @return true if the item was added to the campfire's inventory, false otherwise
     */
    public boolean tryInventoryAdd(ItemStack stack)
    {
        int slot = getNextAcceptableSlot(stack);
        if (slot != -1)
        {
            setInventorySlotContents(slot, stack);
            return true;
        }
        else
            return false;
    }

    /**
     * Finds the next slot that's able to accept the stack, or -1 if none. Called by {@link #tryInventoryAdd(ItemStack)}.
     * 
     * @param stack
     *            - the stack to check
     * @return the next empty slot, or -1 if all slots are full or if the item isn't valid
     */
    public int getNextAcceptableSlot(ItemStack stack)
    {
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            if (getStackInSlot(slot) == null)
                if (isItemValidForSlot(slot, stack))
                    return slot;
        }
        return -1;
    }

    /**
     * Drops all the campfire's items into the world.
     */
    public void popItems()
    {
        for (int slot = 0; slot < this.getSizeInventory(); ++slot)
        {
            ItemStack itemstack = this.getStackInSlot(slot);

            if (itemstack != null)
            {
                popItem(itemstack, getWorldObj(), xCoord, yCoord, zCoord);
                setInventorySlotContents(slot, null);
            }
        }
    }

    /**
     * Drops the given <code>ItemStack</code>, one stackSize at a time.
     * 
     * @param itemstack
     */
    public static void popStackedItem(ItemStack itemstack, World world, int x, int y, int z)
    {
        ItemStack oneStack = ItemStack.copyItemStack(itemstack);
        oneStack.stackSize = 1;

        for (int i = 0; i < itemstack.stackSize; ++i)
            popItem(ItemStack.copyItemStack(oneStack), world, x, y, z);
    }

    /**
     * Drops the given <code>ItemStack</code>.
     * 
     * @param itemstack
     */
    public static void popItem(ItemStack itemstack, World world, int x, int y, int z)
    {
        EntityItem entityitem = new EntityItem(world,
                x + RAND.nextDouble() * 0.75 + 0.125,
                y + RAND.nextDouble() * 0.75 + 0.5,
                z + RAND.nextDouble() * 0.75 + 0.125,
                ItemStack.copyItemStack(itemstack));

        if (itemstack.hasTagCompound())
            entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());

        entityitem.motionX = RAND.nextGaussian() * 0.05;
        entityitem.motionY = RAND.nextGaussian() * 0.05 + 0.2;
        entityitem.motionZ = RAND.nextGaussian() * 0.05;

        world.spawnEntityInWorld(entityitem);
    }

    @Override
    public String getInventoryName()
    {
        return hasCustomInventoryName() ? customName : StatCollector.translateToLocal("tile.campfire.inventory.name");
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return customName != null && customName.length() > 0;
    }

    public void func_145951_a(String name)
    {
        customName = name;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return true;
    }

    @Override
    public void openInventory()
    {
        ;
    }

    @Override
    public void closeInventory()
    {
        ;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side)
    {
        if (CampfireBackportConfig.automation.matches(getThisType()))
            return side == 1 ? slotsEnds : (side == 0 ? slotsEnds : slotsSides);
        else
            return slotsEnds;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        if (CampfireBackportConfig.automation.matches(getThisType()))
            return side == 1 ? false : (side == 0 ? false : isItemValidForSlot(slot, stack));
        else
            return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side)
    {
        return false;
    }

    // Getters and Setters

    /**
     * Sets the cooking time in the slot based on the campfire recipe of the <code>ItemStack</code>. If the <code>ItemStack</code> doesn't have a recipe or the
     * <code>ItemStack</code> is null, sets it to 600.
     * 
     * @param slot
     *            - the slot receiving an <code>ItemStack</code>
     * @param stack
     *            - the <code>ItemStack</code> being added
     */
    public void setTotalCookingTimeInSlot(int slot, ItemStack stack)
    {
        if (stack != null)
        {
            CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack, getThisType(), isSignalFire());

            if (crecipe != null)
            {
                setTotalCookingTimeInSlot(slot, crecipe.getCookingTime());
                return;
            }
        }
        setTotalCookingTimeInSlot(slot, 600);
    }

    // lit
    /**
     * Returns the cached lit state of this tile entity. If it hasn't been cached yet, gets it from the world, but if this isn't on a campfire block, just returns a lit campfire.
     * 
     * @return true if this tile entity thinks it's lit, false otherwise
     */
    public boolean getThisLit()
    {
        if (thisLit != null)
            return thisLit;
        else if (getWorldObj().getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
            return setThisLit(((BlockCampfire) getWorldObj().getBlock(xCoord, yCoord, zCoord)).isLit());
        else
            return setThisLit(true);
    }

    public boolean setThisLit(boolean lit)
    {
        return thisLit = lit;
    }

    // type
    /**
     * Returns the cached type of this tile entity. If it hasn't been cached yet, gets it from the world, but if this isn't on a campfire block, just returns a regular campfire.
     * 
     * @return the campfire type this tile entity thinks it is
     */
    public String getThisType()
    {
        if (thisType != null)
            return thisType;
        else if (getWorldObj().getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
            return setThisType(((BlockCampfire) getWorldObj().getBlock(xCoord, yCoord, zCoord)).getType());
        else
            return setThisType(EnumCampfireType.REGULAR);
    }

    public String setThisType(String type)
    {
        return thisType = type;
    }

    public int getThisTypeToInt()
    {
        return EnumCampfireType.toInt(getThisType());
    }

    // thisMeta
    /**
     * Returns this tile entity's cached meta. If it hasn't been set, gets it from the world, but if this isn't on a campfire block, just returns 2.
     *
     * @return the meta this tile entity thinks the block it's in has
     */
    public int getThisMeta()
    {
        if (thisMeta != null)
            return thisMeta;
        else if (getWorldObj().getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
            return setThisMeta(getWorldObj().getBlockMetadata(xCoord, yCoord, zCoord));
        else
            return setThisMeta(2);
    }

    public int setThisMeta(int meta)
    {
        return thisMeta = meta;
    }

    /**
     * Refreshes all cached variables. Currently only called if certain values aren't found in NBT that should be, which should only happen with old (v1.3 or earlier) campfires.
     * These old campfire tile entities would otherwise forget whether they're lit or not, and which way they're facing.
     */
    public void refreshThis()
    {
        refreshThis = false;

        thisLit = null;
        getThisLit();

        thisType = null;
        getThisType();

        thisMeta = null;
        getThisMeta();
    }

    // animTimer
    public int getAnimTimer()
    {
        return animTimer;
    }

    public void setAnimTimer(int frame)
    {
        animTimer = frame;
    }

    public void incrementAnimTimer()
    {
        ++animTimer;
        if (animTimer == Integer.MAX_VALUE)
            animTimer = 0;
    }

    // life
    public int getLife()
    {
        return life;
    }

    public void decrementLife()
    {
        if (life > Integer.MIN_VALUE)
            --life;
    }

    public void resetLife()
    {
        life = Math.round(CampfireBackportConfig.burnOutTimer[getThisTypeToInt()] * (0.9F + RAND.nextFloat() * 0.2F));
    }

    // signalFire
    public boolean isSignalFire()
    {
        return signalFire;
    }

    public void setSignalFire(boolean set)
    {
        signalFire = set;
        markDirty();
    }

    // cookingTimes
    public int getCookingTimeInSlot(int slot)
    {
        return cookingTimes[slot];
    }

    public void setCookingTimeInSlot(int slot, int time)
    {
        cookingTimes[slot] = time;
    }

    public void resetCookingTimeInSlot(int slot)
    {
        setCookingTimeInSlot(slot, 0);
    }

    public void incrementCookingTimeInSlot(int slot)
    {
        if (cookingTimes[slot] < Integer.MAX_VALUE)
            ++cookingTimes[slot];
    }

    // cookingTotalTimes
    public int getTotalCookingTimeInSlot(int slot)
    {
        return cookingTotalTimes[slot];
    }

    public void setTotalCookingTimeInSlot(int slot, int time)
    {
        cookingTotalTimes[slot] = time;
    }

    // heal
    public void resetRegenWaitTimer()
    {
        regenWaitTimer = Math.round(getThisType().equals(EnumCampfireType.REGULAR) ? CampfireBackportConfig.regularRegen[3]
                : CampfireBackportConfig.soulRegen[3] * (0.9F + RAND.nextFloat() * 0.2F));
    }

    // inventory
    public void setStackInSlot(int slot, ItemStack stack)
    {
        inventory[slot] = stack;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        return inventory[i];
    }

    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    public ItemStack[] getInventory()
    {
        return inventory;
    }

}
