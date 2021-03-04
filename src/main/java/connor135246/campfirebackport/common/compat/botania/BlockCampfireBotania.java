package connor135246.campfirebackport.common.compat.botania;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.mana.ILens;
import vazkii.botania.api.mana.IManaTrigger;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.lens.ItemLens;

public class BlockCampfireBotania extends BlockCampfire implements IManaTrigger
{

    public BlockCampfireBotania(boolean lit, String type)
    {
        super(lit, type);
    }

    @Override
    public void onBurstCollision(IManaBurst burst, World world, int x, int y, int z)
    {
        if (!burst.isFake() && !isLit() && shouldLensLightCampfire(burst.getSourceLens()))
        {
            if (!world.isRemote)
                toggleCampfireBlockState(world, x, y, z);

            burst.setFake(true); // to stop the burst from creating a fire block afterwards
        }
    }

    /**
     * Checks if the lens (or the composite lens) is the Kindle Lens.
     */
    public static boolean shouldLensLightCampfire(ItemStack lens)
    {
        if (lens != null)
        {
            if (lens.getItem() == ModItems.lens && lens.getItemDamage() == ItemLens.FIRE)
                return true;

            if (lens.getItem() instanceof ILens)
            {
                ItemStack innerLens = ((ILens) lens.getItem()).getCompositeLens(lens);
                if (innerLens != null && innerLens.getItem() == ModItems.lens && innerLens.getItemDamage() == ItemLens.FIRE)
                    return true;
            }
        }
        return false;
    }

}
