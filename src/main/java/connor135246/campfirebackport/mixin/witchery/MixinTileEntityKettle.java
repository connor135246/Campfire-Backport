package connor135246.campfirebackport.mixin.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.emoniph.witchery.blocks.BlockKettle.TileEntityKettle;
import com.emoniph.witchery.blocks.TileEntityBase;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.ISidedInventory;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * This mixin allows lit campfires to act as a heat source for Witchery kettles.
 */
@Mixin(TileEntityKettle.class)
public abstract class MixinTileEntityKettle extends TileEntityBase implements ISidedInventory, IFluidHandler
{

    // func_145845_h (in production environment) / updateEntity (in development environment)
    @Redirect(method = "func_145845_h", at = @At(value = "INVOKE", ordinal = 4), remap = false)
    public Material getMaterialProxy(Block block)
    {
        if (block instanceof BlockCampfire && ((BlockCampfire) block).isLit())
            return Material.fire;
        else
            return block.getMaterial();
    }

}
