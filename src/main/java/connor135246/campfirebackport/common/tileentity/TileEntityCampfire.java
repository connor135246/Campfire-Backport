package connor135246.campfirebackport.common.tileentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
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
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityCampfire extends TileEntity implements ISidedInventory
{
    // final variables
    private static final Random RAND = new Random();
    private static final int[] slotsSides = new int[] { 0, 1, 2, 3 };
    private static final int[] slotsEnds = new int[] {};

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
    private ItemStack[] inventory = new ItemStack[4];
    private int[] cookingTimes = new int[] { 0, 0, 0, 0 };
    private int[] cookingTotalTimes = new int[] { 600, 600, 600, 600 };
    private boolean signalFire = false;
    private String customName;
    private int regenWaitTimer = -1;
    private int life = -1;
    private int startingLife = -1;
    private int baseBurnOutTimer = -2;

    // variables that don't need to be saved to NBT
    private boolean rainAndSky = false;

    // only used client side
    private boolean firstTick = true;
    private int animTimer = RAND.nextInt(32);

    @Override
    public void updateEntity()
    {
        if (getWorldObj().getTotalWorldTime() % 100L == 0L && (RAND.nextInt(6) == 0 || getWorldObj().isRemote))
        {
            rainAndSky = getWorldObj().isRaining() && getWorldObj().canBlockSeeTheSky(xCoord, yCoord, zCoord)
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
            }
        }
        else
        {
            if (isLit())
                addParticles();

            // fixes the animation looking a little weird right when placed
            if (!firstTick)
                incrementAnimTimer();

            if (firstTick)
                firstTick = false;
        }
    }

    /**
     * Cooks inventory items...
     */
    private void cook()
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
            // for updating the cooking waila display
            if (getWorldObj().getTotalWorldTime() % 50L == 0)
                markDirty();

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
                            drop(crecipe, Arrays.asList(slot));
                    }
                }
            }
        }
    }

    /**
     * Finds matching stacks for multi-input recipes.
     */
    private void doMultiInputCooking(CampfireRecipe crecipe)
    {
        int target = crecipe.getInputs().length;
        List<Integer> found = new ArrayList<Integer>(target);
        boolean[] matchedCinputs = new boolean[target];

        invLoop: for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            ItemStack stack = getStackInSlot(slot);
            if (stack != null)
            {
                for (int cinputIndex = 0; cinputIndex < target; ++cinputIndex)
                {
                    if (matchedCinputs[cinputIndex])
                        continue;

                    if (crecipe.getInputs()[cinputIndex].matches(stack) && getCookingTimeInSlot(slot) >= crecipe.getCookingTime())
                    {
                        matchedCinputs[cinputIndex] = true;
                        found.add(slot);

                        if (found.size() == target)
                        {
                            drop(crecipe, found);
                            return;
                        }

                        continue invLoop;
                    }
                }
            }
        }
    }

    /**
     * ...and drops them when done.
     */
    private void drop(CampfireRecipe crecipe, List<Integer> slotsToEmpty)
    {
        popStackedItem(crecipe.getOutput(), getWorldObj(), xCoord, yCoord, zCoord);

        if (crecipe.hasByproduct() && RAND.nextDouble() < crecipe.getByproductChance())
            popStackedItem(crecipe.getByproduct(), getWorldObj(), xCoord, yCoord, zCoord);

        for (int slot : slotsToEmpty)
            setInventorySlotContents(slot, null);
    }

    /**
     * Applies regeneration effects periodically.
     */
    private void heal()
    {
        if (CampfireBackportConfig.regenCampfires.matches(getType()))
        {
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

            if (getRegenWaitTimer() <= 0)
                resetRegenWaitTimer();

            decrementRegenWaitTimer();
        }
    }

    /**
     * Burns out the campfire if it's being rained on.
     */
    private void burnOutFromRain()
    {
        if (rainAndSky && CampfireBackportConfig.putOutByRain.matches(this) && (!isSignalFire() || CampfireBackportConfig.signalFiresBurnOut.matches(this)))
            burnOutOrToNothing();

        rainAndSky = false;
    }

    /**
     * Burns out the campfire once its life timer runs out.
     */
    private void burnOutOverTime()
    {
        if (getBaseBurnOutTimer() != -1 && (!isSignalFire() || CampfireBackportConfig.signalFiresBurnOut.matches(this)))
        {
            if (getLife() == 0)
                burnOutOrToNothing();

            if (getLife() <= 0)
                resetLife();

            decrementLife();
        }
    }

    /**
     * Either burns out the campfire or burns it to nothing.
     */
    private void burnOutOrToNothing()
    {
        if (RAND.nextDouble() < CampfireBackportConfig.burnToNothingChances[getTypeToInt()])
        {
            popStackedItem(ItemStack.copyItemStack(CampfireBackportConfig.campfireDropsStacks[getTypeToInt()]), getWorldObj(), xCoord, yCoord, zCoord);

            playFizzAndAddSmokeServerSide(65, 0.25);

            getWorldObj().setBlock(xCoord, yCoord, zCoord, Blocks.air);
        }
        else
            BlockCampfire.updateCampfireBlockState(false, getType(), getWorldObj(), xCoord, yCoord, zCoord);
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
                    MathHelper.getRandomIntegerInRange(RAND, particles - 5, particles + 5), 0.3D, 0.1D, 0.3D, 0.005D);
        }
    }

    /**
     * Helper function that redirects to {@link #playFizzAndAddSmokeServerSide(World, double, double, double, int, double)} for this tile entity
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

            if (block == Blocks.air)
            {
                updateSignalFireState(false);
                return;
            }

            int meta = getWorldObj().getBlockMetadata(xCoord, yCoord - 1, zCoord);

            if (CampfireBackportConfig.signalFireBlocks.get(block) != null)
            {
                if (CampfireBackportConfig.signalFireBlocks.get(block) == OreDictionary.WILDCARD_VALUE
                        || CampfireBackportConfig.signalFireBlocks.get(block) == meta)
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
    public void updateSignalFireState(boolean newState)
    {
        if (isSignalFire() != newState)
        {
            setSignalFire(newState);
            markDirty();
        }
    }

    /**
     * Generates big smoke particles above the campfire, vanilla smoke particles above the items in the campfire, and some vanilla smoke particles above the middle if it's raining.
     */
    private void addParticles()
    {
        int setting = Minecraft.getMinecraft().gameSettings.particleSetting + 1;
        int multiplier = setting % 2 == 1 ? setting ^ 2 : setting;

        int[] iro = RenderCampfire.getRenderSlotMappingFromMeta(getBlockMetadata());
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            if (getStackInSlot(slot) != null)
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
            int meta = 0;

            if (CampfireBackportConfig.colourfulSmoke.matches(this))
            {
                Block blockBelow = getWorldObj().getBlock(xCoord, yCoord - 1, zCoord);

                if (blockBelow != Blocks.air && getWorldObj().isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))
                {
                    block = blockBelow;
                    meta = getWorldObj().getBlockMetadata(xCoord, yCoord - 1, zCoord);
                }
            }

            for (int i = 0; i < RAND.nextInt(2) + 2; ++i)
                CampfireBackport.proxy.generateBigSmokeParticles(getWorldObj(), xCoord, yCoord, zCoord, isSignalFire(), block, meta);
        }

        if (rainAndSky)
        {
            for (int i = 0; i < RAND.nextInt(multiplier); ++i)
                getWorldObj().spawnParticle("smoke", xCoord + RAND.nextDouble(), yCoord + 0.9, zCoord + RAND.nextDouble(), 0.0D, 0.0D, 0.0D);
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

        readFromNBTIfItExists(compound, true);
    }

    /**
     * Reads tags from NBT, but if the tags aren't there, leaves values as default ones.<br>
     * This makes sure things that should be -1 stay as -1 instead of being set to 0, for example.
     * 
     * @param compound
     * @param resetStartingLife
     *            - if true and the compound doesn't have the {@link #KEY_StartingLife} key, {@link #startingLife} will be set to the same value as {@link #life}
     */
    public void readFromNBTIfItExists(NBTTagCompound compound, boolean resetStartingLife)
    {
        signalFire = compound.hasKey(KEY_SignalFire, 1) ? compound.getBoolean(KEY_SignalFire) : signalFire;

        int[] cookingTimesArray = compound.hasKey(KEY_CookingTimes, 11) ? compound.getIntArray(KEY_CookingTimes) : cookingTimes;
        int[] cookingTotalTimesArray = compound.hasKey(KEY_CookingTotalTimes, 11) ? compound.getIntArray(KEY_CookingTotalTimes) : cookingTotalTimes;

        regenWaitTimer = compound.hasKey(KEY_RegenWaitTimer, 99) ? compound.getInteger(KEY_RegenWaitTimer) : regenWaitTimer;

        life = compound.hasKey(KEY_Life, 99) ? compound.getInteger(KEY_Life) : life;
        startingLife = compound.hasKey(KEY_StartingLife, 99) ? compound.getInteger(KEY_StartingLife) : (resetStartingLife ? life : startingLife);
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
                    setStackInSlot(slot, ItemStack.loadItemStackFromNBT(itemCompound));
                    setCookingTimeInSlot(slot, cookingTimesArray[slot]);
                    setCookingTotalTimeInSlot(slot, cookingTotalTimesArray[slot]);
                }
            }
        }

        if (compound.hasKey(KEY_CustomName, 8))
            setCustomInventoryName(compound.getString(KEY_CustomName));
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

    @Override
    public void markDirty()
    {
        super.markDirty();

        if (hasWorldObj())
            getWorldObj().markBlockForUpdate(xCoord, yCoord, zCoord);
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
        else if (hasWorldObj())
        {
            CommonProxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".error.invalid_tile",
                    getWorldObj().provider.dimensionId, xCoord, yCoord, zCoord));
            invalidate();
        }
        return (BlockCampfire) CampfireBackportBlocks.campfire_base;
    }

    /**
     * Refreshes cached block and meta and marks the tile entity as changed.
     */
    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();

        markDirty();
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

    public int getTypeToInt()
    {
        return EnumCampfireType.toInt(getType());
    }

    // inventory

    @Override
    public ItemStack decrStackSize(int slot, int decrease)
    {
        ItemStack itemstack = getStackInSlot(slot);

        if (itemstack != null)
        {
            if (itemstack.stackSize <= decrease)
            {
                setInventorySlotContents(slot, null);
                return itemstack;
            }
            else
            {
                itemstack.splitStack(decrease);

                if (getStackInSlot(slot).stackSize == 0)
                    setInventorySlotContents(slot, null);

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
            setInventorySlotContents(slot, null);
            return itemstack;
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
            invStack = stack.splitStack(1);

            CampfireRecipe crecipe = CampfireRecipe.findRecipe(invStack, getType(), isSignalFire());
            if (crecipe != null)
                cookingTotalTime = crecipe.getCookingTime();
        }

        setStackInSlot(slot, invStack);
        setCookingTotalTimeInSlot(slot, cookingTotalTime);
        resetCookingTimeInSlot(slot);

        markDirty();
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
     * Tries to add the ItemStack to the campfire's inventory. Called by {@link BlockCampfire#onBlockActivated(World, int, int, int, EntityPlayer, int, float, float, float)
     * BlockCampfire#onBlockActivated}.
     * 
     * @param stack
     *            - the stack (in the player's hand)
     * @return true if the item was added to the campfire's inventory, false otherwise
     */
    public boolean tryInventoryAdd(ItemStack stack)
    {
        if (isItemValidForCampfire(stack))
        {
            int slot = Arrays.asList(getInventory()).indexOf(null);
            if (slot != -1)
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
            return side == 1 ? slotsEnds : (side == 0 ? slotsEnds : slotsSides);
        else
            return slotsEnds;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        if (CampfireBackportConfig.automation.matches(this))
            return side == 1 ? false : (side == 0 ? false : isItemValidForSlot(slot, stack));
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

    public boolean isSlotNumber(int slot)
    {
        return 0 <= slot && slot < getSizeInventory();
    }

    public ItemStack[] getInventory()
    {
        return inventory;
    }

    /**
     * Drops all the campfire's items into the world.
     */
    public void popItems()
    {
        ItemStack stack;
        for (int slot = 0; slot < getSizeInventory(); ++slot)
        {
            stack = getStackInSlotOnClosing(slot);
            if (stack != null)
                popItem(stack, getWorldObj(), xCoord, yCoord, zCoord);
        }
    }

    /**
     * Drops the given ItemStack, if it's not null. Splits the stack into multiple stacks if the given stack has an illegal stack size.
     */
    public static void popStackedItem(ItemStack stack, World world, int x, int y, int z)
    {
        if (stack != null)
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
     * Drops the given ItemStack, if it's not null.
     */
    public static void popItem(ItemStack stack, World world, int x, int y, int z)
    {
        if (stack != null)
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
        return (isSlotNumber(slot)) ? cookingTimes[slot] : 0;
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
        if (animTimer == Integer.MAX_VALUE)
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
