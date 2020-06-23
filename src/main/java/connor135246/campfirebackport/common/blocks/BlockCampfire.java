package connor135246.campfirebackport.common.blocks;

import java.util.ArrayList;
import java.util.Random;

import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockCampfire extends Block implements ITileEntityProvider
{
    private static final Random RAND = new Random();

    private final boolean lit;
    private final String type;

    private static boolean stateChanging = false;

    protected BlockCampfire(boolean lit, String type)
    {
        super(Material.wood);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.4375F, 1.0F);
        this.setHardness(2.0F);
        this.setResistance(2.0F);
        this.lit = lit;
        this.type = type;
        this.setStepSound(Block.soundTypeWood);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.isBlockContainer = true;
    }

    // metadata:
    // 2 5 3 4
    // S W N E
    @Override
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
            ((TileEntityCampfire) world.getTileEntity(x, y, z)).func_145951_a(stack.getDisplayName());
    }

    @Override
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
                b0 = 3;

            if (block1.func_149730_j() && !block.func_149730_j())
                b0 = 2;

            if (block2.func_149730_j() && !block3.func_149730_j())
                b0 = 5;

            if (block3.func_149730_j() && !block2.func_149730_j())
                b0 = 4;

            world.setBlockMetadataWithNotify(x, y, z, b0, 2);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        if (isLit())
            TileEntityCampfire.checkSignal(world, x, y, z);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityCampfire();
    }

    @Override
    public boolean hasTileEntity(int meta)
    {
        return true;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        if (!world.isRemote)
        {
            if (!isLit() && entity.isBurning())
            {
                entity.extinguish();
                updateCampfireBlockState(true, world, x, y, z, this.type);
            }

            else if (isLit() && !entity.isImmuneToFire() && entity instanceof EntityLivingBase)
            {
                if (entity.attackEntityFrom(DamageSource.inFire, checkType(EnumCampfireType.SOUL) ? 2.0F : 1.0F))
                    world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.fizz", 0.5F,
                            2.6F + (RAND.nextFloat() - RAND.nextFloat()) * 0.8F);
            }
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        if (!isLit() && !world.isRemote)
        {
            if (EnchantmentHelper.getFireAspectModifier(player) != 0)
            {
                updateCampfireBlockState(true, world, x, y, z, this.type);

                if (!player.capabilities.isCreativeMode)
                    player.getCurrentEquippedItem().damageItem(1, player);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int p_149727_6_, float p_149727_7_, float p_149727_8_,
            float p_149727_9_)
    {
        ItemStack itemstack = player.getHeldItem();

        if (itemstack != null)
        {
            TileEntityCampfire tileent = (TileEntityCampfire) world.getTileEntity(x, y, z);
            boolean survival = !player.capabilities.isCreativeMode;
            boolean update = false;

            if (tileent.tryInventoryAdd(survival ? itemstack : itemstack.copy()))
                return true;

            if (isLit())
            {
                if (player.getHeldItem().getItem() instanceof ItemSpade)
                {
                    itemstack.damageItem(1, player);
                    update = true;
                }
                else if (player.getHeldItem().getItem() == Items.water_bucket)
                {
                    if (survival)
                        player.setCurrentItemOrArmor(0, new ItemStack(Items.bucket));
                    update = true;
                }

                if (update)
                {
                    if (!world.isRemote)
                        updateCampfireBlockState(false, world, x, y, z, this.type);
                    return true;
                }
            }
            else
            {
                if (player.getHeldItem().getItem() instanceof ItemFlintAndSteel)
                {
                    itemstack.damageItem(1, player);
                    update = true;
                }
                else if (player.getHeldItem().getItem() == Items.fire_charge)
                {
                    if (survival)
                        --itemstack.stackSize;
                    update = true;
                }

                if (update)
                {
                    if (!world.isRemote)
                        updateCampfireBlockState(true, world, x, y, z, this.type);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates a campfire's block between the lit and unlit ("base") version. If the position and type given aren't accurate, nothing happens.
     * 
     * @param light
     *            - true to update to lit, false to update to unlit
     * @param world
     * @param x
     * @param y
     * @param z
     * @param type
     *            - the type of campfire this is
     */
    public static void updateCampfireBlockState(boolean light, World world, int x, int y, int z, String type)
    {
        if (!world.isRemote)
        {
            Block block = world.getBlock(x, y, z);

            // verify that the block is what this method thinks it is
            if (block instanceof BlockCampfire)
            {
                if (!((BlockCampfire) block).checkType(type))
                    return;
            }
            else
                return;

            int meta = world.getBlockMetadata(x, y, z);
            TileEntityCampfire tilecamp = (TileEntityCampfire) world.getTileEntity(x, y, z);
            stateChanging = true;
            BlockCampfire changeTo;

            if (light)
            {
                world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "fire.ignite", 1.0F, RAND.nextFloat() * 0.4F + 0.8F);

                world.setBlock(x, y, z,
                        (BlockCampfire) (type.equals(EnumCampfireType.REGULAR) ? CampfireBackportBlocks.campfire : CampfireBackportBlocks.soul_campfire));

                TileEntityCampfire.checkSignal(world, x, y, z, tilecamp);
            }
            else
            {
                world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.fizz", 0.5F, RAND.nextFloat() * 0.4F + 0.8F);
                world.spawnParticle("smoke", (double) x + 0.25D + RAND.nextDouble() / 2.0D * (double) (RAND.nextBoolean() ? 1 : -1), (double) y + 0.44D,
                        (double) z + 0.25D + RAND.nextDouble() / 2.0D * (double) (RAND.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);

                tilecamp.popItems();

                world.setBlock(x, y, z, (BlockCampfire) (type.equals(EnumCampfireType.REGULAR) ? CampfireBackportBlocks.campfire_base
                        : CampfireBackportBlocks.soul_campfire_base));
            }

            stateChanging = false;
            world.setBlockMetadataWithNotify(x, y, z, meta, 3);

            if (tilecamp != null)
            {
                tilecamp.validate();
                tilecamp.markDirty();
                tilecamp.setThisLit(light);
                tilecamp.setThisType(type);
                tilecamp.setThisMeta(meta);
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
                if (!world.isRemote)
                    tileent.popItems();

                world.func_147453_f(x, y, z, p_149749_5_);
            }
            super.breakBlock(world, x, y, z, p_149749_5_, p_149749_6_);
            world.removeTileEntity(x, y, z);
        }
    }

    @Override
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

            if (random.nextInt(5) == 0 && checkType(EnumCampfireType.REGULAR))
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
        if (CampfireBackportConfig.rememberState.matches(this))
            return Item.getItemFromBlock(this);
        else
        {
            if (checkType(EnumCampfireType.SOUL))
                return Item.getItemFromBlock(
                        CampfireBackportConfig.startUnlit.matches(this) ? CampfireBackportBlocks.soul_campfire_base : CampfireBackportBlocks.soul_campfire);
            else
                return Item.getItemFromBlock(
                        CampfireBackportConfig.startUnlit.matches(this) ? CampfireBackportBlocks.campfire_base : CampfireBackportBlocks.campfire);
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        if (CampfireBackportConfig.silkNeeded.matches(this))
        {
            if (checkType(EnumCampfireType.SOUL))
                drops.add(new ItemStack(CampfireBackportConfig.soulSoil == Blocks.air ? Blocks.soul_sand : CampfireBackportConfig.soulSoil));
            else
                drops.add(new ItemStack(Items.coal, 2, 1));
        }
        else
            drops.add(new ItemStack(getCampfireBlockItem(), 1, 0));

        return drops;
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
    protected ItemStack createStackedBlock(int meta)
    {
        return new ItemStack(getCampfireBlockItem(), 1, 0);
    }

    @Override
    protected boolean canSilkHarvest()
    {
        return CampfireBackportConfig.silkNeeded.matches(this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Item getItem(World world, int x, int y, int z)
    {
        return getCampfireBlockItem();
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
        return -1;
    }

    // Getters and Setters

    public boolean isLit()
    {
        return this.lit;
    }

    public String getType()
    {
        return this.type;
    }

    public boolean checkType(String type)
    {
        return getType().equals(type);
    }

}
