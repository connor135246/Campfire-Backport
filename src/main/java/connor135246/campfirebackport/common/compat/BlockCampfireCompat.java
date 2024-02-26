package connor135246.campfirebackport.common.compat;

import java.util.List;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import cpw.mods.fml.common.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Optional.InterfaceList(value = {
        @Optional.Interface(iface = "vazkii.botania.api.mana.IManaTrigger", modid = "Botania"),
        @Optional.Interface(iface = "gregapi.block.IBlockToolable", modid = "gregapi"),
})
public class BlockCampfireCompat extends BlockCampfire implements vazkii.botania.api.mana.IManaTrigger, gregapi.block.IBlockToolable
{

    public BlockCampfireCompat(boolean lit, int typeIndex)
    {
        super(lit, typeIndex);
    }

    // Botania

    @Optional.Method(modid = "Botania")
    @Override
    public void onBurstCollision(vazkii.botania.api.internal.IManaBurst burst, World world, int x, int y, int z)
    {
        if (!world.isRemote && !burst.isFake() && shouldLensLightCampfire(burst.getSourceLens()) && igniteOrReigniteCampfire(null, world, x, y, z) != 0)
            burst.setFake(true); // to stop the burst from creating a fire block afterwards
    }

    /**
     * Checks if the lens (or the composite lens) is the Kindle Lens.
     */
    @Optional.Method(modid = "Botania")
    public static boolean shouldLensLightCampfire(ItemStack lens)
    {
        if (lens != null)
        {
            if (lens.getItem() == vazkii.botania.common.item.ModItems.lens && lens.getItemDamage() == vazkii.botania.common.item.lens.ItemLens.FIRE)
                return true;

            if (lens.getItem() instanceof vazkii.botania.api.mana.ILens)
            {
                ItemStack innerLens = ((vazkii.botania.api.mana.ILens) lens.getItem()).getCompositeLens(lens);
                if (innerLens != null && innerLens.getItem() == vazkii.botania.common.item.ModItems.lens
                        && innerLens.getItemDamage() == vazkii.botania.common.item.lens.ItemLens.FIRE)
                    return true;
            }
        }
        return false;
    }

    // gregapi

    @Optional.Method(modid = "gregapi")
    @Override
    public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, IInventory aPlayerInventory,
            boolean aSneaking, ItemStack aStack, World aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ)
    {
        if (!aSneaking)
        {
            EntityPlayer maybeplayer = aPlayer instanceof EntityPlayer ? (EntityPlayer) aPlayer : null;
            if (gregapi.data.CS.TOOL_igniter.equals(aTool))
            {
                switch (igniteOrReigniteCampfire(maybeplayer, aWorld, aX, aY, aZ))
                {
                case 1:
                    return 10000;
                case 2:
                    return 0;
                }
            }
            else if (gregapi.data.CS.TOOL_shovel.equals(aTool))
            {
                switch (extinguishCampfire(maybeplayer, aWorld, aX, aY, aZ))
                {
                case 1:
                    return 10000;
                case 2:
                    return 0;
                }
            }
        }
        return gregapi.block.ToolCompat.onToolClick(this, aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack,
                aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
    }

}
