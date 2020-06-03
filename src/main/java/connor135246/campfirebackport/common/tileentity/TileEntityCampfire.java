package connor135246.campfirebackport.common.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHay;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;

public class TileEntityCampfire extends TileEntity implements ISidedInventory
{
    private static final Random RAND = new Random();

    // main variables
    private static final int[] slotsSides = new int[] { 0, 1, 2, 3 };
    private static final int[] slotsEnds = new int[] {};
    private ItemStack[] inventory = new ItemStack[4];
    private int[] cookingTimes = new int[4];
    private int[] cookingTotalTimes = new int[4];
    private boolean signalFire = false;
    private String customName;
    private int regenWaitTimer = -1;

    // rendering variables
    private int animTimer = 0;

    public void updateEntity()
    {
        Block block = worldObj.getBlock(xCoord, yCoord, zCoord);

        if (!worldObj.isRemote)
        {
            if (block == CampfireBackportBlocks.campfire)
            {
                cookAndDrop();
                if (CampfireBackportConfig.regenCampfires)
                {
                    heal();
                }
            }
            else
            {
                int totaltime;
                for (int i = 0; i < getSizeInventory(); ++i)
                {
                    totaltime = getCookingTimeInSlot(i);
                    if (totaltime > 0)
                    {
                        setCookingTimeInSlot(i, MathHelper.clamp_int(totaltime - 2, 0, totaltime));
                    }
                }
            }
        }
        else
        {
            if (block == CampfireBackportBlocks.campfire)
                addParticles();

            ++animTimer;
            if (animTimer == 800)
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
            List<EntityPlayer> playerlist = getWorldObj().getEntitiesWithinAABB(EntityPlayer.class,
                    AxisAlignedBB.getBoundingBox(xCoord - CampfireBackportConfig.regenRadius, yCoord - CampfireBackportConfig.regenRadius,
                            zCoord - CampfireBackportConfig.regenRadius,
                            xCoord + CampfireBackportConfig.regenRadius, yCoord + CampfireBackportConfig.regenRadius,
                            zCoord + CampfireBackportConfig.regenRadius));

            for (EntityPlayer player : playerlist)
            {
                if (!player.isPotionActive(Potion.regeneration))
                    player.addPotionEffect(new PotionEffect(Potion.regeneration.id, CampfireBackportConfig.regenTime, CampfireBackportConfig.regenLevel, true));
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
                    CampfireRecipe crecipe = CampfireRecipe.findRecipe(itemstack);

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
        ItemStack stack;
        World world = getWorldObj();
        int setting = (Minecraft.getMinecraft().gameSettings.particleSetting + 1);
        int multiplier = setting % 2 == 1 ? setting ^ 2 : setting;

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

        if (world.isRaining())
        {
            for (int i = 0; i < RAND.nextInt(multiplier); ++i)
                world.spawnParticle("smoke", (double) xCoord + RAND.nextDouble(), (double) yCoord + 0.9, (double) zCoord + RAND.nextDouble(), 0.0D, 0.0D,
                        0.0D);
        }
    }

    public boolean hasTileEntity(int meta)
    {
        return true;
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

        setSignalFire(compound.getBoolean("SignalFire"));
        NBTTagList nbttaglist = compound.getTagList("Items", 10);
        int[] nbtintarray1 = compound.getIntArray("CookingTimes");
        int[] nbtintarray2 = compound.getIntArray("CookingTotalTimes");
        this.regenWaitTimer = compound.getInteger("RegenWaitTimer");
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
        return CampfireRecipe.findRecipe(stack) != null && ((BlockCampfire) this.worldObj.getBlock(xCoord, yCoord, zCoord)).isLit();
    }

    /**
     * Tries to add the ItemStack to the campfire's inventory. Called by BlockCampfire when right clicked.
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
     * Finds the next slot that's able to accept the stack, or -1 if none. Called by the above method.
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
        if (CampfireBackportConfig.allowAutomation)
            return side == 1 ? slotsEnds : (side == 0 ? slotsEnds : slotsSides);
        else
            return slotsEnds;
    }

    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        if (CampfireBackportConfig.allowAutomation)
            return side == 1 ? false : (side == 0 ? false : isItemValidForSlot(slot, stack));
        else
            return false;
    }

    public boolean canExtractItem(int slot, ItemStack stack, int side)
    {
        return false;
    }

    // Getters and Setters

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
        CampfireRecipe crecipe = CampfireRecipe.findRecipe(stack);

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
        regenWaitTimer = Math.round(CampfireBackportConfig.regenWait * (0.9F + RAND.nextFloat() * 0.2F));
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
