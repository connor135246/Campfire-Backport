package connor135246.campfirebackport.common.items;

import java.util.ArrayList;
import java.util.List;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class ItemBlockCampfire extends ItemBlockWithMetadata
{
    @SideOnly(Side.CLIENT)
    protected IIcon overlay;

    protected String type;
    protected boolean lit;

    public ItemBlockCampfire(Block block)
    {
        super(block, block);
        this.lit = ((BlockCampfire) block).isLit();
        this.type = ((BlockCampfire) block).getType();
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int invSlot, boolean beingHeld)
    {
        if (!world.isRemote && entity instanceof EntityPlayerMP && burnOutAsAnItem(stack, world, entity))
        {
            ((EntityPlayerMP) entity).inventory.setInventorySlotContents(invSlot, null);
            ((EntityPlayerMP) entity).inventoryContainer.putStackInSlot(invSlot, null);
            ((EntityPlayerMP) entity).mcServer.getConfigurationManager().syncPlayerInventory(((EntityPlayerMP) entity));
        }
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem)
    {
        if (!entityItem.worldObj.isRemote && burnOutAsAnItem(entityItem.getEntityItem(), entityItem.worldObj, entityItem))
        {
            entityItem.setDead();
            return true;
        }
        return false;
    }

    /**
     * Handles burning out of campfire items, if the campfire is lit and the config option is set for it.<br>
     * The life timer is decremented and timestamp is updated each tick. If the expected NBT isn't there, it gets added.<br>
     * Using a timestamp is required so that you can't just pause the timer as long as you like by putting it in a container.
     * 
     * @param stack
     * @param world
     * @param entity
     *            - if we're updating from the player's inventory, this is the player. if we're updating from an item on the ground, this is that item entity.
     * @return true if the item burned out, false if it hasn't burned out yet
     */
    protected boolean burnOutAsAnItem(ItemStack stack, World world, Entity entity)
    {
        if (isLit() && CampfireBackportConfig.burnOutAsItem.matches(this))
        {
            int baseBurnOut = BurnOutRule.findBurnOutRule(world, entity.posX, entity.posY, entity.posZ, getType()).getTimer();

            if (baseBurnOut != -1)
            {
                if (!stack.hasTagCompound())
                    stack.setTagCompound(new NBTTagCompound());

                if (!stack.getTagCompound().hasKey(TileEntityCampfire.KEY_BlockEntityTag, 10))
                    stack.getTagCompound().setTag(TileEntityCampfire.KEY_BlockEntityTag, new NBTTagCompound());

                NBTTagCompound tilenbt = stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag);

                final boolean hasLife = tilenbt.hasKey(TileEntityCampfire.KEY_Life, 99);
                final boolean hasStartingLife = tilenbt.hasKey(TileEntityCampfire.KEY_StartingLife, 99);

                int life;
                final long timestamp = world.getTotalWorldTime();

                if (hasLife || hasStartingLife)
                {
                    life = hasLife ? tilenbt.getInteger(TileEntityCampfire.KEY_Life) : tilenbt.getInteger(TileEntityCampfire.KEY_StartingLife);

                    if (!hasStartingLife)
                        tilenbt.setInteger(TileEntityCampfire.KEY_StartingLife, life);

                    long previousTimestamp = tilenbt.hasKey(TileEntityCampfire.KEY_PreviousTimestamp, 99)
                            ? tilenbt.getLong(TileEntityCampfire.KEY_PreviousTimestamp)
                            : timestamp;

                    life -= (int) (timestamp - previousTimestamp);
                }
                else
                {
                    life = TileEntityCampfire.natureRange(baseBurnOut);
                    tilenbt.setInteger(TileEntityCampfire.KEY_StartingLife, life);
                }

                if (life <= 0)
                {
                    onBurningOut(stack, world, entity);
                    return true;
                }
                else
                {
                    tilenbt.setInteger(TileEntityCampfire.KEY_Life, life);
                    tilenbt.setLong(TileEntityCampfire.KEY_PreviousTimestamp, timestamp);
                }
            }
        }
        return false;
    }

    /**
     * Drops the campfire's burn out drops and does burn out effects.
     */
    protected void onBurningOut(ItemStack stack, World world, Entity entity)
    {
        int particles = 0;

        boolean copyTags = false;
        boolean hasItems = false;
        List<ItemStack> invItems = new ArrayList<ItemStack>();

        NBTTagCompound droptag = (NBTTagCompound) stack.getTagCompound().copy();
        NBTTagCompound droptiletag = droptag.getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag);
        NBTTagList droptileitemstag = droptiletag.getTagList(TileEntityCampfire.KEY_Items, 10);

        droptiletag.removeTag(TileEntityCampfire.KEY_Life);
        droptiletag.removeTag(TileEntityCampfire.KEY_StartingLife);
        droptiletag.removeTag(TileEntityCampfire.KEY_PreviousTimestamp);

        if (droptileitemstag.tagCount() != 0)
        {
            for (int i = 0; i < droptileitemstag.tagCount(); ++i)
                invItems.add(ItemStack.loadItemStackFromNBT(droptileitemstag.getCompoundTagAt(i)));

            droptiletag.removeTag(TileEntityCampfire.KEY_Items);
            hasItems = true;
        }

        if (droptiletag.hasNoTags())
            droptag.removeTag(TileEntityCampfire.KEY_BlockEntityTag);

        if (!droptag.hasNoTags())
            copyTags = true;

        for (int i = 0; i < stack.stackSize; ++i)
        {
            ItemStack dropstack;

            if (itemRand.nextDouble() < CampfireBackportConfig.burnToNothingChances[getTypeIndex()])
            {
                dropstack = ItemStack.copyItemStack(CampfireBackportConfig.campfireDropsStacks[getTypeIndex()]);

                particles = 65;
            }
            else
            {
                dropstack = new ItemStack(CampfireBackportBlocks.getBlockFromLitAndType(false, getType()));

                if (copyTags)
                    dropstack.setTagCompound((NBTTagCompound) droptag.copy());

                if (particles == 0)
                    particles = 20;
            }

            entity.entityDropItem(dropstack, 0.1F);

            // lit campfires drop their items when they're extinguished as a block. so they should do the same when they're extinguished as an item!
            if (hasItems)
                invItems.forEach(invStack -> {
                    if (invStack != null)
                        entity.entityDropItem(invStack.copy(), 0.1F);
                });
        }

        TileEntityCampfire.playFizzAndAddSmokeServerSide(world, entity.posX, entity.posY, entity.posZ, particles, 0.25);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advancedTooltips)
    {
        super.addInformation(stack, player, list, advancedTooltips);

        boolean burnOutTip = false;

        if (isLit() && CampfireBackportConfig.burnOutAsItem.matches(this)
                && BurnOutRule.findBurnOutRule(player.worldObj, player.posX, player.posY, player.posZ, getType()).getTimer() != -1)
        {
            list.add(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".tooltip.burning_out"));
            burnOutTip = true;
        }

        if (stack.hasTagCompound())
        {
            NBTTagList itemList = stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag).getTagList(TileEntityCampfire.KEY_Items, 10);

            if (itemList.tagCount() != 0)
            {
                if (burnOutTip)
                    list.add("");

                for (int i = 0; i < itemList.tagCount(); ++i)
                    list.add(ItemStack.loadItemStackFromNBT(itemList.getCompoundTagAt(i)).getDisplayName());
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean requiresMultipleRenderPasses()
    {
        return isLit();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamageForRenderPass(int damage, int pass)
    {
        return pass == 0 ? this.itemIcon : this.overlay;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister iconreg)
    {
        if (EnumCampfireType.isRegular(getType()))
        {
            this.itemIcon = iconreg.registerIcon(Reference.MODID + ":" + "campfire_base");
            this.overlay = iconreg.registerIcon(Reference.MODID + ":" + "overlay");
        }
        else
        {
            this.itemIcon = iconreg.registerIcon(Reference.MODID + ":" + "soul_campfire_base");
            this.overlay = iconreg.registerIcon(Reference.MODID + ":" + "soul_overlay");
        }
    }

    public boolean isLit()
    {
        return this.lit;
    }

    public String getType()
    {
        return this.type;
    }

    public int getTypeIndex()
    {
        return EnumCampfireType.index(getType());
    }

}
