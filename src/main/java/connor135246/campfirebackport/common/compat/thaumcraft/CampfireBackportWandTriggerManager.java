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

    public static void postInit()
    {
        WandTriggerRegistry.registerWandBlockTrigger(INSTANCE, 0, CampfireBackportBlocks.campfire, -1, Reference.MODID);
        WandTriggerRegistry.registerWandBlockTrigger(INSTANCE, 0, CampfireBackportBlocks.campfire_base, -1, Reference.MODID);
        WandTriggerRegistry.registerWandBlockTrigger(INSTANCE, 0, CampfireBackportBlocks.soul_campfire, -1, Reference.MODID);
        WandTriggerRegistry.registerWandBlockTrigger(INSTANCE, 0, CampfireBackportBlocks.soul_campfire_base, -1, Reference.MODID);
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

                Aspect visType;
                double visCost;
                if (cblock.isLit())
                {
                    visType = Aspect.WATER;
                    visCost = CampfireBackportConfig.visCosts[cblock.getTypeToInt()];
                }
                else
                {
                    visType = Aspect.FIRE;
                    visCost = CampfireBackportConfig.visCosts[cblock.getTypeToInt() + 2];
                }

                if (visCost > 0.0 && ThaumcraftApiHelper.consumeVisFromWand(wand, player, new AspectList().add(visType, (int) (visCost * 100)), true, false))
                {
                    cblock.toggleCampfireBlockState(world, x, y, z);
                    return true;
                }
            }
        }
        return false;
    }

}
