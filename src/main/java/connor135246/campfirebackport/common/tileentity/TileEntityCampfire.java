package connor135246.campfirebackport.common.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
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
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityCampfire extends TileEntity implements ISidedInventory
{
    // final variables
    private static final Random RAND = new Random();
    private static final int[] slotsSides = new int[] { 0, 1, 2, 3 };
    private static final int[] slotsEnds = new int[] {};
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
    // variables that don't need to be saved to nbt
    private int animTimer = RAND.nextInt(32);
    private boolean rainAndSky = false;
    private boolean firstTick = true;
    private boolean refreshThis = false;

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
                checkSignal(getWorldObj(), xCoord, yCoord, zCoord, this);
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
            }
        }
        else
        {
            if (getThisLit())
                addParticles();

            // fixes the animation looking a little weird right when placed
            if (!firstTick)
                ++animTimer;
            else
                firstTick = false;
            
            if (animTimer == 31000000)
                animTimer = 0;
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
                    CampfireRecipe crecipe = CampfireRecipe.findRecipe(itemstack, getThisType());

                    if (crecipe != null)
                    {
                        if (crecipe.isMultiInput())
                        {
                            skips = doMultiInputCooking(crecipe, slot);
                        }
                        else
                        {
                            popStackedItem(crecipe.getOutput());
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
        boolean stackIn = !crecipe.isOreDictRecipe();
        boolean[] skips = new boolean[] { false, false, false, false };

        for (int s = 0; s < getSizeInventory(); ++s)
        {
            if (s == startSlot)
                continue;

            ItemStack sStack = getStackInSlot(s);
            if (sStack != null)
            {
                if (stackIn ? CampfireRecipe.matchesTheStack(crecipe, sStack) : CampfireRecipe.matchesTheOre(crecipe, sStack))
                {
                    skips[s] = true;

                    if (getCookingTimeInSlot(s) >= getTotalCookingTimeInSlot(s))
                    {
                        found.add(s);

                        if (found.size() == target)
                        {
                            popStackedItem(crecipe.getOutput());
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
     * Generates big smoke particles above the campfire, vanilla smoke particles above the items in the campfire, and vanilla smoke particles above the middle if it's raining.
     */
    public void addParticles()
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
            for (int i = 0; i < RAND.nextInt(2) + 2; ++i)
                CampfireBackport.proxy.generateBigSmokeParticles(getWorldObj(), xCoord, yCoord, zCoord, isSignalFire());
        }

        if (rainAndSky)
        {
            for (int i = 0; i < RAND.nextInt(multiplier); ++i)
                getWorldObj().spawnParticle("smoke", (double) xCoord + RAND.nextDouble(), (double) yCoord + 0.9, (double) zCoord + RAND.nextDouble(), 0.0D,
                        0.0D, 0.0D);
        }
    }

    /**
     * Checks if the block below is a signal fire block, and updates the tile entity's signalFire state.
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     * @param tilecamp
     *            - the tile entity to update
     */
    public static void checkSignal(World world, int x, int y, int z, TileEntityCampfire tilecamp)
    {
        Block block = world.getBlock(x, y - 1, z);

        if (block == Blocks.air)
        {
            if (tilecamp.isSignalFire())
                tilecamp.setSignalFire(false);
            return;
        }

        int meta = world.getBlockMetadata(x, y - 1, z);

        if (CampfireBackportConfig.signalFireBlocks.get(block) != null)
        {
            if (CampfireBackportConfig.signalFireBlocks.get(block) == -1 || CampfireBackportConfig.signalFireBlocks.get(block) == meta)
            {
                if (!tilecamp.isSignalFire())
                    tilecamp.setSignalFire(true);
                return;
            }
        }
        else
        {
            for (int id : OreDictionary.getOreIDs(new ItemStack(block)))
            {
                if (CampfireBackportConfig.signalFireOres.contains(OreDictionary.getOreName(id)))
                {
                    if (!tilecamp.isSignalFire())
                        tilecamp.setSignalFire(true);
                    return;
                }
            }
        }
        if (tilecamp.isSignalFire())
            tilecamp.setSignalFire(false);
    }

    public static void checkSignal(World world, int x, int y, int z)
    {
        TileEntityCampfire tilecamp = (TileEntityCampfire) world.getTileEntity(x, y, z);
        checkSignal(world, x, y, z, tilecamp);
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

        if (!compound.hasKey("CampfireType"))
            refreshThis = true;

        setSignalFire(compound.getBoolean("SignalFire"));
        int[] nbtintarray1 = compound.getIntArray("CookingTimes");
        int[] nbtintarray2 = compound.getIntArray("CookingTotalTimes");
        this.regenWaitTimer = compound.getInteger("RegenWaitTimer");
        setThisType(compound.getString("CampfireType"));
        setThisLit(compound.getBoolean("CampfireLit"));
        setThisMeta(compound.getInteger("CampfireMeta"));

        NBTTagList nbttaglist = compound.getTagList("Items", 10);
        inventory = new ItemStack[getSizeInventory()];
        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            byte slot = nbttagcompound1.getByte("Slot");
            if (slot >= 0 && slot < getSizeInventory())
            {
                setStackInSlot(slot, ItemStack.loadItemStackFromNBT(nbttagcompound1));
                setCookingTimeInSlot(slot, nbtintarray1[slot]);
                setTotalCookingTimeInSlot(slot, nbtintarray2[slot]);
            }
        }

        if (compound.hasKey("CustomName", 8))
            func_145951_a(compound.getString("CustomName"));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        compound.setBoolean("SignalFire", isSignalFire());
        compound.setIntArray("CookingTimes", cookingTimes);
        compound.setIntArray("CookingTotalTimes", cookingTotalTimes);
        compound.setInteger("RegenWaitTimer", regenWaitTimer);
        compound.setString("CampfireType", getThisType());
        compound.setBoolean("CampfireLit", getThisLit());
        compound.setInteger("CampfireMeta", getThisMeta());

        NBTTagList nbttaglist = new NBTTagList();
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            if (inventory[slot] != null)
            {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) slot);
                inventory[slot].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        compound.setTag("Items", nbttaglist);

        if (hasCustomInventoryName())
            compound.setString("CustomName", getInventoryName());
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
        if (stack != null)
        {
            setStackInSlot(slot, stack.splitStack(1));
            setTotalCookingTimeInSlot(slot, stack);
        }
        else
        {
            setStackInSlot(slot, null);
            setTotalCookingTimeInSlot(slot, 600);
        }

        resetCookingTimeInSlot(slot);

        markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return CampfireRecipe.findRecipe(stack, getThisType()) != null;
    }

    /**
     * Tries to add the ItemStack to the campfire's inventory. Called by {@link BlockCampfire#onBlockActivated(World, int, int, int, EntityPlayer, int, float, float, float)
     * BlockCampfire#onBlockActivated}.
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
                popItem(itemstack);
                setStackInSlot(slot, null);
            }
        }
    }

    /**
     * Drops the given ItemStack into the world above the campfire, one stackSize at a time.
     * 
     * @param itemstack
     */
    public void popStackedItem(ItemStack itemstack)
    {
        ItemStack oneStack = ItemStack.copyItemStack(itemstack);
        oneStack.stackSize = 1;

        for (int i = 0; i < itemstack.stackSize; ++i)
            popItem(oneStack.copy());
    }

    /**
     * Drops the given ItemStack into the world above the campfire.
     * 
     * @param itemstack
     */
    public void popItem(ItemStack itemstack)
    {
        EntityItem entityitem = new EntityItem(getWorldObj(),
                xCoord + RAND.nextDouble() * 0.75 + 0.125,
                yCoord + RAND.nextDouble() * 0.75 + 0.5,
                zCoord + RAND.nextDouble() * 0.75 + 0.125,
                ItemStack.copyItemStack(itemstack));

        if (itemstack.hasTagCompound())
            entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());

        entityitem.motionX = RAND.nextGaussian() * 0.05;
        entityitem.motionY = RAND.nextGaussian() * 0.05 + 0.2;
        entityitem.motionZ = RAND.nextGaussian() * 0.05;

        getWorldObj().spawnEntityInWorld(entityitem);
    }

    @Override
    public String getInventoryName()
    {
        return hasCustomInventoryName() ? customName : "Cook Stuff";
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

    /**
     * Sets the cooking time in the slot based on the cooking time of the ItemStack. If the ItemStack doesn't have a recipe, sets it to 600.
     * 
     * @param slot
     *            - the slot receiving an ItemStack
     * @param stack
     *            - the ItemStack being added
     */
    public void setTotalCookingTimeInSlot(int slot, ItemStack stack)
    {
        CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack, getThisType());

        if (crecipe != null)
            setTotalCookingTimeInSlot(slot, crecipe.getCookingTime());
        else
            setTotalCookingTimeInSlot(slot, 600);
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
