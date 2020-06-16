package connor135246.campfirebackport.common.tileentity;

import java.util.List;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.util.EnumCampfireType;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
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
    private int animTimer = 0;
    private String thisType;
    private Boolean thisLit;
    private Integer thisMeta;
    // variables that don't need to be saved to nbt
    private boolean rainAndSky = false;
    private boolean firstTick = false;
    private boolean refreshThis = false;

    public void updateEntity()
    {
        if (this.worldObj.getTotalWorldTime() % 100L == 0L && RAND.nextInt(5) == 0)
            rainAndSky = worldObj.isRaining() && worldObj.canBlockSeeTheSky(xCoord, yCoord, zCoord)
                    && worldObj.getBiomeGenForCoords(xCoord, zCoord).canSpawnLightningBolt();

        if (!worldObj.isRemote)
        {
            if (refreshThis)
                refreshThis();

            if (getThisLit())
            {
                cookAndDrop();

                if (CampfireBackportConfig.regenCampfires.matches(getThisType()))
                    heal();

                if (rainAndSky && CampfireBackportConfig.putOutByRain.matches(getThisType()))
                    BlockCampfire.updateCampfireBlockState(false, worldObj, xCoord, yCoord, zCoord, getThisType());
            }
        }
        else
        {
            if (getThisLit())
            {
                addParticles();

                if (firstTick)
                    ++animTimer;

                if (animTimer == 31000000)
                    animTimer = 0;

                // fixes the animation looking a little weird right when it starts
                firstTick = true;
            }
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
        for (int i = 0; i < getSizeInventory(); ++i)
        {
            itemstack = getStackInSlot(i);
            if (itemstack != null)
            {
                incrementCookingTimeInSlot(i);
                if (getCookingTimeInSlot(i) >= getTotalCookingTimeInSlot(i))
                {
                    CampfireRecipe crecipe = CampfireRecipe.findRecipe(itemstack, getThisType());

                    if (crecipe != null)
                    {
                        EntityItem entityitem = new EntityItem(worldObj,
                                xCoord + RAND.nextDouble() * 0.75 + 0.125,
                                yCoord + RAND.nextDouble() * 0.75 + 0.5,
                                zCoord + RAND.nextDouble() * 0.75 + 0.125,
                                ItemStack.copyItemStack(crecipe.getOutput()));

                        entityitem.motionX = RAND.nextGaussian() * 0.05;
                        entityitem.motionY = RAND.nextGaussian() * 0.05 + 0.2;
                        entityitem.motionZ = RAND.nextGaussian() * 0.05;

                        worldObj.spawnEntityInWorld(entityitem);

                        setInventorySlotContents(i, null);
                    }
                }
            }
        }
    }

    /**
     * Generates big smoke particles above the campfire, vanilla smoke particles above the items in the campfire, and vanilla smoke particles above the middle if it's raining.
     */
    public void addParticles()
    {
        World world = getWorldObj();
        int setting = (Minecraft.getMinecraft().gameSettings.particleSetting + 1);
        int multiplier = setting % 2 == 1 ? setting ^ 2 : setting;

        ItemStack stack;
        int[] iro = RenderCampfire.getRenderSlotMappingFromMeta(getBlockMetadata());
        for (int i = 0; i < getSizeInventory(); ++i)
        {
            stack = getStackInSlot(i);

            if (stack != null)
            {
                if (RAND.nextFloat() < multiplier / 15F)
                {
                    double[] position = RenderCampfire.getRenderPositionFromRenderSlot(iro[i], true);
                    world.spawnParticle("smoke", position[0] + xCoord, position[1] + 0.06 + yCoord, position[2] + zCoord, 0.0, 5.0E-4, 0.0);
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
                world.spawnParticle("smoke", (double) xCoord + RAND.nextDouble(), (double) yCoord + 0.9, (double) zCoord + RAND.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
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
        NBTTagList nbttaglist = compound.getTagList("Items", 10);
        int[] nbtintarray1 = compound.getIntArray("CookingTimes");
        int[] nbtintarray2 = compound.getIntArray("CookingTotalTimes");
        this.regenWaitTimer = compound.getInteger("RegenWaitTimer");
        this.animTimer = compound.getInteger("AnimTimer");
        this.thisType = compound.getString("CampfireType");
        this.thisLit = compound.getBoolean("CampfireLit");
        this.thisMeta = compound.getInteger("CampfireMeta");
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

        compound.setBoolean("SignalFire", signalFire);
        compound.setIntArray("CookingTimes", cookingTimes);
        compound.setIntArray("CookingTotalTimes", cookingTotalTimes);
        compound.setInteger("RegenWaitTimer", regenWaitTimer);
        compound.setInteger("AnimTimer", animTimer);
        compound.setString("CampfireType", thisType);
        compound.setBoolean("CampfireLit", thisLit);
        compound.setInteger("CampfireMeta", thisMeta);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < getSizeInventory(); ++i)
        {
            if (inventory[i] != null)
            {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        compound.setTag("Items", nbttaglist);

        if (hasCustomInventoryName())
            compound.setString("CustomName", customName);
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        if (hasWorldObj())
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

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
     * Finds the next slot that's able to accept the stack, or -1 if none. Called by {@link #tryInventoryAdd(ItemStack) tryInventoryAdd}.
     * 
     * @param stack
     *            - the stack to check
     * @return the next empty slot, or -1 if all slots are full or if the item isn't valid
     */
    public int getNextAcceptableSlot(ItemStack stack)
    {
        for (int i = 0; i < getSizeInventory(); ++i)
        {
            if (getStackInSlot(i) == null)
                if (isItemValidForSlot(i, stack))
                    return i;
        }
        return -1;
    }

    /**
     * Drops all the campfire's items into the world.
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public void popItems(World world, int x, int y, int z)
    {
        for (int i = 0; i < this.getSizeInventory(); ++i)
        {
            popItem(world, x, y, z, i);
        }
    }

    /**
     * Drops the item in the specified slot into the world, if it's not null.
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     * @param slot
     */
    public void popItem(World world, int x, int y, int z, int slot)
    {
        ItemStack itemstack = this.getStackInSlot(slot);

        if (itemstack != null)
        {
            float f = this.RAND.nextFloat() * 0.8F + 0.1F;
            float f1 = this.RAND.nextFloat() * 0.8F + 0.1F;
            float f2 = this.RAND.nextFloat() * 0.8F + 0.1F;

            EntityItem entityitem = new EntityItem(world, (double) x + f, (double) y + f1, (double) z + f2,
                    new ItemStack(itemstack.getItem(), itemstack.stackSize, itemstack.getItemDamage()));

            if (itemstack.hasTagCompound())
                entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());

            float f3 = 0.05F;
            entityitem.motionX = (double) this.RAND.nextGaussian() * f3;
            entityitem.motionY = (double) this.RAND.nextGaussian() * f3 + 0.2F;
            entityitem.motionZ = (double) this.RAND.nextGaussian() * f3;
            world.spawnEntityInWorld(entityitem);
            this.setStackInSlot(slot, null);
        }
    }

    public String getInventoryName()
    {
        return hasCustomInventoryName() ? customName : "container.campfire";
    }

    public boolean hasCustomInventoryName()
    {
        return customName != null && customName.length() > 0;
    }

    public void func_145951_a(String name)
    {
        customName = name;
    }

    public int getInventoryStackLimit()
    {
        return 1;
    }

    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return true;
    }

    public void openInventory()
    {
        ;
    }

    public void closeInventory()
    {
        ;
    }

    public int[] getAccessibleSlotsFromSide(int side)
    {
        if (CampfireBackportConfig.automation.matches(getThisType()))
            return side == 1 ? slotsEnds : (side == 0 ? slotsEnds : slotsSides);
        else
            return slotsEnds;
    }

    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        if (CampfireBackportConfig.automation.matches(getThisType()))
            return side == 1 ? false : (side == 0 ? false : isItemValidForSlot(slot, stack));
        else
            return false;
    }

    public boolean canExtractItem(int slot, ItemStack stack, int side)
    {
        return false;
    }

    // Getters and Setters

    // lit
    /**
     * Returns the cached lit state of this tile entity. If it hasn't been cached yet, gets it from the world, but if this isn't on a campfire block, invalidates it.
     * 
     * @return true if this tile entity thinks it's lit, false otherwise
     */
    public boolean getThisLit()
    {
        if (thisLit != null)
        {
            return thisLit;
        }
        else if (worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
        {
            return setThisLit(((BlockCampfire) worldObj.getBlock(xCoord, yCoord, zCoord)).isLit());
        }
        else
        {
            this.invalidate();
            CommonProxy.modlog.warn("Found an invalid campfire tile entity. It'll be removed.");
            return setThisLit(false);
        }
    }

    public boolean setThisLit(boolean lit)
    {
        return thisLit = lit;
    }

    // type
    /**
     * Returns the cached type of this tile entity. If it hasn't been cached yet, gets it from the world, but if this isn't on a campfire block, invalidates it.
     * 
     * @return the campfire type this tile entity thinks it is
     */
    public String getThisType()
    {
        if (thisType != null)
        {
            return thisType;
        }
        else if (worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
        {
            return setThisType(((BlockCampfire) worldObj.getBlock(xCoord, yCoord, zCoord)).getType());
        }
        else
        {
            this.invalidate();
            CommonProxy.modlog.warn("Found an invalid campfire tile entity. It'll be removed.");
            return setThisType(EnumCampfireType.REGULAR);
        }
    }

    public String setThisType(String type)
    {
        return thisType = type;
    }

    // thisMeta
    /**
     * Returns this tile entity's cached meta. If it hasn't been set, gets it from the world, but if this isn't on a campfire block, invalidates it.
     *
     * @return the meta this tile entity thinks the block it's in has
     */
    public int getThisMeta()
    {
        if (thisMeta != null)
        {
            return thisMeta;
        }
        else if (worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockCampfire)
        {
            return setThisMeta(worldObj.getBlockMetadata(xCoord, yCoord, zCoord));
        }
        else
        {
            this.invalidate();
            CommonProxy.modlog.warn("Found an invalid campfire tile entity. It'll be removed.");
            return setThisMeta(2);
        }
    }

    public int setThisMeta(int meta)
    {
        return thisMeta = meta;
    }

    /**
     * Refreshes all cached variables. Currently only called if certain values aren't found in NBT that should be, which should only happen with old (v1.3 or earlier) campfires.
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

    // signalFire
    public boolean isSignalFire()
    {
        return signalFire;
    }

    public void setSignalFire(boolean set)
    {
        signalFire = set;
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

    public ItemStack getStackInSlot(int i)
    {
        return inventory[i];
    }

    public int getSizeInventory()
    {
        return inventory.length;
    }

    public ItemStack[] getInventory()
    {
        return inventory;
    }

}
