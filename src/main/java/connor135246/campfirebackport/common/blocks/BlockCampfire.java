package connor135246.campfirebackport.common.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHay;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.FluidEvent.FluidFillingEvent;
import net.minecraftforge.fluids.FluidEvent;
import net.minecraftforge.fluids.FluidRegistry;

public class BlockCampfire extends Block implements ITileEntityProvider
{
    private static final Random RAND = new Random();
    private final boolean lit;
    private static boolean stateChanging = false;

    protected BlockCampfire(boolean lit)
    {
        super(Material.wood);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.4375F, 1.0F);
        this.setHardness(2.0F);
        this.setResistance(2.0F);
        this.lit = lit;
        this.setStepSound(Block.soundTypeWood);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.isBlockContainer = true;
    }

    // metadata:
    // 2 5 3 4
    // S W N E
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack)
    {
        int facing = MathHelper.floor_double((double) (entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;

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

        if (stack.hasDisplayName())
        {
            ((TileEntityCampfire) world.getTileEntity(x, y, z)).func_145951_a(stack.getDisplayName());
        }
    }

    public void onBlockAdded(World world, int x, int y, int z)
    {
        super.onBlockAdded(world, x, y, z);
        if (!world.isRemote)
        {
            Block block = world.getBlock(x, y, z - 1);
            Block block1 = world.getBlock(x, y, z + 1);
            Block block2 = world.getBlock(x - 1, y, z);
            Block block3 = world.getBlock(x + 1, y, z);
            byte b0 = 3;

            if (block.func_149730_j() && !block1.func_149730_j())
            {
                b0 = 3;
            }

            if (block1.func_149730_j() && !block.func_149730_j())
            {
                b0 = 2;
            }

            if (block2.func_149730_j() && !block3.func_149730_j())
            {
                b0 = 5;
            }

            if (block3.func_149730_j() && !block2.func_149730_j())
            {
                b0 = 4;
            }

            world.setBlockMetadataWithNotify(x, y, z, b0, 2);
        }
    }

    public void onPostBlockPlaced(World world, int x, int y, int z, int meta)
    {
        TileEntityCampfire tileent = (TileEntityCampfire) world.getTileEntity(x, y, z);
        tileent.resetRegenWaitTimer();
        checkSignal(world, x, y, z);
    }

    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        checkSignal(world, x, y, z);
    }

    /**
     * Checks if the block below is a hay bale, and updates the tile entity's signalFire state.
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public void checkSignal(World world, int x, int y, int z)
    {
        TileEntityCampfire tileent = (TileEntityCampfire) world.getTileEntity(x, y, z);
        if (world.getBlock(x, y - 1, z) instanceof BlockHay)
            tileent.setSignalFire(true);
        else
            tileent.setSignalFire(false);

        tileent.markDirty();
    }

    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityCampfire();
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        if (!world.isRemote)
        {
            if (!isLit() && entity.isBurning())
            {
                entity.extinguish();
                updateCampfireBlockState(true, world, x, y, z);
            }

            else if (isLit() && !entity.isImmuneToFire() && entity instanceof EntityLivingBase)
            {
                // why don't the items that mobs drop when they die on a campfire end up cooked in vanilla?
                if (((EntityLivingBase) entity).getHealth() <= 1.0F)
                    entity.setFire(10);

                entity.attackEntityFrom(DamageSource.inFire, 1.0F);
            }
        }
    }

    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        if (!isLit() && !world.isRemote)
        {
            if (EnchantmentHelper.getFireAspectModifier(player) != 0)
            {
                updateCampfireBlockState(true, world, x, y, z);

                if (!player.capabilities.isCreativeMode)
                    player.getCurrentEquippedItem().attemptDamageItem(1, RAND);
            }
        }
    }

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int p_149727_6_, float p_149727_7_, float p_149727_8_,
            float p_149727_9_)
    {
        if (!world.isRemote)
        {
            ItemStack itemstack = player.getHeldItem();

            if (itemstack != null)
            {
                TileEntityCampfire tileent = (TileEntityCampfire) world.getTileEntity(x, y, z);

                boolean survival = !player.capabilities.isCreativeMode;

                if (isLit())
                {
                    if (tileent.tryInventoryAdd(survival ? itemstack : itemstack.copy()))
                        return true;

                    else if (player.getHeldItem().getItem() instanceof ItemSpade)
                    {
                        updateCampfireBlockState(false, world, x, y, z);

                        if (survival)
                            itemstack.attemptDamageItem(1, RAND);

                        return true;
                    }

                    else if (player.getHeldItem().getItem() == Items.water_bucket)
                    {
                        updateCampfireBlockState(false, world, x, y, z);

                        if (survival)
                            player.setCurrentItemOrArmor(0, new ItemStack(Items.bucket));

                        player.clearItemInUse();

                        return true;
                    }

                }
                else
                {
                    if (player.getHeldItem().getItem() instanceof ItemFlintAndSteel)
                    {
                        updateCampfireBlockState(true, world, x, y, z);

                        if (survival)
                            itemstack.attemptDamageItem(1, RAND);

                        return true;
                    }

                    else if (player.getHeldItem().getItem() == Items.fire_charge)
                    {
                        updateCampfireBlockState(true, world, x, y, z);

                        if (survival)
                            --itemstack.stackSize;

                        return true;
                    }
                }
            }
        }
        return true;

    }

    /**
     * Updates the campfire's block between the lit and unlit ("base") version.
     * 
     * @param light
     *            - true to update to lit, false to update to unlit
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public static void updateCampfireBlockState(boolean light, World world, int x, int y, int z)
    {
        if (!world.isRemote)
        {
            int meta = world.getBlockMetadata(x, y, z);
            TileEntity tileentity = world.getTileEntity(x, y, z);
            stateChanging = true;

            if (light)
            {
                world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);
                world.setBlock(x, y, z, CampfireBackportBlocks.campfire);
            }
            else
            {
                world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.fizz", 0.5F, RAND.nextFloat() * 0.4F + 0.8F);
                world.spawnParticle("smoke", (double) x + 0.25D + RAND.nextDouble() / 2.0D * (double) (RAND.nextBoolean() ? 1 : -1), (double) y + 0.44D,
                        (double) z + 0.25D + RAND.nextDouble() / 2.0D * (double) (RAND.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);
                world.setBlock(x, y, z, CampfireBackportBlocks.campfire_base);
            }

            stateChanging = false;
            world.setBlockMetadataWithNotify(x, y, z, meta, 3);

            if (tileentity != null)
            {
                tileentity.validate();
                tileentity.markDirty();
            }
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block p_149749_5_, int p_149749_6_)
    {
        if (!stateChanging)
        {
            TileEntityCampfire tileent = (TileEntityCampfire) world.getTileEntity(x, y, z);

            if (tileent != null)
            {
                for (int i = 0; i < tileent.getSizeInventory(); ++i)
                {
                    ItemStack itemstack = tileent.getStackInSlot(i);

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
                    }

                }

                world.func_147453_f(x, y, z, p_149749_5_);
            }
            super.breakBlock(world, x, y, z, p_149749_5_, p_149749_6_);
            world.removeTileEntity(x, y, z);
        }

    }

    public boolean onBlockEventReceived(World world, int x, int y, int z, int p_149696_5_, int p_149696_6_)
    {
        super.onBlockEventReceived(world, x, y, z, p_149696_5_, p_149696_6_);
        TileEntity tileentity = world.getTileEntity(x, y, z);
        return tileentity != null ? tileentity.receiveClientEvent(p_149696_5_, p_149696_6_) : false;
    }

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random)
    {
        if (isLit())
        {
            if (random.nextInt(10) == 0)
            {
                world.playSound((double) (x + 0.5F), (double) (y + 0.5F), (double) (z + 0.5F),
                        Reference.MODID + ":" + "block.campfire.crackle",
                        0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
            }

            if (random.nextInt(5) == 0)
            {
                world.spawnParticle("lava", (double) (x + 0.5F), (double) (y + 0.5F), (double) (z + 0.5F),
                        (double) (random.nextFloat() / 2.0F), 5.0E-5D, (double) (random.nextFloat() / 2.0F));
            }
        }
    }

    /**
     * @return either the campfire_base block item or the campfire block item, depending on two config settings
     */
    public Item getCampfireBlockItem()
    {
        if (CampfireBackportConfig.maintainState)
            return Item.getItemFromBlock(this);
        else
            return Item.getItemFromBlock(
                    CampfireBackportConfig.startUnlit ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        if (CampfireBackportConfig.silkRequired)
        {
            drops.add(new ItemStack(Items.coal, 2, 1));
            return drops;
        }
        else
        {
            drops.add(new ItemStack(getCampfireBlockItem(), 1, 0));
            return drops;
        }
    }

    // so that waila harvestability recognizes this block as needing silk touch
    @Override
    public int quantityDropped(Random p_149745_1_)
    {
        return 0;
    }

    @Override
    protected ItemStack createStackedBlock(int meta)
    {
        return new ItemStack(getCampfireBlockItem(), 1, 0);
    }

    @Override
    protected boolean canSilkHarvest()
    {
        return CampfireBackportConfig.silkRequired;
    }

    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z)
    {
        return getCampfireBlockItem();
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    public boolean renderAsNormalBlock()
    {
        return false;
    }

    public int getRenderType()
    {
        return -1;
    }

    public boolean isLit()
    {
        return this.lit;
    }

}
