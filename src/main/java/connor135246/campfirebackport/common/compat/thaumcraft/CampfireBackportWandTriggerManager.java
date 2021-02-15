package connor135246.campfirebackport.common.compat.thaumcraft;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.api.wands.WandTriggerRegistry;

public class CampfireBackportWandTriggerManager implements IWandTriggerManager
{
    public static final IWandTriggerManager INSTANCE = new CampfireBackportWandTriggerManager();

    public static void load()
    {
        CampfireBackportBlocks.LIST_OF_CAMPFIRES.forEach(cblock -> {
            WandTriggerRegistry.registerWandBlockTrigger(INSTANCE, 0, cblock, -1, Reference.MODID);
        });
    }

    @Override
    public boolean performTrigger(World world, ItemStack wand, EntityPlayer player, int x, int y, int z, int side, int event)
    {
        if (!world.isRemote && event == 0)
        {
            Block block = world.getBlock(x, y, z);
            if (block instanceof BlockCampfire)
            {
                BlockCampfire cblock = (BlockCampfire) block;

                AspectList list;
                double visCost;
                if (cblock.isLit())
                {
                    visCost = CampfireBackportConfig.visCosts[cblock.getTypeIndex()];
                    list = new AspectList().add(Aspect.WATER, (int) (visCost * 100));
                }
                else
                {
                    visCost = CampfireBackportConfig.visCosts[cblock.getTypeIndex() + 2];
                    list = new AspectList().add(Aspect.FIRE, (int) (visCost * 100));
                }

                if (visCost > 0.0 && ThaumcraftApiHelper.consumeVisFromWand(wand, player, list, false, false)
                        && BlockCampfire.updateCampfireBlockState(!cblock.isLit(), player, world, x, y, z) == 1)
                {
                    ThaumcraftApiHelper.consumeVisFromWand(wand, player, list, true, false);
                    return true;
                }
            }
        }
        return false;
    }

}
