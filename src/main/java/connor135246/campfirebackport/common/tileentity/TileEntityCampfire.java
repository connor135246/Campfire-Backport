package connor135246.campfirebackport.common.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.CampfireBackportFakePlayer;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
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
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityCampfire extends TileEntity implements ISidedInventory
{
    // final variables
    protected static final Random RAND = new Random();
    protected static final int[] slotsSides = new int[] { 0, 1, 2, 3 };
    protected static final int[] slotsEnds = new int[] {};

    // NBT keys
    public static final String KEY_BlockEntityTag = "BlockEntityTag",
            KEY_Items = "Items",
            KEY_Slot = "Slot",
            KEY_CookingTimes = "CookingTimes",
            KEY_CookingTotalTimes = "CookingTotalTimes",
            KEY_SignalFire = "SignalFire",
            KEY_CustomName = "CustomName",
            KEY_RegenWaitTimer = "RegenWaitTimer",
            KEY_Life = "Life",
            KEY_StartingLife = "StartingLife",
            KEY_BaseBurnOutTimer = "BaseBurnOutTimer",
            KEY_PreviousTimestamp = "PreviousTimestamp";

    // main variables
    protected ItemStack[] inventory = new ItemStack[4];
    protected int[] cookingTimes = new int[] { 0, 0, 0, 0 };
    protected int[] cookingTotalTimes = new int[] { 600, 600, 600, 600 };
    protected boolean signalFire = false;
    protected String customName;
    protected int regenWaitTimer = -1;
    protected int life = -1;
    protected int startingLife = -1;
    protected int baseBurnOutTimer = -2;

    // variables that don't need to be saved to NBT
    protected boolean rainAndSky = false;
    protected boolean firstTick = true;

    // only used client side
    protected int animTimer = RAND.nextInt(32);

    @Override
    public void updateEntity()
    {
        if (isLit() && getWorldObj().getTotalWorldTime() % 100L == 0L && (getWorldObj().isRemote || RAND.nextInt(6) == 0))
        {
            rainAndSky = getWorldObj().isRaining() && getWorldObj().getPrecipitationHeight(xCoord, zCoord) <= yCoord + 1
                    && getWorldObj().getBiomeGenForCoords(xCoord, zCoord).canSpawnLightningBolt();
        }

        if (!getWorldObj().isRemote)
        {
            if (isLit())
            {
                cook();
                heal();
                burnOutFromRain();
                burnOutOverTime();

                if (!firstTick && getWorldObj().getTotalWorldTime() % 20L == 0L)
                    burnOutDueToLackOfOxygen();
            }
        }
        else
        {
            if (isLit())
            {
                addParticles();

                // fixes the animation looking a little weird right when placed
                if (!firstTick)
                    incrementAnimTimer();
            }
        }

        if (firstTick)
            firstTick = false;
    }

    /**
     * Cooks inventory items...
     */
    protected void cook()
    {
        int invCount = 0;

        for (int slot = 0; slot < getSizeInventory(); ++slot)
            if (getStackInSlot(slot) != null)
            {
                incrementCookingTimeInSlot(slot);
                ++invCount;
            }

        if (invCount > 0)
        {
            markDirty();

            // for updating the cooking waila display
            if (getWorldObj().getTotalWorldTime() % 50L == 0)
                markForClient();

            // if a multi-input recipe is found, only check for it once.
            List<CampfireRecipe> skips = new ArrayList<CampfireRecipe>(4);

            for (int slot = 0; slot < getSizeInventory(); ++slot)
            {
                ItemStack stack = getStackInSlot(slot);
                if (stack != null && getCookingTimeInSlot(slot) >= getCookingTotalTimeInSlot(slot))
                {
                    CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack, getType(), isSignalFire(), skips, invCount);
                    if (crecipe != null)
                    {
                        if (crecipe.isMultiInput())
                        {
                            skips.add(crecipe);
                            doMultiInputCooking(crecipe);
                        }
                        else
                        {
                            int[] slotToCinput = new int[] { -1, -1, -1, -1 };
                            slotToCinput[slot] = 0;
                            useInputs(crecipe, slotToCinput);
                            drop(crecipe);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds matching stacks for multi-input recipes.
     */
    protected void doMultiInputCooking(CampfireRecipe crecipe)
    {
        int target = crecipe.getInputs().length;
        int[] cinputToSlot = new int[] { -1, -1, -1, -1 };
        int[] slotToCinput = new int[] { -1, -1, -1, -1 };
        int matchCount = 0;

        invLoop: for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            ItemStack stack = getStackInSlot(slot);
            if (stack != null)
            {
                for (int cinputIndex = 0; cinputIndex < target; ++cinputIndex)
                {
                    if (cinputToSlot[cinputIndex] != -1)
                        continue;
                    else if (crecipe.getInputs()[cinputIndex].matches(stack) && getCookingTimeInSlot(slot) >= crecipe.getCookingTime())
                    {
                        cinputToSlot[cinputIndex] = slot;
                        slotToCinput[slot] = cinputIndex;
                        matchCount++;

                        if (matchCount == target)
                        {
                            useInputs(crecipe, slotToCinput);
                            drop(crecipe);
                            return;
                        }

                        continue invLoop;
                    }
                }
            }
        }
    }

    /**
     * Calls {@link CampfireRecipe#onUsingInput(int, ItemStack, EntityPlayer)} on each slot for each input with the fake player. <br>
     * Then drops any items that were added to the fake player.
     * 
     * @param crecipe
     * @param slotToCinput
     *            - an array of length 4, of inventory slot indexes to CustomInput indexes (or -1).
     */
    protected void useInputs(CampfireRecipe crecipe, int[] slotToCinput)
    {
        if (getWorldObj() instanceof WorldServer)
        {
            FakePlayer fakePlayer = CampfireBackportFakePlayer.getFakePlayer((WorldServer) getWorldObj());

            for (int slot = 0; slot < getSizeInventory(); ++slot)
                if (slotToCinput[slot] != -1)
                {
                    ItemStack stack = getStackInSlot(slot);
                    ItemStack transformedStack = crecipe.onUsingInput(slotToCinput[slot], stack, fakePlayer);
                    if (transformedStack != null && transformedStack.stackSize <= 0)
                        transformedStack = null;
                    if (transformedStack != stack)
                        setInventorySlotContentsForced(slot, transformedStack);
                }

            for (int slot = 0; slot < fakePlayer.inventory.getSizeInventory(); ++slot)
                popStackedItem(fakePlayer.inventory.getStackInSlotOnClosing(slot), getWorldObj(), xCoord, yCoord, zCoord);
        }
    }

    /**
     * ...and drops them when done.
     */
    protected void drop(CampfireRecipe crecipe)
    {
        popStackedItem(ItemStack.copyItemStack(crecipe.getOutput()), getWorldObj(), xCoord, yCoord, zCoord);

        if (crecipe.hasByproduct() && RAND.nextDouble() < crecipe.getByproductChance())
            popStackedItem(ItemStack.copyItemStack(crecipe.getByproduct()), getWorldObj(), xCoord, yCoord, zCoord);
    }

    /**
     * Applies regeneration effects periodically.
     */
    protected void heal()
    {
        if (CampfireBackportConfig.regenCampfires.matches(getType()))
        {
            if (getRegenWaitTimer() < 0)
                resetRegenWaitTimer();

            if (getRegenWaitTimer() == 0)
            {
                int[] regenValues = EnumCampfireType.option(getType(), CampfireBackportConfig.regularRegen, CampfireBackportConfig.soulRegen);

                List<EntityPlayer> playerlist = getWorldObj().getEntitiesWithinAABB(EntityPlayer.class,
                        AxisAlignedBB.getBoundingBox(xCoord - regenValues[2], yCoord - regenValues[2], zCoord - regenValues[2],
                                xCoord + regenValues[2], yCoord + regenValues[2], zCoord + regenValues[2]));

                for (EntityPlayer player : playerlist)
                {
                    if (!player.isPotionActive(Potion.regeneration))
                        player.addPotionEffect(new PotionEffect(Potion.regeneration.id, regenValues[1], regenValues[0], true));
                }
            }

            decrementRegenWaitTimer();

            markDirty();
        }
    }

    /**
     * Burns out the campfire if it's being rained on.
     */
    protected void burnOutFromRain()
    {
        if (rainAndSky && CampfireBackportConfig.putOutByRain.matches(this) && canBurnOut())
            burnOutOrToNothing();

        rainAndSky = false;
    }

    /**
     * Burns out the campfire once its life timer runs out.
     */
    protected void burnOutOverTime()
    {
        if (getBaseBurnOutTimer() != -1 && canBurnOut())
        {
            if (getLife() < 0)
                resetLife();

            if (getLife() == 0)
                burnOutOrToNothing();

            decrementLife();

            markDirty();
        }
    }

    /**
     * Burns out the campfire if it has no oxygen. Galacticraft / Advanced Rocketry compatibility.<br>
     * It's called from {@link BlockCampfire#onBlockPlacedBy}, as well as the normal campfire update tick.
     */
    public void burnOutDueToLackOfOxygen()
    {
        if (isLit() && !CampfireBackportCompat.hasOxygen(getWorldObj(), getBlockType(), xCoord, yCoord, zCoord))
            burnOutOrToNothing();
    }

    /**
     * Either burns out the campfire or burns it to nothing.
     */
    public void burnOutOrToNothing()
    {
        if (RAND.nextDouble() < CampfireBackportConfig.burnToNothingChances[getTypeIndex()])
        {
            popStackedItem(ItemStack.copyItemStack(CampfireBackportConfig.campfireDropsStacks[getTypeIndex()]), getWorldObj(), xCoord, yCoord, zCoord);

            playFizzAndAddSmokeServerSide(65, 0.25);

            getWorldObj().setBlock(xCoord, yCoord, zCoord, Blocks.air);
        }
        else
            BlockCampfire.updateCampfireBlockState(false, null, getWorldObj(), xCoord, yCoord, zCoord);
    }

    /**
     * Generates a lot of vanilla smoke particles above the campfire and plays a fizzing sound.<br>
     * It's server side particle spawning for when a campfire is extinguished.
     * 
     * @param particles
     *            - approx number of particles to spawn
     * @param height
     *            - additional height to add to particle spawn location, from the bottom of the block
     */
    public static void playFizzAndAddSmokeServerSide(World world, double x, double y, double z, int particles, double height)
    {
        if (!world.isRemote)
        {
            world.playSoundEffect(x, y, z, "random.fizz", 0.5F, RAND.nextFloat() * 0.4F + 0.8F);

            ((WorldServer) world).func_147487_a("smoke", x, y + height, z,
                    MathHelper.getRandomIntegerInRange(RAND, particles - 5, particles + 5), 0.3, 0.1, 0.3, 0.005);
        }
    }

    /**
     * Helper function that redirects to {@link #playFizzAndAddSmokeServerSide(World, double, double, double, int, double)} for this tile entity.
     */
    public void playFizzAndAddSmokeServerSide(int particles, double height)
    {
        playFizzAndAddSmokeServerSide(getWorldObj(), xCoord + 0.5, yCoord, zCoord + 0.5, particles, height);
    }

    /**
     * Checks if the block below is a signal fire block, and updates the {@link #signalFire} state.
     */
    public void checkSignal()
    {
        if (!getWorldObj().isRemote)
        {
            Block block = getWorldObj().getBlock(xCoord, yCoord - 1, zCoord);

            if (block.getMaterial() == Material.air)
            {
                updateSignalFireState(false);
                return;
            }

            int meta = getWorldObj().getBlockMetadata(xCoord, yCoord - 1, zCoord);

            if (CampfireBackportConfig.signalFireBlocks.get(block) != null)
            {
                Integer accepted = CampfireBackportConfig.signalFireBlocks.get(block);
                if (accepted == OreDictionary.WILDCARD_VALUE || accepted == meta)
                {
                    updateSignalFireState(true);
                    return;
                }
            }
            else
            {
                for (int id : OreDictionary.getOreIDs(new ItemStack(block, 1, meta)))
                {
                    if (CampfireBackportConfig.signalFireOres.contains(id))
                    {
                        updateSignalFireState(true);
                        return;
                    }
                }
            }

            updateSignalFireState(false);
        }
    }

    /**
     * Helper function that redirects to {@link #checkSignal()} for the campfire at the world and block given.
     */
    public static void checkSignal(World world, int x, int y, int z)
    {
        if (!world.isRemote)
        {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityCampfire)
                ((TileEntityCampfire) tile).checkSignal();
        }
    }

    /**
     * Updates the signal fire state and marks the tile entity as changed.
     */
    protected void updateSignalFireState(boolean newState)
    {
        if (isSignalFire() != newState)
        {
            setSignalFire(newState);
            markDirty();
            markForClient();
        }
    }

    /**
     * @return whether or not the campfire is allowed to burn out. in other words it's not a signal fire, or it is but the config option is set. note that it doesn't indicate
     *         whether the campfire is currently lit or not.
     */
    public boolean canBurnOut()
    {
        return !isSignalFire() || CampfireBackportConfig.signalFiresBurnOut.matches(this);
    }

    /**
     * Generates big smoke particles above the campfire, vanilla smoke particles above the items in the campfire, and some vanilla smoke particles above the middle if it's raining.
     */
    protected void addParticles()
    {
        CampfireBackport.proxy.generateBigSmokeParticles(getWorldObj(), xCoord, yCoord, zCoord, getType(), isSignalFire());

        CampfireBackport.proxy.generateSmokeOverItems(getWorldObj(), xCoord, yCoord, zCoord, getBlockMetadata(), inventory);

        if (rainAndSky)
        {
            for (int i = 0; i < RAND.nextInt(3); ++i)
                getWorldObj().spawnParticle("smoke", xCoord + RAND.nextDouble(), yCoord + 0.9, zCoord + RAND.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    // Data

    @Override
    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z)
    {
        return !(newBlock instanceof BlockCampfire);
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

        signalFire = compound.getBoolean(KEY_SignalFire);

        readFromNBTIfItExists(compound);
    }

    /**
     * Reads tags from NBT, but if the tags aren't there, leaves values as default ones.<br>
     * This makes sure things that should be -1 stay as -1 instead of being set to 0, for example.
     */
    public void readFromNBTIfItExists(NBTTagCompound compound)
    {
        int[] cookingTimesArray = compound.hasKey(KEY_CookingTimes, 11) ? compound.getIntArray(KEY_CookingTimes) : cookingTimes;
        boolean hasCookingTotalTimes = compound.hasKey(KEY_CookingTotalTimes, 11);
        int[] cookingTotalTimesArray = hasCookingTotalTimes ? compound.getIntArray(KEY_CookingTotalTimes) : cookingTotalTimes;

        regenWaitTimer = compound.hasKey(KEY_RegenWaitTimer, 99) ? compound.getInteger(KEY_RegenWaitTimer) : regenWaitTimer;

        life = compound.hasKey(KEY_Life, 99) ? compound.getInteger(KEY_Life) : life;
        startingLife = compound.hasKey(KEY_StartingLife, 99) ? compound.getInteger(KEY_StartingLife) : life;
        baseBurnOutTimer = compound.hasKey(KEY_BaseBurnOutTimer, 99) ? compound.getInteger(KEY_BaseBurnOutTimer) : baseBurnOutTimer;

        if (compound.hasKey(KEY_Items, 9))
        {
            NBTTagList itemList = compound.getTagList(KEY_Items, 10);
            inventory = new ItemStack[4];

            for (int i = 0; i < itemList.tagCount(); ++i)
            {
                NBTTagCompound itemCompound = itemList.getCompoundTagAt(i);
                byte slot = itemCompound.getByte(KEY_Slot);
                if (slot >= 0 && slot < getSizeInventory())
                {
                    ItemStack stack = ItemStack.loadItemStackFromNBT(itemCompound);
                    setStackInSlot(slot, stack);
                    setCookingTimeInSlot(slot, cookingTimesArray[slot]);
                    if (hasCookingTotalTimes)
                        setCookingTotalTimeInSlot(slot, cookingTotalTimesArray[slot]);
                    else
                    {
                        CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack, getType(), signalFire);
                        setCookingTotalTimeInSlot(slot, crecipe == null ? 600 : crecipe.getCookingTime());
                    }
                }
            }
        }

        customName = compound.hasKey(KEY_CustomName, 8) ? compound.getString(KEY_CustomName) : customName;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        compound.setBoolean(KEY_SignalFire, isSignalFire());

        compound.setIntArray(KEY_CookingTimes, cookingTimes);
        compound.setIntArray(KEY_CookingTotalTimes, cookingTotalTimes);

        compound.setInteger(KEY_RegenWaitTimer, getRegenWaitTimer());

        compound.setInteger(KEY_Life, getLife());
        compound.setInteger(KEY_StartingLife, getStartingLife());
        compound.setInteger(KEY_BaseBurnOutTimer, getBaseBurnOutTimer());

        NBTTagList itemList = new NBTTagList();
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            if (inventory[slot] != null)
            {
                NBTTagCompound itemCompound = new NBTTagCompound();
                itemCompound.setByte(KEY_Slot, (byte) slot);
                inventory[slot].writeToNBT(itemCompound);
                itemList.appendTag(itemCompound);
            }
        }
        compound.setTag(KEY_Items, itemList);

        if (hasCustomInventoryName())
            compound.setString(KEY_CustomName, getInventoryName());
    }

    /**
     * Marks that this tile entity's nbt has changed.
     */
    @Override
    public void markDirty()
    {
        if (hasWorldObj())
            getWorldObj().markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
    }

    /**
     * Sends the {@link #getDescriptionPacket()} if on the server. Then rerenders the tile on the client.
     */
    public void markForClient()
    {
        if (hasWorldObj())
            getWorldObj().markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /**
     * Notifies neighbours of the tile entity changing. In vanilla, all this is used for is the comparator.
     */
    public void markForNeighbours()
    {
        if (hasWorldObj())
            getWorldObj().func_147453_f(xCoord, yCoord, zCoord, getBlockType());
    }

    // Getters and Setters

    // block properties

    @Override
    public Block getBlockType()
    {
        if (hasWorldObj())
            return super.getBlockType();
        else
            return blockType;
    }

    @Override
    public int getBlockMetadata()
    {
        if (hasWorldObj())
            return super.getBlockMetadata();
        else
            return blockMetadata;
    }

    /**
     * Verifies that the block at the location of this tile entity is a BlockCampfire. If it isn't, invalidates this tile entity.
     * 
     * @return the BlockCampfire at this location, or the regular campfire_base if there isn't one
     */
    public BlockCampfire getBlockTypeAsCampfire()
    {
        if (getBlockType() instanceof BlockCampfire)
            return (BlockCampfire) getBlockType();
        else if (hasWorldObj()) // && ForgeModContainer.removeErroringTileEntities
        {
            CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".error.invalid_tile",
                    getWorldObj().provider.dimensionId, xCoord, yCoord, zCoord));
            invalidate();
        }
        return (BlockCampfire) CampfireBackportBlocks.campfire_base;
    }

    /**
     * Returns the lit state of the cached campfire.<br>
     * If it hasn't been cached yet, gets it from the world, but if this tile entity doesn't have a campfire, invalidates it.
     */
    public boolean isLit()
    {
        return getBlockTypeAsCampfire().isLit();
    }

    /**
     * Returns the type of the cached campfire.<br>
     * If it hasn't been cached yet, gets it from the world, but if this tile entity doesn't have a campfire, invalidates it.
     */
    public String getType()
    {
        return getBlockTypeAsCampfire().getType();
    }

    public int getTypeIndex()
    {
        return EnumCampfireType.index(getType());
    }

    // inventory

    @Override
    public ItemStack decrStackSize(int slot, int decrease)
    {
        ItemStack stack = getStackInSlot(slot);

        if (stack != null)
        {
            if (stack.stackSize <= decrease)
            {
                setInventorySlotContents(slot, null);
                return stack;
            }
            else
            {
                ItemStack returnStack = stack.splitStack(decrease);

                if (stack.stackSize <= 0)
                    setInventorySlotContents(slot, null);
                else
                {
                    markDirty();
                    markForClient();
                    markForNeighbours();
                }

                return returnStack;
            }
        }
        else
            return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot)
    {
        ItemStack stack = getStackInSlot(slot);

        if (stack != null)
        {
            setInventorySlotContents(slot, null);
            return stack;
        }
        else
            return null;
    }

    /**
     * Puts the ItemStack into the campfire, updates {@link #cookingTimes} and {@link #cookingTotalTimes}, and marks the tile entity as changed.
     */
    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        ItemStack invStack = null;
        int cookingTotalTime = 600;

        if (stack != null)
        {
            invStack = stack.splitStack(getInventoryStackLimit());

            CampfireRecipe crecipe = CampfireRecipe.findRecipe(invStack, getType(), isSignalFire());
            if (crecipe != null)
                cookingTotalTime = crecipe.getCookingTime();
        }

        setStackInSlot(slot, invStack);
        setCookingTotalTimeInSlot(slot, cookingTotalTime);
        resetCookingTimeInSlot(slot);

        markDirty();
        markForClient();
        markForNeighbours();
    }

    /**
     * Unlike {@link #setInventorySlotContents}, the stack given is exactly the stack that is placed in the campfire; it doesn't split the stack to fit
     * {@link #getInventoryStackLimit}. <br>
     * Also, {@link #cookingTimes} is only reset if the stack is null. And {@link #cookingTotalTimes} is only updated if the stack is null or actually has a recipe. <br>
     * In other words, this method behaves exactly the same as {@link #setInventorySlotContents} if the stack is null.
     */
    public void setInventorySlotContentsForced(int slot, ItemStack stack)
    {
        if (stack != null)
        {
            CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack, getType(), isSignalFire());
            if (crecipe != null)
                setCookingTotalTimeInSlot(slot, crecipe.getCookingTime());
        }
        else
        {
            resetCookingTimeInSlot(slot);
            setCookingTotalTimeInSlot(slot, 600);
        }

        setStackInSlot(slot, stack);

        markDirty();
        markForClient();
        markForNeighbours();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return isItemValidForCampfire(stack);
    }

    /**
     * Returns true if anything is allowed to insert the given stack (ignoring stack size) into the campfire, in general.
     */
    public boolean isItemValidForCampfire(ItemStack stack)
    {
        return CampfireRecipe.findRecipe(stack, getType(), isSignalFire()) != null;
    }

    /**
     * Tries to add the ItemStack to the campfire's inventory. Called by {@link BlockCampfire#onBlockActivated}.
     * 
     * @param stack
     *            - the stack (in the player's hand)
     * @return true if the item was added to the campfire's inventory, false otherwise
     */
    public boolean tryInventoryAdd(ItemStack stack)
    {
        if (isItemValidForCampfire(stack))
        {
            for (int slot = 0; slot < getSizeInventory(); ++slot)
                if (getStackInSlot(slot) == null)
                {
                    setInventorySlotContents(slot, stack);
                    return true;
                }
        }
        return false;
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

    public void setCustomInventoryName(String name)
    {
        customName = name;
        markDirty();
        markForClient();
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
        if (CampfireBackportConfig.automation.matches(this))
            return side == 1 || side == 0 ? slotsEnds : slotsSides;
        else
            return slotsEnds;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        if (CampfireBackportConfig.automation.matches(this))
            return side == 1 || side == 0 ? false : isItemValidForSlot(slot, stack);
        else
            return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side)
    {
        return false;
    }

    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (isSlotNumber(slot))
            inventory[slot] = stack;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return isSlotNumber(slot) ? inventory[slot] : null;
    }

    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    /**
     * Verifies that the slot number is a valid slot in the campfire's inventory, to avoid an index out of bounds error.
     */
    public boolean isSlotNumber(int slot)
    {
        return 0 <= slot && slot < getSizeInventory();
    }

    /**
     * Drops all the campfire's items into the world.
     */
    public void popItems()
    {
        for (int slot = 0; slot < getSizeInventory(); ++slot)
            popItem(getStackInSlotOnClosing(slot), getWorldObj(), xCoord, yCoord, zCoord);
    }

    /**
     * Drops the given ItemStack, if it's not null or empty. Splits the stack into multiple stacks if the given stack has an illegal stack size.
     */
    public static void popStackedItem(ItemStack stack, World world, int x, int y, int z)
    {
        if (stack != null && stack.stackSize > 0)
        {
            int max = stack.getMaxStackSize();

            while (stack.stackSize > 0)
            {
                if (stack.stackSize > max)
                    popItem(stack.splitStack(max), world, x, y, z);
                else
                {
                    popItem(stack, world, x, y, z);
                    break;
                }
            }
        }
    }

    /**
     * Drops the given ItemStack, if it's not null or empty.
     */
    public static void popItem(ItemStack stack, World world, int x, int y, int z)
    {
        if (stack != null && stack.stackSize > 0)
        {
            EntityItem entityitem = new EntityItem(world, x + RAND.nextDouble() * 0.75 + 0.125, y + RAND.nextDouble() * 0.75 + 0.5,
                    z + RAND.nextDouble() * 0.75 + 0.125, stack);

            entityitem.motionX = RAND.nextGaussian() * 0.05;
            entityitem.motionY = RAND.nextGaussian() * 0.05 + 0.2;
            entityitem.motionZ = RAND.nextGaussian() * 0.05;

            world.spawnEntityInWorld(entityitem);
        }
    }

    // cookingTimes

    public int getCookingTimeInSlot(int slot)
    {
        return isSlotNumber(slot) ? cookingTimes[slot] : 0;
    }

    public void setCookingTimeInSlot(int slot, int time)
    {
        if (isSlotNumber(slot))
            cookingTimes[slot] = time;
    }

    public void resetCookingTimeInSlot(int slot)
    {
        setCookingTimeInSlot(slot, 0);
    }

    public void incrementCookingTimeInSlot(int slot)
    {
        if (isSlotNumber(slot) && cookingTimes[slot] < Integer.MAX_VALUE)
            ++cookingTimes[slot];
    }

    // cookingTotalTimes

    public int getCookingTotalTimeInSlot(int slot)
    {
        return isSlotNumber(slot) ? cookingTotalTimes[slot] : 600;
    }

    public void setCookingTotalTimeInSlot(int slot, int time)
    {
        if (isSlotNumber(slot))
            cookingTotalTimes[slot] = time;
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
        if (animTimer > 31)
            animTimer = 0;
    }

    // burning out

    public int getLife()
    {
        return life;
    }

    public void decrementLife()
    {
        if (life > -1)
            --life;
    }

    public void resetLife()
    {
        resetBaseBurnOutTimer();
        life = startingLife = natureRange(getBaseBurnOutTimer());
    }

    public int getStartingLife()
    {
        return startingLife;
    }

    public int getBaseBurnOutTimer()
    {
        if (baseBurnOutTimer == -2)
            resetBaseBurnOutTimer();

        return baseBurnOutTimer;
    }

    public void resetBaseBurnOutTimer()
    {
        baseBurnOutTimer = BurnOutRule.findBurnOutRule(getWorldObj(), xCoord, yCoord, zCoord, getType()).getTimer();
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

    // heal

    public int getRegenWaitTimer()
    {
        return regenWaitTimer;
    }

    public void decrementRegenWaitTimer()
    {
        if (regenWaitTimer > -1)
            --regenWaitTimer;
    }

    public void resetRegenWaitTimer()
    {
        regenWaitTimer = natureRange(EnumCampfireType.option(getType(), CampfireBackportConfig.regularRegen[3], CampfireBackportConfig.soulRegen[3]));
    }

    // etc

    /**
     * @return base +/- 10%
     */
    public static int natureRange(int base)
    {
        return Math.round(base * (0.9F + RAND.nextFloat() * 0.2F));
    }

}
