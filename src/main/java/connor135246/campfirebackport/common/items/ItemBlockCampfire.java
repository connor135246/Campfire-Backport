package connor135246.campfirebackport.common.items;

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

                NBTTagCompound tilenbt = stack.getTagCompound().getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag);
                long timestamp = world.getTotalWorldTime();

                if (!tilenbt.hasNoTags())
                {
                    int timePassed = (int) (timestamp - tilenbt.getLong(TileEntityCampfire.KEY_PreviousTimestamp));

                    tilenbt.setInteger(TileEntityCampfire.KEY_Life, tilenbt.getInteger(TileEntityCampfire.KEY_Life) - timePassed);

                    tilenbt.setLong(TileEntityCampfire.KEY_PreviousTimestamp, timestamp);
                }
                else
                {
                    int burnOut = TileEntityCampfire.natureRange(baseBurnOut);

                    tilenbt.setInteger(TileEntityCampfire.KEY_Life, burnOut);
                    tilenbt.setInteger(TileEntityCampfire.KEY_StartingLife, burnOut);
                    tilenbt.setLong(TileEntityCampfire.KEY_PreviousTimestamp, timestamp);
                    stack.getTagCompound().setTag(TileEntityCampfire.KEY_BlockEntityTag, tilenbt);
                }

                if (tilenbt.getInteger(TileEntityCampfire.KEY_Life) <= 0)
                {
                    int particles = 0;

                    boolean copyTags = false;
                    boolean hasItems = false;

                    NBTTagCompound droptag = (NBTTagCompound) stack.getTagCompound().copy();
                    NBTTagCompound droptiletag = droptag.getCompoundTag(TileEntityCampfire.KEY_BlockEntityTag);
                    NBTTagList droptileitemstag = droptiletag.getTagList(TileEntityCampfire.KEY_Items, 10);

                    droptiletag.removeTag(TileEntityCampfire.KEY_Life);
                    droptiletag.removeTag(TileEntityCampfire.KEY_StartingLife);
                    droptiletag.removeTag(TileEntityCampfire.KEY_PreviousTimestamp);

                    if (droptileitemstag.tagCount() != 0)
                    {
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
                        {
                            for (int j = 0; j < droptileitemstag.tagCount(); ++j)
                                entity.entityDropItem(ItemStack.loadItemStackFromNBT(droptileitemstag.getCompoundTagAt(j)), 0.1F);
                        }
                    }

                    TileEntityCampfire.playFizzAndAddSmokeServerSide(world, entity.posX, entity.posY, entity.posZ, particles, 0.25);

                    return true;
                }
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advancedTooltips)
    {
        super.addInformation(stack, player, list, advancedTooltips);

        if (isLit() && CampfireBackportConfig.burnOutAsItem.matches(this)
                && BurnOutRule.findBurnOutRule(player.worldObj, player.posX, player.posY, player.posZ, getType()).getTimer() != -1)
        {
            list.add(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".tooltip.burning_out"));
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
