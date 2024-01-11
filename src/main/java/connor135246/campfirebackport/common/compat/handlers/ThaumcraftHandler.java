package connor135246.campfirebackport.common.compat.handlers;

import java.util.function.Consumer;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;

public class ThaumcraftHandler
{

    public static void load()
    {
        Consumer<? super Block> register = cblock -> {
            WandTriggerRegistry.registerWandBlockTrigger(CampfireBackportWandTriggerManager.INSTANCE, 0, cblock, -1, Reference.MODID);
        };
        CampfireBackportBlocks.LIT_CAMPFIRES.forEach(register);
        CampfireBackportBlocks.UNLIT_CAMPFIRES.forEach(register);

        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.campfire, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.campfire_base, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.soul_campfire, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.SOUL, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.soul_campfire_base, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.SOUL, 1));

        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.foxfire_campfire, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.foxfire_campfire_base, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.shadow_campfire, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.DARKNESS, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(CampfireBackportBlocks.shadow_campfire_base, 1, OreDictionary.WILDCARD_VALUE),
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.TREE, 9).add(Aspect.DARKNESS, 1));

        // primal arrow extinguishing/igniting compat
        BlockCampfire.primalArrowClass = EntityPrimalArrow.class;
    }

    public static class CampfireBackportWandTriggerManager implements IWandTriggerManager
    {

        public static final IWandTriggerManager INSTANCE = new CampfireBackportWandTriggerManager();

        @Override
        public boolean performTrigger(World world, ItemStack wand, EntityPlayer player, int x, int y, int z, int side, int event)
        {
            if (!world.isRemote && event == 0) // if this returns true on the client, apparently nothing will happen on the server.
            {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileEntityCampfire)
                {
                    TileEntityCampfire ctile = (TileEntityCampfire) tile;

                    double igniteCost = CampfireBackportConfig.visCosts[ctile.getActingTypeIndex() + 2];
                    double extinguishCost = CampfireBackportConfig.visCosts[ctile.getActingTypeIndex()];
                    boolean extinguisher;
                    AspectList list = null;

                    if (igniteCost > 0.0 && (!ctile.isLit() || ctile.canBeReignited()))
                    {
                        extinguisher = false;
                        list = new AspectList().add(Aspect.FIRE, (int) (igniteCost * 100));
                    }
                    else if (extinguishCost > 0.0 && ctile.isLit())
                    {
                        extinguisher = true;
                        list = new AspectList().add(Aspect.WATER, (int) (extinguishCost * 100));
                    }
                    else
                        return false;

                    if (ThaumcraftApiHelper.consumeVisFromWand(wand, player, list, false, false)
                            && BlockCampfire.updateCampfireBlockState(extinguisher, player, ctile) == 1)
                    {
                        ThaumcraftApiHelper.consumeVisFromWand(wand, player, list, true, false);
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
