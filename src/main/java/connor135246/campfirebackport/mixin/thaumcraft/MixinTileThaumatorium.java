package connor135246.campfirebackport.mixin.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.ISidedInventory;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumatorium;

/**
 * This mixin allows lit campfires to act as a heat source for Thaumcraft thaumatoriums.
 */
@Mixin(TileThaumatorium.class)
public abstract class MixinTileThaumatorium extends TileThaumcraft implements IAspectContainer, IEssentiaTransport, ISidedInventory
{

    @Inject(method = "checkHeat", at = @At(value = "INVOKE", ordinal = 3), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true, remap = false)
    void onCheckHeat(CallbackInfoReturnable<Boolean> cir, Material mat, Block bi)
    {
        if (bi instanceof BlockCampfire && ((BlockCampfire) bi).isLit())
            cir.setReturnValue(true);
    }

}
